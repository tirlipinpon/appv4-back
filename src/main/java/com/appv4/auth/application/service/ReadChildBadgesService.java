package com.appv4.auth.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildBadgesUseCase;
import com.appv4.auth.application.port.out.ChildBadgePort;
import com.appv4.auth.application.port.out.ChildBadgePort.StoredBadge;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.result.ChildBadgesView;
import com.appv4.auth.application.result.ChildBadgesView.Badge;
import com.appv4.auth.application.result.ChildBadgesView.Gained;

/**
 * Collection des neuf badges (BACK-19).
 *
 * <p><b>Business :</b> seules les étoiles jaunes, première comprise, font monter un niveau
 * dans ce ticket. Les autres badges restent à 0. Une date n'existe que si le franchissement
 * a eu lieu pendant un essai traité ici. La reprise d'une somme ancienne ne la date pas d'aujourd'hui.
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
			new Kind(MISSIONS, "Missions terminées", 1));

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildQuestionAttemptPort attempts;
	private final ChildBadgePort badges;
	private final Clock clock;

	public ReadChildBadgesService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildQuestionAttemptPort attempts,
			ChildBadgePort badges,
			Clock clock) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.attempts = attempts;
		this.badges = badges;
		this.clock = clock;
	}

	@Override
	@Transactional
	public ChildBadgesView badges(String rawToken) {
		long childId = requireActiveChild(rawToken);
		initialize(childId);
		return view(childId, today());
	}

	@Override
	@Transactional
	public void beforeAttempt(long childId) {
		initialize(childId);
	}

	@Override
	@Transactional
	public void afterMarksChanged(long childId) {
		initialize(childId);
		int sum = yellowSum(childId);
		LocalDate today = today();
		grow(childId, PREMIERE, sum >= 1 ? 1 : 0, sum >= 1 ? 1 : 0, today);
		grow(childId, ETOILES, ChildBadgeRules.level(sum, ETOILES_FIRST), sum, today);
	}

	private void initialize(long childId) {
		if (badges.initialized(childId)) {
			return;
		}
		int sum = yellowSum(childId);
		if (sum >= 1) {
			badges.save(childId, new StoredBadge(PREMIERE, 1, 1, null));
		}
		int starLevel = ChildBadgeRules.level(sum, ETOILES_FIRST);
		if (starLevel > 0 || sum > 0) {
			badges.save(childId, new StoredBadge(ETOILES, starLevel, sum, null));
		}
		badges.markInitialized(childId);
	}

	private void grow(long childId, String code, int level, int retained, LocalDate today) {
		StoredBadge previous = load(childId).get(code);
		int oldLevel = previous == null ? 0 : previous.level();
		LocalDate date = previous == null ? null : previous.unlockedOn();
		if (level > oldLevel) {
			date = today;
		}
		if (level == 0 && retained == 0) {
			return;
		}
		badges.save(childId, new StoredBadge(code, Math.max(level, oldLevel), retained, date));
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
		return new ChildBadgesView(cards, gained, null);
	}

	private Badge card(Kind kind, StoredBadge stored, int sum) {
		if (PREMIERE.equals(kind.code())) {
			int level = sum >= 1 ? 1 : 0;
			LocalDate date = stored != null && stored.level() == level ? stored.unlockedOn() : null;
			return new Badge(kind.code(), kind.name(), level, level == 0 ? 0 : 1, null, date);
		}
		if (ETOILES.equals(kind.code())) {
			int computed = ChildBadgeRules.level(sum, ETOILES_FIRST);
			int kept = stored == null ? 0 : stored.level();
			int level = Math.max(computed, kept);
			LocalDate date = stored != null && stored.level() == level ? stored.unlockedOn() : null;
			Integer next = ChildBadgeRules.nextThreshold(level, ETOILES_FIRST);
			return new Badge(kind.code(), kind.name(), level, sum, next, date);
		}
		return new Badge(kind.code(), kind.name(), 0, 0, kind.firstThreshold(), null);
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
