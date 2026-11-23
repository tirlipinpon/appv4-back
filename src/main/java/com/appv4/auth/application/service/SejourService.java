package com.appv4.auth.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.SejourPort;
import com.appv4.auth.application.port.out.SejourPort.OpenSlice;
import com.appv4.auth.application.port.out.SejourPort.StoredInterval;
import com.appv4.auth.application.port.out.SejourPort.StoredSejour;

/**
 * Compte le séjour à partir des signaux (BACK-38).
 *
 * <p><b>Business :</b> seul le temps confirmé par un signal compte. Une lecture
 * plus tard ne rajoute pas les secondes sans nouveau signal.
 * <p><b>Technique :</b> l'horloge injectée fixe l'instant du signal. La tranche
 * ouverte va de son début au dernier signal confirmé.
 * <p><b>Métier / flow :</b> port in Séjour. Les routes enfant et parent appellent ici.
 */
public class SejourService implements SejourUseCase {

	static final String SCREEN_UNKNOWN = "Cet écran est inconnu.";
	static final String SIGNAL_REFUSED = "Ce signal est refusé.";
	static final String DEVICE_REQUIRED = "Il manque l'appareil.";

	private static final Duration LECTURE = Duration.ofMinutes(2);

	private static final ZoneId PARIS = ReadChildProgressService.PROGRESS_ZONE;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
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
	private final Clock clock;

	public SejourService(
			ChildSessionTokenPort sessions,
			ChildLoginPort logins,
			ChildPort children,
			SejourPort sejours,
			Clock clock) {
		this.sessions = sessions;
		this.logins = logins;
		this.children = children;
		this.sejours = sejours;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void signal(String rawToken, String kind, String deviceId, String screen) {
		long childId = requireChild(rawToken);
		if (deviceId == null || deviceId.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, DEVICE_REQUIRED);
		}
		if (!"screen".equals(kind) && !"gesture".equals(kind) && !"presence".equals(kind) && !"leave".equals(kind)) {
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
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ChildSejour readChild(String rawToken, String deviceId) {
		long childId = requireChild(rawToken);
		if (deviceId == null || deviceId.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, DEVICE_REQUIRED);
		}
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		boolean holds = open != null && deviceId != null && deviceId.equals(open.deviceId());
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
			if (open.screen() == null) {
				Instant confirmed = confirmedUntil(open, now);
				sejours.saveOpen(slice(open, deviceId, code, open.sliceStartedAt(), confirmed, open.lastGestureAt()));
				return;
			}
			if (code.equals(open.screen())) {
				return;
			}
			Instant confirmed = confirmedUntil(open, now);
			remember(open, confirmed);
			sejours.saveOpen(slice(open, deviceId, code, now, now, open.lastGestureAt()));
			return;
		}
		freeze(open);
		sejours.saveOpen(slice(open, deviceId, code, now, now, open.lastGestureAt()));
	}

	private void markGesture(long childId, String deviceId) {
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		Instant now = clock.instant();
		if (open == null) {
			OpenSlice created = sejours.open(childId, now, deviceId, null);
			sejours.saveOpen(copy(created, now, now));
			return;
		}
		if (!holds(open, deviceId)) {
			freeze(open);
			sejours.saveOpen(slice(open, deviceId, open.screen(), now, now, now));
			return;
		}
		if (open.lastGestureAt() != null && now.isAfter(open.lastGestureAt().plus(LECTURE))) {
			remember(open, confirmedUntil(open, now));
			sejours.saveOpen(slice(open, deviceId, open.screen(), now, now, now));
			return;
		}
		sejours.saveOpen(copy(open, confirmedUntil(open, now), now));
	}

	private void leave(long childId, String deviceId) {
		OpenSlice open = sejours.findOpen(childId).orElse(null);
		if (open == null || !holds(open, deviceId)) {
			return;
		}
		Instant confirmed = confirmedUntil(open, clock.instant());
		remember(open, confirmed);
		sejours.close(open.id(), clock.instant());
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
		sejours.saveOpen(copy(open, confirmed, open.lastGestureAt()));
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
		Map<String, Long> millis = new LinkedHashMap<>();
		for (StoredInterval interval : stored.intervals()) {
			if (interval.screen() == null) {
				continue;
			}
			long span = Duration.between(interval.startedAt(), interval.endedAt()).toMillis();
			if (span > 0) {
				millis.merge(interval.screen(), span, Long::sum);
			}
		}
		if (stored.open() && stored.sliceStartedAt() != null && stored.lastConfirmedAt() != null) {
			Instant end = stored.lastConfirmedAt();
			if (stored.lastGestureAt() != null) {
				Instant cap = stored.lastGestureAt().plus(LECTURE);
				if (end.isAfter(cap)) {
					end = cap;
				}
			}
			if (stored.screen() != null && end.isAfter(stored.sliceStartedAt())) {
				long span = Duration.between(stored.sliceStartedAt(), end).toMillis();
				millis.merge(stored.screen(), span, Long::sum);
			}
		}
		List<Line> lines = new ArrayList<>();
		long total = 0;
		for (Map.Entry<String, Long> entry : millis.entrySet()) {
			long seconds = entry.getValue() / 1000L;
			if (seconds < 1) {
				continue;
			}
			total += seconds;
			lines.add(new Line(LABELS.get(entry.getKey()), durationLabel(seconds)));
		}
		if (total < 1) {
			return null;
		}
		var when = stored.startedAt().atZone(PARIS);
		return new ParentSejour(DATE.format(when), TIME.format(when), durationLabel(total), List.copyOf(lines));
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

	private void remember(OpenSlice open, Instant end) {
		if (open.screen() != null && end.isAfter(open.sliceStartedAt())) {
			sejours.appendInterval(open.id(), open.screen(), open.sliceStartedAt(), end);
		}
	}

	private static boolean holds(OpenSlice open, String deviceId) {
		return deviceId != null && deviceId.equals(open.deviceId());
	}

	private static OpenSlice slice(
			OpenSlice open,
			String deviceId,
			String screen,
			Instant sliceStartedAt,
			Instant lastConfirmedAt,
			Instant lastGestureAt) {
		return new OpenSlice(
				open.id(),
				open.childId(),
				open.startedAt(),
				deviceId,
				screen,
				sliceStartedAt,
				lastConfirmedAt,
				lastGestureAt);
	}

	private static OpenSlice copy(OpenSlice open, Instant lastConfirmedAt, Instant lastGestureAt) {
		return new OpenSlice(
				open.id(),
				open.childId(),
				open.startedAt(),
				open.deviceId(),
				open.screen(),
				open.sliceStartedAt(),
				lastConfirmedAt,
				lastGestureAt);
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
