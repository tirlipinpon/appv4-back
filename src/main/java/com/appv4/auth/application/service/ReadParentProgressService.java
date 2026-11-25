package com.appv4.auth.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.text.Collator;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase;
import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.out.AppHoursPort;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.FailedEssai;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.MissionDay;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.ProgressDay;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.RecentSuccess;
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.LectureEssai;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.MissionEssai;
import com.appv4.auth.application.port.out.QuestionTextPort;
import com.appv4.auth.application.service.SuccessWindows.Result;
import com.appv4.auth.application.service.SuccessWindows.Sample;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

/**
 * Quatre lectures du suivi parent (BACK-40).
 *
 * <p><b>Business :</b> le créneau suit les heures de l'admin. La tendance est celle
 * de l'humeur. Les minutes viennent des séjours déjà confirmés.
 * <p><b>Technique :</b> rien n'est écrit. Une fenêtre vide laisse la tendance absente.
 * <p><b>Métier / flow :</b> port in de {@code GET /api/int/parent/children/{childId}/progress}.
 */
public class ReadParentProgressService implements ReadParentProgressUseCase {

	static final String WINDOW_LABEL = "7 jours";
	static final String SUCCESS_LABEL = "3 jours";
	static final Duration THREE_DAYS = Duration.ofHours(3 * 24);

	private static final LocalTime NOON = LocalTime.of(12, 0);
	private static final LocalTime EVENING = LocalTime.of(18, 0);
	private final ChildPort children;
	private final ChildProgressPort progress;
	private final ChildQuestionAttemptPort attempts;
	private final ChildDailyMissionPort missions;
	private final QuestionTextPort questionTexts;
	private final SejourUseCase sejours;
	private final AppHoursPort hours;
	private final QuestionFormRegistry forms;
	private final Clock clock;

	public ReadParentProgressService(
			ChildPort children,
			ChildProgressPort progress,
			ChildQuestionAttemptPort attempts,
			ChildDailyMissionPort missions,
			QuestionTextPort questionTexts,
			SejourUseCase sejours,
			AppHoursPort hours,
			QuestionFormRegistry forms,
			Clock clock) {
		this.children = children;
		this.progress = progress;
		this.attempts = attempts;
		this.missions = missions;
		this.questionTexts = questionTexts;
		this.sejours = sejours;
		this.hours = hours;
		this.forms = forms;
		this.clock = clock;
	}

	/**
	 * Quatre blocs pour ce parent et cet enfant.
	 *
	 * <p><b>Business :</b> fiche visible seulement. La Nuit n'empêche pas le parent de lire.
	 * <p><b>Technique :</b> les essais, les secondes d'activité et les heures de l'admin.
	 * <p><b>Métier / flow :</b> après le filtre JWT, avant la réponse HTTP.
	 */
	@Override
	@Transactional(readOnly = true)
	public ParentProgress read(long parentUserId, String activeRole, long childId) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
		children.findOwned(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ParentChildrenService.CHILD_UNKNOWN));
		Instant now = clock.instant();
		List<LectureEssai> essais = attempts.lectureEssais(childId);
		if (essais == null) {
			essais = List.of();
		}
		AppHours current = hours.current();
		AppHours window = current == null ? AppHours.defaults() : current;
		LocalDate today = LocalDate.now(clock.withZone(ReadChildProgressService.PROGRESS_ZONE));
		StoredProgress stored = progress.findByChildId(childId).orElse(ChildProgressRules.empty(childId));
		var photo = ChildProgressRules.toView(stored, today);
		List<MissionDay> missionDays = missionDays(childId, now, today);
		MissionDay todayMission = null;
		for (MissionDay day : missionDays) {
			if (day.date().equals(today) && day.status() != null) {
				todayMission = day;
			}
		}
		return new ParentProgress(
				creneau(essais, window, now),
				minutes(sejours.countableSeconds(childId)),
				tendance(essais, now),
				formes(essais, now),
				photo.xpTotal(),
				photo.currentStreak(),
				photo.leagueLabel(),
				reussite(essais, now),
				jours(essais, now, today, photo.xpTotal()),
				todayMission,
				missionDays);
	}

	private RecentSuccess reussite(List<LectureEssai> essais, Instant now) {
		Instant cut = now.minus(THREE_DAYS);
		int passed = 0;
		int total = 0;
		for (LectureEssai essai : essais) {
			Instant at = essai.createdAt();
			if (at == null || at.isAfter(now) || !at.isAfter(cut)) {
				continue;
			}
			total++;
			if (essai.passed()) {
				passed++;
			}
		}
		BigDecimal rate = SuccessWindows.rate(passed, total);
		if (rate == null) {
			return null;
		}
		return new RecentSuccess(rate, total, SUCCESS_LABEL);
	}

	/**
	 * Jours du premier essai jusqu'à aujourd'hui.
	 *
	 * <p><b>Business :</b> l'XP d'un jour est celui que l'accueil a vraiment ajouté.
	 * Le dernier total rejoint l'XP déjà gardé. Un jour sans essai n'a pas de compte.
	 */
	private List<ProgressDay> jours(List<LectureEssai> essais, Instant now, LocalDate today, int xpTotal) {
		TreeMap<LocalDate, DayCount> counts = new TreeMap<>();
		for (LectureEssai essai : essais) {
			Instant at = essai.createdAt();
			if (at == null || at.isAfter(now)) {
				continue;
			}
			LocalDate day = at.atZone(ReadChildProgressService.PROGRESS_ZONE).toLocalDate();
			if (day.isAfter(today)) {
				continue;
			}
			counts.computeIfAbsent(day, ignored -> new DayCount()).add(essai);
		}
		if (counts.isEmpty()) {
			return List.of();
		}
		int reconstructed = 0;
		for (DayCount count : counts.values()) {
			reconstructed += count.xpGained;
		}
		int running = Math.max(0, xpTotal - reconstructed);
		List<ProgressDay> days = new ArrayList<>();
		for (LocalDate cursor = counts.firstKey(); !cursor.isAfter(today); cursor = cursor.plusDays(1)) {
			DayCount count = counts.get(cursor);
			int gained = count == null ? 0 : count.xpGained;
			running += gained;
			days.add(new ProgressDay(
					cursor,
					gained,
					running,
					count == null ? null : count.passed,
					count == null ? null : count.failed));
		}
		return days;
	}

	private static final class DayCount {
		private int xpGained;
		private int passed;
		private int failed;

		private void add(LectureEssai essai) {
			if (essai.passed()) {
				passed++;
			} else {
				failed++;
			}
			if (Boolean.TRUE.equals(essai.pay())) {
				xpGained += ChildProgressRules.xpGain(essai.scorePercent());
			}
		}
	}

	private String creneau(List<LectureEssai> essais, AppHours window, Instant now) {
		Slot bestSlot = null;
		Tally best = null;
		for (Slot slot : Slot.values()) {
			Tally tally = tally(essais, window, now, slot);
			if (tally.total == 0) {
				continue;
			}
			if (best == null || tally.beats(best, slot, bestSlot)) {
				best = tally;
				bestSlot = slot;
			}
		}
		return bestSlot == null ? null : bestSlot.label;
	}

	private Tally tally(List<LectureEssai> essais, AppHours window, Instant now, Slot slot) {
		int passed = 0;
		int total = 0;
		for (LectureEssai essai : essais) {
			if (!counts(essai, now) || window.closedAt(essai.createdAt(), zone())) {
				continue;
			}
			if (slotOf(essai.createdAt()) != slot) {
				continue;
			}
			total++;
			if (essai.passed()) {
				passed++;
			}
		}
		return new Tally(passed, total);
	}

	private boolean counts(LectureEssai essai, Instant now) {
		return essai.createdAt() != null && !essai.createdAt().isAfter(now);
	}

	private Slot slotOf(Instant at) {
		LocalTime time = at.atZone(zone()).toLocalTime();
		if (time.isBefore(NOON)) {
			return Slot.MATIN;
		}
		if (time.isBefore(EVENING)) {
			return Slot.APRES_MIDI;
		}
		return Slot.SOIR;
	}

	private Integer minutes(List<Long> seconds) {
		if (seconds == null || seconds.isEmpty()) {
			return null;
		}
		long sum = 0;
		int count = 0;
		for (Long value : seconds) {
			if (value == null || value < 1) {
				continue;
			}
			sum += value;
			count++;
		}
		if (count == 0) {
			return null;
		}
		BigDecimal rounded = BigDecimal.valueOf(sum)
				.divide(BigDecimal.valueOf(count * 60L), 0, RoundingMode.HALF_UP);
		return rounded.intValueExact();
	}

	private Trend tendance(List<LectureEssai> essais, Instant now) {
		List<Sample> samples = new ArrayList<>();
		for (LectureEssai essai : essais) {
			samples.add(new Sample(essai.createdAt(), essai.passed()));
		}
		Result result = SuccessWindows.at(samples, now);
		if (!result.present()) {
			return null;
		}
		return new Trend(result.direction(), result.delta(), WINDOW_LABEL);
	}

	private List<FormeRatee> formes(List<LectureEssai> essais, Instant now) {
		List<TallyForm> groups = new ArrayList<>();
		for (LectureEssai essai : essais) {
			if (!counts(essai, now) || essai.passed() || essai.formCode() == null) {
				continue;
			}
			TallyForm found = null;
			for (TallyForm group : groups) {
				if (group.code.equals(essai.formCode())) {
					found = group;
					break;
				}
			}
			if (found == null) {
				groups.add(new TallyForm(essai.formCode()));
			} else {
				found.count++;
			}
		}
		List<FormeRatee> kept = new ArrayList<>();
		for (TallyForm group : groups) {
			if (group.count < 2) {
				continue;
			}
			String label = forms.knownLabel(group.code);
			if (label == null) {
				continue;
			}
			kept.add(new FormeRatee(group.code, group.count, label));
		}
		Collator labels = collator();
		kept.sort(Comparator.comparingInt(FormeRatee::count).reversed()
				.thenComparing(FormeRatee::label, labels));
		return List.copyOf(kept);
	}

	private static ZoneId zone() {
		return ReadChildProgressService.PROGRESS_ZONE;
	}

	private static Collator collator() {
		Collator collator = Collator.getInstance(Locale.FRANCE);
		collator.setStrength(Collator.PRIMARY);
		return collator;
	}

	private enum Slot {
		MATIN("Matin", 1),
		APRES_MIDI("Après-midi", 2),
		SOIR("Soir", 3);

		private final String label;
		private final int rank;

		Slot(String label, int rank) {
			this.label = label;
			this.rank = rank;
		}
	}

	private List<MissionDay> missionDays(long childId, Instant now, LocalDate today) {
		List<StoredMission> stored = missions.missionsOf(childId);
		if (stored == null) {
			stored = List.of();
		}
		List<LocalDate> finished = missions.finishedDays(childId);
		Set<LocalDate> finishedDays = finished == null ? Set.of() : new HashSet<>(finished);
		Map<LocalDate, StoredMission> byDate = new HashMap<>();
		LocalDate firstMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		for (StoredMission mission : stored) {
			if (mission == null || mission.missionDate() == null) {
				continue;
			}
			byDate.put(mission.missionDate(), mission);
			LocalDate monday = mission.missionDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			if (monday.isBefore(firstMonday)) {
				firstMonday = monday;
			}
		}
		LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		List<MissionEssai> essais = attempts.missionEssais(childId);
		if (essais == null) {
			essais = List.of();
		}
		essais = new ArrayList<>(essais);
		essais.sort(Comparator
				.comparing(MissionEssai::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparingLong(MissionEssai::id));
		Map<Long, String> prompts = questionTexts.promptsOf(questionIds(byDate.values()));
		if (prompts == null) {
			prompts = Map.of();
		}
		List<MissionDay> days = new ArrayList<>();
		for (LocalDate date = firstMonday; !date.isAfter(sunday); date = date.plusDays(1)) {
			days.add(missionDay(date, byDate.get(date), finishedDays.contains(date), essais, prompts, now));
		}
		return days;
	}

	private static Collection<Long> questionIds(Collection<StoredMission> missions) {
		Set<Long> ids = new HashSet<>();
		for (StoredMission mission : missions) {
			if (mission.tasks() == null) {
				continue;
			}
			for (StoredTask task : mission.tasks()) {
				ids.add(task.questionId());
			}
		}
		return ids;
	}

	private static MissionDay missionDay(
			LocalDate date,
			StoredMission mission,
			boolean rememberedFinished,
			List<MissionEssai> essais,
			Map<Long, String> prompts,
			Instant now) {
		if (mission == null) {
			return new MissionDay(date, null, null, null, null, null, null);
		}
		List<StoredTask> tasks = mission.tasks() == null ? List.of() : mission.tasks();
		List<FailedEssai> failed = failedEssais(date, tasks, essais, prompts, now);
		if (tasks.isEmpty()) {
			if (rememberedFinished) {
				return new MissionDay(date, mission.objective(), null, null, "terminee", "Terminée", List.of());
			}
			return new MissionDay(date, mission.objective(), 0, 0, "en_cours", "En cours", failed);
		}
		int progress = 0;
		int target = 0;
		boolean open = false;
		for (StoredTask task : tasks) {
			progress += task.progressCount();
			target += task.targetCount();
			if (task.progressCount() < task.targetCount()) {
				open = true;
			}
		}
		if (open) {
			return new MissionDay(date, mission.objective(), progress, target, "en_cours", "En cours", failed);
		}
		return new MissionDay(date, mission.objective(), progress, target, "terminee", "Terminée", failed);
	}

	private static List<FailedEssai> failedEssais(
			LocalDate date,
			List<StoredTask> tasks,
			List<MissionEssai> essais,
			Map<Long, String> prompts,
			Instant now) {
		Map<Long, String> labels = new HashMap<>();
		for (StoredTask task : tasks) {
			labels.putIfAbsent(task.questionId(), task.label());
		}
		List<FailedEssai> failed = new ArrayList<>();
		for (MissionEssai essai : essais) {
			Instant at = essai.createdAt();
			if (essai.passed() || at == null || at.isAfter(now) || !labels.containsKey(essai.questionId())) {
				continue;
			}
			LocalDate day = at.atZone(ReadChildProgressService.PROGRESS_ZONE).toLocalDate();
			if (!day.equals(date)) {
				continue;
			}
			failed.add(new FailedEssai(labels.get(essai.questionId()), prompts.get(essai.questionId()), at));
		}
		return failed;
	}

	private record Tally(int passed, int total) {

		private boolean beats(Tally other, Slot slot, Slot otherSlot) {
			long mine = SuccessWindows.thousandths(passed, total);
			long theirs = SuccessWindows.thousandths(other.passed, other.total);
			if (mine != theirs) {
				return mine > theirs;
			}
			if (total != other.total) {
				return total > other.total;
			}
			return slot.rank > otherSlot.rank;
		}
	}

	private static final class TallyForm {
		private final String code;
		private int count = 1;

		private TallyForm(String code) {
			this.code = code;
		}
	}
}
