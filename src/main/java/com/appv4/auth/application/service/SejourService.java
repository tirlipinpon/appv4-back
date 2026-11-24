package com.appv4.auth.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.in.TeacherStudentsUseCase;
import com.appv4.auth.application.port.in.SejourUseCase.Plafond;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildSharePort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.SejourPort;
import com.appv4.auth.application.port.out.SejourPort.OpenSlice;
import com.appv4.auth.application.port.out.SejourPort.StayEnd;
import com.appv4.auth.application.port.out.SejourPort.StoredInterval;
import com.appv4.auth.application.port.out.SejourPort.StoredSejour;
import com.appv4.auth.application.port.out.SejourQuestionPort;
import com.appv4.auth.application.port.out.SejourQuestionPort.QuestionCaption;

/**
 * Compte le séjour à partir des signaux, ferme l'app enfant pendant la Nuit,
 * et demande si l'enfant est encore là
 * (BACK-38, question BACK-79, Nuit BACK-80, popup BACK-81, professeur BACK-82).
 *
 * <p><b>Business :</b> seul le temps confirmé par un signal compte, jamais après 22 h.
 * De 22 h à 6 h l'enfant n'entre plus. À vingt minutes sans geste, la lecture envoie
 * la popup. Sans « Je continue » à vingt-cinq minutes, le séjour finit.
 * <p><b>Technique :</b> l'horloge injectée est lue en Europe/Paris, le même fuseau que
 * la progression. La tranche ouverte s'arrête au plus tôt entre le dernier geste plus
 * deux minutes et 22 h.
 * <p><b>Métier / flow :</b> port in Séjour. Les routes enfant appellent ici et reçoivent
 * la phrase. La lecture parent et la lecture professeur restent ouvertes pendant la Nuit.
 * Les deux lisent la même liste, après leur propre contrôle d'accès.
 */
public class SejourService implements SejourUseCase {

	static final String SCREEN_UNKNOWN = "Cet écran est inconnu.";
	static final String SIGNAL_REFUSED = "Ce signal est refusé.";
	static final String DEVICE_REQUIRED = "Il manque l'appareil.";

	/** Phrase unique renvoyée par l'API pendant la Nuit. */
	public static final String NUIT_MESSAGE = "L'app est fermée jusqu'à 6 h.";

	/** Phrase de la popup, renvoyée par la lecture enfant seulement quand elle est due. */
	public static final String POPUP_MESSAGE = "Tu es encore là ?";

	/** Bouton de la popup, le seul geste qui relance le séjour. */
	public static final String POPUP_BUTTON = "Je continue";

	/** Phrase unique après vingt-cinq minutes sans « Je continue ». */
	public static final String DISCONNECT_MESSAGE = "Tu as été déconnecté.";

	private static final LocalTime NUIT_START = LocalTime.of(22, 0);
	private static final LocalTime NUIT_END = LocalTime.of(6, 0);
	private static final Duration LECTURE = Duration.ofMinutes(2);
	private static final Duration POPUP = Duration.ofMinutes(20);
	private static final Duration IDLE = Duration.ofMinutes(25);
	private static final String ENDED_LEAVE = "leave";
	private static final String ENDED_NUIT = "nuit";
	private static final String ENDED_IDLE = "idle";

	private static final ZoneId PARIS = ReadChildProgressService.PROGRESS_ZONE;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
	private static final LocalTime NUIT_DEBUT = LocalTime.of(22, 0);
	private static final LocalTime NUIT_FIN = LocalTime.of(6, 0);
	private static final Map<String, String> LABELS = Map.of(
			"accueil", "Accueil",
			"matieres", "Matières",
			"board", "Board",
			"questions", "Questions",
			"recompenses", "Récompenses",
			"classement", "Classement",
			"profil", "Profil");

	private final ChildSessionTokenPort sessions;
	private final ChildLoginPort logins;
	private final ChildPort children;
	private final SejourPort sejours;
	private final SejourQuestionPort questions;
	private final TeacherStudentsUseCase students;
	private final ChildSharePort shares;
	private final Clock clock;

	static final String PLAFOND_RANGE = "Le plafond est entre 1 et 960 minutes.";
	static final String PLAFOND_EXCEEDED = "Plafond du jour dépassé.";

	public SejourService(
			ChildSessionTokenPort sessions,
			ChildLoginPort logins,
			ChildPort children,
			SejourPort sejours,
			Clock clock) {
		this(sessions, logins, children, sejours, clock, ignored -> Optional.empty());
	}

	/**
	 * @param questions énoncé et matière actuels, pour une ligne de question
	 */
	public SejourService(
			ChildSessionTokenPort sessions,
			ChildLoginPort logins,
			ChildPort children,
			SejourPort sejours,
			Clock clock,
			SejourQuestionPort questions) {
		this(sessions, logins, children, sejours, clock, questions, null);
	}

	/**
	 * @param students contrôle de classe déjà utilisé pour les élèves du professeur
	 */
	public SejourService(
			ChildSessionTokenPort sessions,
			ChildLoginPort logins,
			ChildPort children,
			SejourPort sejours,
			Clock clock,
			SejourQuestionPort questions,
			TeacherStudentsUseCase students) {
		this(sessions, logins, children, sejours, clock, questions, students, null);
	}

	/**
	 * @param shares détenteurs de la fiche, pour savoir qui peut écrire le plafond
	 */
	public SejourService(
			ChildSessionTokenPort sessions,
			ChildLoginPort logins,
			ChildPort children,
			SejourPort sejours,
			Clock clock,
			SejourQuestionPort questions,
			TeacherStudentsUseCase students,
			ChildSharePort shares) {
		this.sessions = sessions;
		this.logins = logins;
		this.children = children;
		this.sejours = sejours;
		this.questions = questions;
		this.students = students;
		this.shares = shares;
		this.clock = clock;
	}

	@Override
	@Transactional(noRollbackFor = AuthDomainException.class)
	public void signal(String rawToken, String kind, String deviceId, String screen, Long questionId) {
		refuseDuringNuit(rawToken);
		long childId = requireChild(rawToken);
		if (deviceId == null || deviceId.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, DEVICE_REQUIRED);
		}
		settleIdle(childId, rawToken);
		if ("question".equals(kind)) {
			if (questionId == null) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, SIGNAL_REFUSED);
			}
			openQuestion(childId, deviceId, questionId);
			return;
		}
		if (!"screen".equals(kind) && !"gesture".equals(kind) && !"presence".equals(kind)
				&& !"leave".equals(kind) && !"reactivate".equals(kind)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, SIGNAL_REFUSED);
		}
		if ("screen".equals(kind)) {
			openScreen(childId, deviceId, screen);
			return;
		}
		if ("gesture".equals(kind)) {
			markGesture(childId, deviceId);
			return;
		}
		if ("presence".equals(kind)) {
			confirmPresence(childId, deviceId);
			return;
		}
		if ("leave".equals(kind)) {
			leave(childId, deviceId);
			return;
		}
		reactivate(childId, deviceId);
	}

	@Override
	@Transactional(noRollbackFor = AuthDomainException.class)
	public ChildSejour readChild(String rawToken, String deviceId) {
		refuseDuringNuit(rawToken);
		long childId = requireChild(rawToken);
		if (deviceId == null || deviceId.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, DEVICE_REQUIRED);
		}
		settleIdle(childId, rawToken);
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		boolean holds = open != null && deviceId != null && deviceId.equals(open.deviceId());
		if (holds && popupDue(open, clock.instant())) {
			return new ChildSejour(true, POPUP_MESSAGE, POPUP_BUTTON);
		}
		return new ChildSejour(holds);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ParentSejour> readParent(long parentUserId, String activeRole, long childId) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
		children.findOwned(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ParentChildrenService.CHILD_UNKNOWN));
		return confirmed(childId);
	}

	@Override
	@Transactional(readOnly = true)
	public JourneeMonth readParentMonth(
			long parentUserId, String activeRole, long childId, Integer year, Integer month) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
		children.findOwned(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ParentChildrenService.CHILD_UNKNOWN));
		JourneeMonth shown = monthOf(childId, year, month);
		return new JourneeMonth(
				shown.monthLabel(),
				shown.year(),
				shown.month(),
				shown.currentMonth(),
				shown.journees(),
				plafondOf(parentUserId, childId));
	}

	@Override
	@Transactional
	public Plafond saveParentPlafond(
			long parentUserId, String activeRole, long childId, BigDecimal limitMinutes) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
		children.findOwned(parentUserId, childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ParentChildrenService.CHILD_UNKNOWN));
		if (!referent(parentUserId, childId)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, ParentChildrenService.ONLY_REFERENT);
		}
		Integer minutes = wholeMinutes(limitMinutes);
		children.savePlafond(childId, minutes);
		return plafondOf(parentUserId, childId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ParentSejour> readTeacher(long teacherUserId, String activeRole, long childId) {
		students.requireInClass(teacherUserId, activeRole, childId);
		return confirmed(childId);
	}

	@Override
	@Transactional(readOnly = true)
	public JourneeMonth readTeacherMonth(
			long teacherUserId, String activeRole, long childId, Integer year, Integer month) {
		students.requireInClass(teacherUserId, activeRole, childId);
		return monthOf(childId, year, month);
	}

	/**
	 * Journées d'un mois. La Nuit ne compte pas. Un mois futur est refusé.
	 *
	 * <p><b>Business :</b> le parent et le professeur lisent le même regroupement.
	 * <p><b>Technique :</b> les intervalles fermés et la tranche ouverte confirmée.
	 * <p><b>Métier / flow :</b> après le contrôle d'accès de la lecture du mois.
	 */
	private JourneeMonth monthOf(long childId, Integer year, Integer month) {
		ZonedDateTime now = clock.instant().atZone(PARIS);
		YearMonth asked = askedMonth(year, month, now);
		YearMonth current = YearMonth.from(now);
		if (asked.isAfter(current)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, "Ce mois n'est pas encore là.");
		}
		List<Journee> journees = assemble(childId, asked, now.toLocalDate());
		return new JourneeMonth(
				MONTH.format(asked.atDay(1)),
				asked.getYear(),
				asked.getMonthValue(),
				asked.equals(current),
				journees,
				null);
	}

	/**
	 * Plafond de cette fiche, comparé aux secondes confirmées d'aujourd'hui.
	 *
	 * <p><b>Business :</b> égal au plafond, ce n'est pas dépassé. La Nuit ne compte pas.
	 * Un mois passé n'efface pas l'alerte du jour en cours.
	 * <p><b>Technique :</b> les minutes stockées fois 60. La phrase est fixe.
	 * <p><b>Métier / flow :</b> après le contrôle d'accès de la lecture ou de l'enregistrement.
	 */
	private Plafond plafondOf(long parentUserId, long childId) {
		Integer limit = storedLimit(childId);
		long seconds = todaySeconds(childId);
		boolean exceeded = limit != null && seconds > limit.longValue() * 60L;
		String message = exceeded ? PLAFOND_EXCEEDED : "";
		return new Plafond(limit, exceeded, message, referent(parentUserId, childId));
	}

	private Integer storedLimit(long childId) {
		Optional<Integer> found = children.findPlafond(childId);
		if (found == null || found.isEmpty()) {
			return null;
		}
		return found.get();
	}

	private long todaySeconds(long childId) {
		ZonedDateTime now = clock.instant().atZone(PARIS);
		LocalDate today = now.toLocalDate();
		return assemble(childId, YearMonth.from(today), today).stream()
				.filter(Journee::today)
				.mapToLong(Journee::durationSeconds)
				.findFirst()
				.orElse(0L);
	}

	private boolean referent(long userId, long childId) {
		if (shares != null && shares.isReferent(userId, childId)) {
			return true;
		}
		if (shares != null && shares.holds(userId, childId)) {
			return false;
		}
		return children.findOwned(userId, childId).isPresent();
	}

	private static Integer wholeMinutes(BigDecimal raw) {
		if (raw == null) {
			return null;
		}
		boolean fraction = raw.stripTrailingZeros().scale() > 0;
		boolean outside = raw.compareTo(BigDecimal.ONE) < 0 || raw.compareTo(new BigDecimal("960")) > 0;
		if (fraction || outside) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, PLAFOND_RANGE);
		}
		return raw.intValue();
	}

	private static YearMonth askedMonth(Integer year, Integer month, ZonedDateTime now) {
		if (year == null && month == null) {
			return YearMonth.from(now);
		}
		if (year == null || month == null || month < 1 || month > 12) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, "Mois inconnu.");
		}
		return YearMonth.of(year, month);
	}

	private List<Journee> assemble(long childId, YearMonth month, LocalDate today) {
		Map<LocalDate, Map<String, Piece>> days = new LinkedHashMap<>();
		for (StoredSejour stored : sejours.list(childId)) {
			for (StoredInterval interval : stored.intervals()) {
				pour(days, interval.screen(), interval.questionId(), interval.prompt(), interval.subjectName(),
						interval.startedAt(), interval.endedAt());
			}
			pourOuvert(days, stored);
		}
		List<Journee> rows = new ArrayList<>();
		for (Map.Entry<LocalDate, Map<String, Piece>> entry : days.entrySet()) {
			if (!YearMonth.from(entry.getKey()).equals(month)) {
				continue;
			}
			Journee journee = toJournee(entry.getKey(), entry.getValue(), today);
			if (journee != null) {
				rows.add(journee);
			}
		}
		rows.sort(Comparator.comparing(Journee::year).thenComparing(Journee::month).thenComparing(Journee::day)
				.reversed());
		return List.copyOf(rows);
	}

	private void pourOuvert(Map<LocalDate, Map<String, Piece>> days, StoredSejour stored) {
		if (!stored.open() || stored.sliceStartedAt() == null || stored.lastConfirmedAt() == null) {
			return;
		}
		Instant end = stored.lastConfirmedAt();
		if (stored.lastGestureAt() != null) {
			Instant cap = stored.lastGestureAt().plus(LECTURE);
			if (end.isAfter(cap)) {
				end = cap;
			}
		}
		if (end.isAfter(stored.sliceStartedAt())) {
			pour(days, stored.screen(), stored.questionId(), stored.prompt(), stored.subjectName(),
					stored.sliceStartedAt(), end);
		}
	}

	private void pour(
			Map<LocalDate, Map<String, Piece>> days,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			Instant startedAt,
			Instant endedAt) {
		if (startedAt == null || endedAt == null || !endedAt.isAfter(startedAt)) {
			return;
		}
		if (screen == null && questionId == null) {
			return;
		}
		ZonedDateTime cursor = startedAt.atZone(PARIS);
		ZonedDateTime end = endedAt.atZone(PARIS);
		while (cursor.isBefore(end)) {
			if (nuit(cursor.toLocalTime())) {
				LocalDate wakeDay = cursor.toLocalTime().isBefore(NUIT_FIN)
						? cursor.toLocalDate()
						: cursor.toLocalDate().plusDays(1);
				ZonedDateTime wake = wakeDay.atTime(NUIT_FIN).atZone(PARIS);
				cursor = wake.isAfter(end) ? end : wake;
				continue;
			}
			ZonedDateTime night = cursor.toLocalDate().atTime(NUIT_DEBUT).atZone(PARIS);
			ZonedDateTime next = night.isBefore(end) ? night : end;
			long millis = Duration.between(cursor, next).toMillis();
			if (millis > 0) {
				String key = questionId != null ? "q" + questionId : "s" + screen;
				Instant started = cursor.toInstant();
				LocalDate day = cursor.toLocalDate();
				Piece piece = days.computeIfAbsent(day, ignored -> new LinkedHashMap<>())
						.computeIfAbsent(key, ignored -> new Piece(screen, questionId, started));
				if (started.isBefore(piece.firstAt)) {
					piece.firstAt = started;
				}
				piece.millis += millis;
				if (piece.prompt == null && prompt != null) {
					piece.prompt = prompt;
					piece.subjectName = subjectName;
				}
			}
			cursor = next;
		}
	}

	private static boolean nuit(LocalTime time) {
		return !time.isBefore(NUIT_DEBUT) || time.isBefore(NUIT_FIN);
	}

	private Journee toJournee(LocalDate date, Map<String, Piece> pieces, LocalDate today) {
		List<Piece> ordered = new ArrayList<>(pieces.values());
		ordered.sort(Comparator.comparing(piece -> piece.firstAt));
		List<JourneeLine> lines = new ArrayList<>();
		long total = 0;
		for (Piece piece : ordered) {
			long seconds = piece.millis / 1000L;
			if (seconds < 1) {
				continue;
			}
			total += seconds;
			String space = piece.questionId != null ? "question" : piece.screen;
			String label = piece.questionId != null ? piece.prompt : LABELS.get(piece.screen);
			lines.add(new JourneeLine(space, label, piece.subjectName, durationLabel(seconds), seconds));
		}
		if (total < 1) {
			return null;
		}
		return new Journee(
				date.getYear(),
				date.getMonthValue(),
				date.getDayOfMonth(),
				date.equals(today),
				DATE.format(date),
				durationLabel(total),
				total,
				List.copyOf(lines));
	}

	/** Séjours confirmés, du plus récent au plus ancien. Le parent et le professeur lisent ici. */
	private List<ParentSejour> confirmed(long childId) {
		List<StoredSejour> stored = new ArrayList<>(sejours.list(childId));
		stored.sort(Comparator.comparing(StoredSejour::startedAt).reversed()
				.thenComparing(StoredSejour::id, Comparator.reverseOrder()));
		List<ParentSejour> rows = new ArrayList<>();
		for (StoredSejour sejour : stored) {
			ParentSejour view = toParent(sejour);
			if (view != null) {
				rows.add(view);
			}
		}
		return List.copyOf(rows);
	}

	private void openScreen(long childId, String deviceId, String screen) {
		String code = requireScreen(screen);
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		Instant now = clock.instant();
		if (open == null) {
			sejours.open(childId, now, deviceId, code);
			return;
		}
		if (holds(open, deviceId)) {
			if (open.screen() == null && open.questionId() == null) {
				Instant confirmed = confirmedUntil(open, now);
				save(slice(open, deviceId, code, null, open.sliceStartedAt(), confirmed, open.lastGestureAt()));
				return;
			}
			if (code.equals(open.screen())) {
				return;
			}
			Instant confirmed = confirmedUntil(open, now);
			remember(open, confirmed);
			save(slice(open, deviceId, code, null, now, now, open.lastGestureAt()));
			return;
		}
		freeze(open);
		save(slice(open, deviceId, code, null, now, now, open.lastGestureAt()));
	}

	private void openQuestion(long childId, String deviceId, long questionId) {
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		Instant now = clock.instant();
		if (open == null) {
			OpenSlice created = sejours.open(childId, now, deviceId, null);
			save(slice(created, deviceId, null, questionId, now, now, null));
			return;
		}
		if (holds(open, deviceId)) {
			if (open.screen() == null && open.questionId() == null) {
				Instant confirmed = confirmedUntil(open, now);
				save(slice(
						open, deviceId, null, questionId, open.sliceStartedAt(), confirmed, open.lastGestureAt()));
				return;
			}
			if (Objects.equals(open.questionId(), questionId)) {
				return;
			}
			Instant confirmed = confirmedUntil(open, now);
			remember(open, confirmed);
			save(slice(open, deviceId, null, questionId, now, now, open.lastGestureAt()));
			return;
		}
		freeze(open);
		save(slice(open, deviceId, null, questionId, now, now, open.lastGestureAt()));
	}

	private void markGesture(long childId, String deviceId) {
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		Instant now = clock.instant();
		if (open != null && popupDue(open, now)) {
			return;
		}
		if (open == null) {
			OpenSlice created = sejours.open(childId, now, deviceId, null);
			save(copy(created, now, now));
			return;
		}
		if (!holds(open, deviceId)) {
			freeze(open);
			save(slice(open, deviceId, open.screen(), open.questionId(), now, now, now));
			return;
		}
		if (open.lastGestureAt() != null && now.isAfter(open.lastGestureAt().plus(LECTURE))) {
			remember(open, confirmedUntil(open, now));
			save(slice(open, deviceId, open.screen(), open.questionId(), now, now, now));
			return;
		}
		save(copy(open, confirmedUntil(open, now), now));
	}

	/**
	 * « Je continue » garde le même séjour et relance l'horloge du geste.
	 * Le trou depuis la fin des deux minutes n'est pas de l'activité.
	 */
	private void reactivate(long childId, String deviceId) {
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		Instant now = clock.instant();
		if (open == null || !holds(open, deviceId) || !popupDue(open, now)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, SIGNAL_REFUSED);
		}
		remember(open, confirmedUntil(open, now));
		save(slice(open, deviceId, open.screen(), open.questionId(), now, now, now));
	}

	private void leave(long childId, String deviceId) {
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		if (open == null || !holds(open, deviceId)) {
			return;
		}
		Instant confirmed = confirmedUntil(open, clock.instant());
		remember(open, confirmed);
		sejours.close(open.id(), clock.instant(), ENDED_LEAVE);
	}

	private void confirmPresence(long childId, String deviceId) {
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		if (open == null || !holds(open, deviceId)) {
			return;
		}
		Instant confirmed = confirmedUntil(open, clock.instant());
		if (!confirmed.isAfter(open.lastConfirmedAt())) {
			return;
		}
		save(copy(open, confirmed, open.lastGestureAt()));
	}

	/**
	 * Pendant la Nuit, le séjour ouvert se ferme à 22 h et la route enfant refuse.
	 * La fermeture est conservée même si l'appel se termine par le refus.
	 * La lecture parent ne passe pas ici.
	 */
	@Override
	@Transactional(noRollbackFor = AuthDomainException.class)
	public void refuseDuringNuit(String rawToken) {
		Instant now = clock.instant();
		if (!isNuit(now)) {
			return;
		}
		endOpenSejour(rawToken, nuitCut(now));
		throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, NUIT_MESSAGE);
	}

	/**
	 * Vingt-cinq minutes après le dernier geste, le séjour se ferme.
	 * Les appels suivants refusent jusqu'à un jeton émis après cette fin.
	 */
	@Override
	@Transactional(noRollbackFor = AuthDomainException.class)
	public void refuseAfterIdle(String rawToken) {
		long childId;
		try {
			childId = requireChild(rawToken);
		} catch (AuthDomainException ex) {
			return;
		}
		settleIdle(childId, rawToken);
	}

	/** Ferme l'oubli une fois, puis refuse sans rien écrire de plus. */
	private void settleIdle(long childId, String rawToken) {
		Instant now = clock.instant();
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		if (open != null && idleDue(open, now)) {
			remember(open, open.lastConfirmedAt());
			sejours.close(open.id(), open.lastGestureAt().plus(IDLE), ENDED_IDLE);
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, DISCONNECT_MESSAGE);
		}
		StayEnd latest = sejours.latestEnd(childId).orElse(null);
		if (latest == null || latest.open() || !ENDED_IDLE.equals(latest.endedBy())) {
			return;
		}
		if (latest.closedAt() != null && renewedSince(rawToken, latest.closedAt())) {
			return;
		}
		throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, DISCONNECT_MESSAGE);
	}

	/** Vrai si ce jeton a été émis après la fin : c'est une nouvelle entrée de jour. */
	private boolean renewedSince(String rawToken, Instant closedAt) {
		Optional<Instant> issued = sessions.issuedAt(rawToken);
		return issued != null && issued.isPresent() && issued.get().isAfter(closedAt);
	}

	/** Confirme le délai de lecture au plus jusqu'à 22 h, puis ferme. Sans geste, rien de plus. */
	private void endOpenSejour(String rawToken, Instant cut) {
		long childId;
		try {
			childId = requireChild(rawToken);
		} catch (AuthDomainException ex) {
			return;
		}
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		if (open == null) {
			return;
		}
		Instant end = open.lastGestureAt() == null ? open.lastConfirmedAt() : confirmedUntil(open, cut);
		remember(open, end);
		sejours.close(open.id(), cut, ENDED_NUIT);
	}

	/** 22 h inclus, 6 h exclus, sur l'horloge de la progression. */
	static boolean isNuit(Instant instant) {
		LocalTime time = instant.atZone(PARIS).toLocalTime();
		return !time.isBefore(NUIT_START) || time.isBefore(NUIT_END);
	}

	/** Le 22 h qui a ouvert la Nuit en cours. */
	private static Instant nuitCut(Instant instant) {
		ZonedDateTime paris = instant.atZone(PARIS);
		LocalDate day = paris.toLocalDate();
		if (paris.toLocalTime().isBefore(NUIT_END)) {
			day = day.minusDays(1);
		}
		return day.atTime(NUIT_START).atZone(PARIS).toInstant();
	}

	/** Vrai de vingt minutes après le dernier geste jusqu'à la fin, à vingt-cinq. */
	private static boolean popupDue(OpenSlice open, Instant now) {
		if (open.lastGestureAt() == null) {
			return false;
		}
		Instant popupAt = open.lastGestureAt().plus(POPUP);
		return !now.isBefore(popupAt) && now.isBefore(open.lastGestureAt().plus(IDLE));
	}

	/** Vrai à partir de vingt-cinq minutes après le dernier geste, sans réactivation. */
	private static boolean idleDue(OpenSlice open, Instant now) {
		return open.lastGestureAt() != null && !now.isBefore(open.lastGestureAt().plus(IDLE));
	}

	/** Dernier instant confirmable : pas après le dernier geste plus deux minutes. */
	private static Instant confirmedUntil(OpenSlice open, Instant now) {
		Instant end = now;
		if (open.lastGestureAt() != null) {
			Instant cap = open.lastGestureAt().plus(LECTURE);
			if (end.isAfter(cap)) {
				end = cap;
			}
		}
		if (end.isBefore(open.lastConfirmedAt())) {
			return open.lastConfirmedAt();
		}
		return end;
	}

	private ParentSejour toParent(StoredSejour stored) {
		Map<String, Visit> visits = new LinkedHashMap<>();
		for (StoredInterval interval : stored.intervals()) {
			long span = Duration.between(interval.startedAt(), interval.endedAt()).toMillis();
			addVisit(visits, interval.screen(), interval.questionId(), interval.prompt(), interval.subjectName(), span);
		}
		if (stored.open() && stored.sliceStartedAt() != null && stored.lastConfirmedAt() != null) {
			Instant end = stored.lastConfirmedAt();
			if (stored.lastGestureAt() != null) {
				Instant cap = stored.lastGestureAt().plus(LECTURE);
				if (end.isAfter(cap)) {
					end = cap;
				}
			}
			if (end.isAfter(stored.sliceStartedAt())) {
				long span = Duration.between(stored.sliceStartedAt(), end).toMillis();
				addVisit(visits, stored.screen(), stored.questionId(), stored.prompt(), stored.subjectName(), span);
			}
		}
		List<Line> lines = new ArrayList<>();
		long total = 0;
		for (Visit visit : visits.values()) {
			long seconds = visit.millis / 1000L;
			if (seconds < 1) {
				continue;
			}
			total += seconds;
			lines.add(lineOf(visit, seconds));
		}
		if (total < 1) {
			return null;
		}
		var when = stored.startedAt().atZone(PARIS);
		return new ParentSejour(DATE.format(when), TIME.format(when), durationLabel(total), List.copyOf(lines));
	}

	private Line lineOf(Visit visit, long seconds) {
		if (visit.questionId == null) {
			return new Line(LABELS.get(visit.screen), null, durationLabel(seconds));
		}
		return new Line(visit.prompt, visit.subjectName, durationLabel(seconds));
	}

	private static void addVisit(
			Map<String, Visit> visits,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			long span) {
		if (span <= 0 || (screen == null && questionId == null)) {
			return;
		}
		String key = questionId != null ? "q" + questionId : "s" + screen;
		Visit visit = visits.computeIfAbsent(key, ignored -> new Visit(screen, questionId));
		visit.millis += span;
		if (visit.prompt == null && prompt != null) {
			visit.prompt = prompt;
			visit.subjectName = subjectName;
		}
	}

	private static String durationLabel(long seconds) {
		if (seconds < 60) {
			return seconds + " s";
		}
		long minutes = seconds / 60;
		long rest = seconds % 60;
		if (rest == 0) {
			return minutes + " min";
		}
		return minutes + " min " + rest;
	}

	private static String requireScreen(String screen) {
		if (screen == null || !LABELS.containsKey(screen)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, SCREEN_UNKNOWN);
		}
		return screen;
	}

	private void freeze(OpenSlice open) {
		remember(open, open.lastConfirmedAt());
	}

	private void save(OpenSlice open) {
		sejours.saveOpen(snap(open, open.lastConfirmedAt()));
	}

	/**
	 * Recopie l'énoncé et la matière la première fois que la question atteint une seconde.
	 * Une copie déjà gardée ne se relit pas.
	 */
	private OpenSlice snap(OpenSlice open, Instant end) {
		if (open.questionId() == null || open.prompt() != null) {
			return open;
		}
		String prompt = null;
		String subjectName = null;
		long millis = 0;
		for (StoredSejour stored : sejours.list(open.childId())) {
			if (stored.id() != open.id()) {
				continue;
			}
			for (StoredInterval interval : stored.intervals()) {
				if (!open.questionId().equals(interval.questionId())) {
					continue;
				}
				millis += Duration.between(interval.startedAt(), interval.endedAt()).toMillis();
				if (prompt == null && interval.prompt() != null) {
					prompt = interval.prompt();
					subjectName = interval.subjectName();
				}
			}
		}
		if (end.isAfter(open.sliceStartedAt())) {
			millis += Duration.between(open.sliceStartedAt(), end).toMillis();
		}
		if (prompt != null) {
			return withCaption(open, prompt, subjectName);
		}
		if (millis < 1000) {
			return open;
		}
		QuestionCaption caption = questions.find(open.questionId()).orElse(null);
		if (caption == null) {
			return open;
		}
		return withCaption(open, caption.prompt(), caption.subjectName());
	}

	private void remember(OpenSlice open, Instant end) {
		OpenSlice snapped = snap(open, end);
		if (!end.isAfter(open.sliceStartedAt())) {
			return;
		}
		if (snapped.screen() == null && snapped.questionId() == null) {
			return;
		}
		sejours.appendInterval(
				snapped.id(),
				snapped.screen(),
				snapped.questionId(),
				snapped.prompt(),
				snapped.subjectName(),
				snapped.sliceStartedAt(),
				end);
	}

	private static boolean holds(OpenSlice open, String deviceId) {
		return deviceId != null && deviceId.equals(open.deviceId());
	}

	private static OpenSlice slice(
			OpenSlice open,
			String deviceId,
			String screen,
			Long questionId,
			Instant sliceStartedAt,
			Instant lastConfirmedAt,
			Instant lastGestureAt) {
		boolean sameQuestion = Objects.equals(questionId, open.questionId());
		return new OpenSlice(
				open.id(),
				open.childId(),
				open.startedAt(),
				deviceId,
				screen,
				questionId,
				sameQuestion ? open.prompt() : null,
				sameQuestion ? open.subjectName() : null,
				sliceStartedAt,
				lastConfirmedAt,
				lastGestureAt);
	}

	private static OpenSlice withCaption(OpenSlice open, String prompt, String subjectName) {
		return new OpenSlice(
				open.id(),
				open.childId(),
				open.startedAt(),
				open.deviceId(),
				open.screen(),
				open.questionId(),
				prompt,
				subjectName,
				open.sliceStartedAt(),
				open.lastConfirmedAt(),
				open.lastGestureAt());
	}

	private static OpenSlice copy(OpenSlice open, Instant lastConfirmedAt, Instant lastGestureAt) {
		return slice(
				open,
				open.deviceId(),
				open.screen(),
				open.questionId(),
				open.sliceStartedAt(),
				lastConfirmedAt,
				lastGestureAt);
	}

	/** Morceau d'une journée, avant le passage en secondes entières. */
	private static final class Piece {
		private final String screen;
		private final Long questionId;
		private Instant firstAt;
		private String prompt;
		private String subjectName;
		private long millis;

		private Piece(String screen, Long questionId, Instant firstAt) {
			this.screen = screen;
			this.questionId = questionId;
			this.firstAt = firstAt;
		}
	}

	/** Un écran ou une question, dans l'ordre de la première visite. */
	private static final class Visit {
		private final String screen;
		private final Long questionId;
		private String prompt;
		private String subjectName;
		private long millis;

		private Visit(String screen, Long questionId) {
			this.screen = screen;
			this.questionId = questionId;
		}
	}

	private long requireChild(String rawToken) {
		String token = rawToken == null ? "" : rawToken;
		long childId = sessions.read(token)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.UNAUTHORIZED, LoginChildService.INVALID_SESSION_MESSAGE))
				.childId();
		if (logins.findActiveById(childId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, LoginChildService.INVALID_SESSION_MESSAGE);
		}
		return childId;
	}
}
