package com.appv4.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildBadgesUseCase;
import com.appv4.auth.application.port.out.ChildBadgePort;
import com.appv4.auth.application.port.out.ChildBadgePort.StoredBadge;
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AttemptTrace;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.result.ChildBadgesView;
import com.appv4.auth.application.result.ChildBadgesView.Badge;
import com.appv4.auth.application.result.ChildBadgesView.Gained;
import com.appv4.auth.application.result.ChildBadgesView.Notice;

/**
 * Collection des neuf badges (BACK-19).
 *
 * <p><b>Business :</b> les étoiles jaunes, première comprise, font monter un niveau.
 * Matière au jaune ne se reconstruit pas : elle monte seulement sur un passage au jaune.
 * Une date n'existe que si le franchissement a eu lieu pendant un essai traité ici.
 * <p><b>Technique :</b> la somme vient des marques. Le niveau gardé ne redescend pas.
 * <p><b>Métier / flow :</b> GET /api/ext/badges, et le jeu avant puis après chaque essai enregistré.
 */
public class ReadChildBadgesService implements ReadChildBadgesUseCase, ChildBadgeProgress {

	static final ZoneId ZONE = ZoneId.of("Europe/Paris");

	static final String PREMIERE = "premiere_etoile_jaune";
	static final String MATIERE = "matiere_au_jaune";
	static final String ETOILES = "etoiles_jaunes";
	static final String RATTRAPAGE = "rattrapage";
	static final String AFFILEE = "bonnes_reponses_affilee";
	static final String JOUR = "bonnes_reponses_du_jour";
	static final String JOURS_AFFILEE = "jours_affilee";
	static final String JOURS_JOUES = "jours_joues";
	static final String MISSIONS = "missions_terminees";
	static final String LECONS = "lecons_finies";

	private static final int ETOILES_FIRST = 5;

	private record Kind(String code, String name, Integer firstThreshold) {
	}

	private static final List<Kind> CATALOG = List.of(
			new Kind(PREMIERE, "Première étoile jaune", null),
			new Kind(MATIERE, "Matière au jaune", null),
			new Kind(ETOILES, "Étoiles jaunes", ETOILES_FIRST),
			new Kind(RATTRAPAGE, "Rattrapage", 1),
			new Kind(AFFILEE, "Bonnes réponses d'affilée", 3),
			new Kind(JOUR, "Bonnes réponses du jour", 3),
			new Kind(JOURS_AFFILEE, "Jours d'affilée", 2),
			new Kind(JOURS_JOUES, "Jours joués", 3),
			new Kind(MISSIONS, "Missions terminées", 1),
			new Kind(LECONS, "Leçons finies", 1));

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildQuestionAttemptPort attempts;
	private final ChildBadgePort badges;
	private final ChildSubjectPort subjects;
	private final TeacherQuestionPort questions;
	private final ChildDailyMissionPort missions;
	private final Clock clock;

	public ReadChildBadgesService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildQuestionAttemptPort attempts,
			ChildBadgePort badges,
			ChildSubjectPort subjects,
			TeacherQuestionPort questions,
			ChildDailyMissionPort missions,
			Clock clock) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.attempts = attempts;
		this.badges = badges;
		this.subjects = subjects;
		this.questions = questions;
		this.missions = missions;
		this.clock = clock;
	}

	@Override
	@Transactional
	public ChildBadgesView badges(String rawToken) {
		long childId = requireActiveChild(rawToken);
		initialize(childId);
		refreshActivity(childId);
		return view(childId, today());
	}

	@Override
	@Transactional
	public void beforeAttempt(long childId) {
		initialize(childId);
	}

	@Override
	@Transactional
	public void afterAttempt(long childId, long subjectId, long questionId, QuestionMark before, QuestionMark after) {
		initialize(childId);
		int sum = yellowSum(childId);
		LocalDate today = today();
		grow(childId, PREMIERE, sum >= 1 ? 1 : 0, sum >= 1 ? 1 : 0, today, null);
		grow(childId, ETOILES, ChildBadgeRules.level(sum, ETOILES_FIRST), sum, today, null);
		if (becameYellow(before, after) && fullyYellow(childId, subjectId)) {
			raiseSubject(childId, subjectId, today);
		}
		refreshActivity(childId);
	}

	@Override
	@Transactional
	public void noteLessonEmptied(long childId) {
		initialize(childId);
		StoredBadge previous = load(childId).get(LECONS);
		int retained = (previous == null ? 0 : previous.retainedValue()) + 1;
		grow(childId, LECONS, ChildBadgeRules.level(retained, 1), retained, today(), null);
	}

	@Override
	@Transactional
	public void noteMission(long childId) {
		initialize(childId);
		LocalDate today = today();
		missions.findByChildAndDate(childId, today).ifPresent(mission -> {
			if (complete(mission)) {
				missions.rememberFinishedDay(childId, today);
			}
		});
		refreshActivity(childId);
	}

	private void initialize(long childId) {
		if (badges.initialized(childId)) {
			return;
		}
		int sum = yellowSum(childId);
		if (sum >= 1) {
			badges.save(childId, new StoredBadge(PREMIERE, 1, 1, null, null));
		}
		int starLevel = ChildBadgeRules.level(sum, ETOILES_FIRST);
		if (starLevel > 0 || sum > 0) {
			badges.save(childId, new StoredBadge(ETOILES, starLevel, sum, null, null));
		}
		badges.markInitialized(childId);
	}

	private void grow(long childId, String code, int level, int retained, LocalDate today, Long subjectId) {
		StoredBadge previous = load(childId).get(code);
		int oldLevel = previous == null ? 0 : previous.level();
		LocalDate date = previous == null ? null : previous.unlockedOn();
		Long keptSubject = subjectId != null ? subjectId : previous == null ? null : previous.subjectId();
		if (level > oldLevel) {
			date = today;
		}
		if (level == 0 && retained == 0) {
			return;
		}
		badges.save(childId, new StoredBadge(code, Math.max(level, oldLevel), retained, date, keptSubject));
	}

	private ChildBadgesView view(long childId, LocalDate today) {
		Map<String, StoredBadge> stored = load(childId);
		int sum = yellowSum(childId);
		List<Badge> cards = new ArrayList<>();
		List<Gained> gained = new ArrayList<>();
		for (Kind kind : CATALOG) {
			Badge card = card(kind, stored.get(kind.code()), sum);
			cards.add(card);
			if (card.unlockedOn() != null && card.unlockedOn().equals(today) && card.level() > 0) {
				gained.add(new Gained(card.code(), card.name(), card.level()));
			}
		}
		return new ChildBadgesView(cards, gained, notice(childId, stored.get(MATIERE)));
	}

	private Badge card(Kind kind, StoredBadge stored, int sum) {
		if (PREMIERE.equals(kind.code())) {
			int level = sum >= 1 ? 1 : 0;
			LocalDate date = stored != null && stored.level() == level ? stored.unlockedOn() : null;
			return new Badge(kind.code(), kind.name(), level, level == 0 ? 0 : 1, null, date);
		}
		if (MATIERE.equals(kind.code())) {
			int level = stored == null ? 0 : stored.level();
			LocalDate date = stored == null ? null : stored.unlockedOn();
			return new Badge(kind.code(), kind.name(), level, level, null, date);
		}
		if (ETOILES.equals(kind.code())) {
			int computed = ChildBadgeRules.level(sum, ETOILES_FIRST);
			int kept = stored == null ? 0 : stored.level();
			int level = Math.max(computed, kept);
			LocalDate date = stored != null && stored.level() == level ? stored.unlockedOn() : null;
			Integer next = ChildBadgeRules.nextThreshold(level, ETOILES_FIRST);
			return new Badge(kind.code(), kind.name(), level, sum, next, date);
		}
		int level = stored == null ? 0 : stored.level();
		int retained = stored == null ? 0 : stored.retainedValue();
		Integer next = kind.firstThreshold() == null
				? null
				: ChildBadgeRules.nextThreshold(level, kind.firstThreshold());
		LocalDate date = stored == null ? null : stored.unlockedOn();
		return new Badge(kind.code(), kind.name(), level, retained, next, date);
	}

	private void refreshActivity(long childId) {
		List<AttemptTrace> traces = attempts.traces(childId);
		if (traces == null) {
			traces = List.of();
		}
		rememberCompleteMissions(childId);
		List<LocalDate> finished = missions.finishedDays(childId);
		if (finished == null) {
			finished = List.of();
		}
		saveCounter(childId, RATTRAPAGE, rattrapage(traces), 1);
		saveCounter(childId, JOURS_JOUES, countedDays(allDays(traces), 3), 3);
		saveCounter(childId, JOURS_AFFILEE, payStreak(traces), 2);
		saveCounter(childId, AFFILEE, answerRun(traces), 3);
		saveCounter(childId, JOUR, bestDay(traces), 3);
		saveCounter(childId, MISSIONS, countedDays(finished, 1), 1);
	}

	private void rememberCompleteMissions(long childId) {
		List<StoredMission> missionsOf = missions.missionsOf(childId);
		if (missionsOf == null) {
			return;
		}
		for (StoredMission mission : missionsOf) {
			if (complete(mission)) {
				missions.rememberFinishedDay(childId, mission.missionDate());
			}
		}
	}

	private static boolean complete(StoredMission mission) {
		return mission.tasks() != null
				&& !mission.tasks().isEmpty()
				&& mission.tasks().stream().allMatch(task -> task.progressCount() >= task.targetCount());
	}

	private void saveCounter(long childId, String code, Outcome outcome, int first) {
		StoredBadge previous = load(childId).get(code);
		int oldLevel = previous == null ? 0 : previous.level();
		int level = Math.max(oldLevel, ChildBadgeRules.level(outcome.value(), first));
		int retained = Math.max(previous == null ? 0 : previous.retainedValue(), outcome.value());
		LocalDate date = previous == null ? null : previous.unlockedOn();
		if (level > oldLevel) {
			date = outcome.crossed();
		}
		if (level == 0 && retained == 0) {
			return;
		}
		Long subjectId = previous == null ? null : previous.subjectId();
		badges.save(childId, new StoredBadge(code, level, retained, date, subjectId));
	}

	private Outcome rattrapage(List<AttemptTrace> traces) {
		Map<Long, Boolean> sawBlack = new HashMap<>();
		Set<Long> recovered = new HashSet<>();
		List<LocalDate> dates = new ArrayList<>();
		for (AttemptTrace trace : traces) {
			if (trace.markCode() == QuestionMark.BLACK) {
				sawBlack.put(trace.questionId(), true);
			}
			if (trace.markCode() == QuestionMark.YELLOW
					&& sawBlack.getOrDefault(trace.questionId(), false)
					&& recovered.add(trace.questionId())) {
				dates.add(paris(trace.createdAt()));
			}
		}
		return outcome(dates.size(), dates, 1);
	}

	private Outcome payStreak(List<AttemptTrace> traces) {
		List<LocalDate> days = payDays(traces);
		int best = 0;
		LocalDate crossed = null;
		int run = 0;
		LocalDate previous = null;
		LocalDate runStart = null;
		for (LocalDate day : days) {
			if (previous != null && day.equals(previous.plusDays(1))) {
				run++;
			} else {
				run = 1;
				runStart = day;
			}
			if (run > best) {
				best = run;
				int level = ChildBadgeRules.level(best, 2);
				if (level > 0) {
					int threshold = ChildBadgeRules.nextThreshold(level - 1, 2);
					crossed = runStart.plusDays(threshold - 1L);
				}
			}
			previous = day;
		}
		return new Outcome(best, best == 0 ? null : crossed);
	}

	private Outcome answerRun(List<AttemptTrace> traces) {
		int run = 0;
		int best = 0;
		LocalDate crossed = null;
		for (AttemptTrace trace : traces) {
			if (trace.pay() == null) {
				continue;
			}
			if (Boolean.TRUE.equals(trace.pay())) {
				int before = ChildBadgeRules.level(best, 3);
				run++;
				if (run > best) {
					best = run;
					if (ChildBadgeRules.level(best, 3) > before) {
						crossed = paris(trace.createdAt());
					}
				}
			} else if (!trace.passed()) {
				run = 0;
			}
		}
		return new Outcome(best, crossed);
	}

	private Outcome bestDay(List<AttemptTrace> traces) {
		Map<LocalDate, Integer> counts = new HashMap<>();
		for (AttemptTrace trace : traces) {
			if (Boolean.TRUE.equals(trace.pay())) {
				counts.merge(paris(trace.createdAt()), 1, Integer::sum);
			}
		}
		int best = 0;
		LocalDate day = null;
		for (Map.Entry<LocalDate, Integer> entry : counts.entrySet()) {
			if (entry.getValue() > best || (entry.getValue() == best && day != null && entry.getKey().isBefore(day))) {
				best = entry.getValue();
				day = entry.getKey();
			}
		}
		int level = ChildBadgeRules.level(best, 3);
		return new Outcome(best, level == 0 ? null : day);
	}

	private static List<LocalDate> allDays(List<AttemptTrace> traces) {
		return traces.stream().map(trace -> paris(trace.createdAt())).distinct().sorted().toList();
	}

	private static List<LocalDate> payDays(List<AttemptTrace> traces) {
		return traces.stream()
				.filter(trace -> Boolean.TRUE.equals(trace.pay()))
				.map(trace -> paris(trace.createdAt()))
				.distinct()
				.sorted()
				.toList();
	}

	private static Outcome countedDays(List<LocalDate> days, int first) {
		List<LocalDate> sorted = days.stream().distinct().sorted().toList();
		return outcome(sorted.size(), sorted, first);
	}

	private static Outcome outcome(int value, List<LocalDate> dates, int first) {
		int level = ChildBadgeRules.level(value, first);
		if (level == 0) {
			return new Outcome(value, null);
		}
		int threshold = ChildBadgeRules.nextThreshold(level - 1, first);
		return new Outcome(value, dates.get(threshold - 1));
	}

	private static LocalDate paris(Instant instant) {
		return LocalDate.ofInstant(instant, ZONE);
	}

	private record Outcome(int value, LocalDate crossed) {
	}

	private void raiseSubject(long childId, long subjectId, LocalDate today) {
		StoredBadge current = load(childId).get(MATIERE);
		if (current != null && current.subjectId() != null && current.subjectId() != subjectId) {
			return;
		}
		int nextLevel = current == null ? 1 : current.level() + 1;
		badges.save(childId, new StoredBadge(MATIERE, nextLevel, nextLevel, today, subjectId));
	}

	private static boolean becameYellow(QuestionMark before, QuestionMark after) {
		return after == QuestionMark.YELLOW && before != QuestionMark.YELLOW;
	}

	private boolean fullyYellow(long childId, long subjectId) {
		List<Long> ids = questionIds(subjectId);
		if (ids.isEmpty()) {
			return false;
		}
		Map<Long, StoredMark> marks = attempts.allMarks(childId);
		if (marks == null) {
			return false;
		}
		for (Long id : ids) {
			StoredMark mark = marks.get(id);
			if (mark == null || mark.code() != QuestionMark.YELLOW) {
				return false;
			}
		}
		return true;
	}

	private Notice notice(long childId, StoredBadge subjectBadge) {
		if (subjectBadge == null || subjectBadge.level() < 1 || subjectBadge.subjectId() == null) {
			return null;
		}
		long subjectId = subjectBadge.subjectId();
		CatalogSubject subject = openSubject(childId, subjectId);
		if (subject == null) {
			return null;
		}
		int remaining = 0;
		Map<Long, StoredMark> marks = attempts.allMarks(childId);
		for (Long id : questionIds(subjectId)) {
			StoredMark mark = marks == null ? null : marks.get(id);
			if (mark == null || mark.code() != QuestionMark.YELLOW) {
				remaining++;
			}
		}
		if (remaining == 0) {
			return null;
		}
		String label = remaining == 1
				? subject.name() + " : 1 question n'est pas encore jaune."
				: subject.name() + " : " + remaining + " questions ne sont pas encore jaunes.";
		return new Notice(label, subject.name(), remaining);
	}

	private CatalogSubject openSubject(long childId, long subjectId) {
		List<StoredLink> links = subjects.linksOf(childId);
		boolean linked = links != null && links.stream().anyMatch(link -> !link.excluded() && link.subjectId() == subjectId);
		if (!linked) {
			return null;
		}
		List<CatalogSubject> visible = subjects.visibleByIds(List.of(subjectId));
		if (visible == null || visible.isEmpty()) {
			return null;
		}
		return visible.get(0);
	}

	private List<Long> questionIds(long subjectId) {
		List<Long> ids = questions.questionIds(subjectId);
		return ids == null ? List.of() : ids;
	}

	private Map<String, StoredBadge> load(long childId) {
		Map<String, StoredBadge> loaded = badges.load(childId);
		return loaded == null ? Map.of() : loaded;
	}

	private int yellowSum(long childId) {
		int sum = 0;
		Map<Long, StoredMark> marks = attempts.allMarks(childId);
		if (marks == null) {
			return 0;
		}
		for (StoredMark mark : marks.values()) {
			if (mark.code() == QuestionMark.YELLOW) {
				sum += mark.yellowCount();
			}
		}
		return sum;
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(ZONE));
	}

	private long requireActiveChild(String rawToken) {
		long childId = childSessionTokenPort.read(rawToken)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.UNAUTHORIZED, ReadChildProgressService.INVALID_SESSION_MESSAGE))
				.childId();
		if (childLoginPort.findActiveById(childId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, ReadChildProgressService.INVALID_SESSION_MESSAGE);
		}
		return childId;
	}
}
