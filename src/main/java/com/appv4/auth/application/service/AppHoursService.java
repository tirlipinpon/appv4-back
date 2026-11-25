package com.appv4.auth.application.service;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.AppHoursUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AppHoursPort;

/**
 * Horaires de l'app enfant, réservés à l'admin.
 *
 * <p><b>Business :</b> l'admin fixe l'heure où l'enfant peut entrer, et l'heure
 * où l'app se ferme. Un autre rôle reçoit le même refus que pour les humeurs.
 * <p><b>Technique :</b> {@code H:mm} accepte {@code 8:00} et {@code 08:00}.
 * La réponse est toujours {@code HH:mm}. Le début doit rester avant la fin.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/admin/app-hours}.
 * Le rôle admin est celui de la base, pas seulement du jeton.
 */
public class AppHoursService implements AppHoursUseCase {

	static final String FORBIDDEN = "Réservé à l'admin.";
	static final String OPEN_INVALID = "L'heure de début est illisible.";
	static final String CLOSE_INVALID = "L'heure de fin est illisible.";
	static final String WINDOW = "L'heure de début est avant l'heure de fin.";

	private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("H:mm");
	private static final DateTimeFormatter STORED = DateTimeFormatter.ofPattern("HH:mm");

	private final ActiveRolePort activeRolePort;
	private final AppHoursPort appHoursPort;

	public AppHoursService(ActiveRolePort activeRolePort, AppHoursPort appHoursPort) {
		this.activeRolePort = activeRolePort;
		this.appHoursPort = appHoursPort;
	}

	@Override
	@Transactional(readOnly = true)
	public AppHoursView read(long callerId) {
		requireAdmin(callerId);
		return view(appHoursPort.current());
	}

	@Override
	@Transactional
	public AppHoursView update(long callerId, String opensAt, String closesAt) {
		requireAdmin(callerId);
		LocalTime opens = parse(opensAt, OPEN_INVALID);
		LocalTime closes = parse(closesAt, CLOSE_INVALID);
		if (!opens.isBefore(closes)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, WINDOW);
		}
		appHoursPort.save(opens, closes);
		return view(appHoursPort.current());
	}

	private void requireAdmin(long callerId) {
		if (!activeRolePort.hasAdminRole(callerId)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, FORBIDDEN);
		}
	}

	private static LocalTime parse(String raw, String error) {
		if (raw == null || raw.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, error);
		}
		try {
			return LocalTime.parse(raw.trim(), HOUR_MINUTE);
		} catch (DateTimeException ex) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, error);
		}
	}

	private static AppHoursView view(AppHours hours) {
		return new AppHoursView(
				STORED.format(hours.opensAt()),
				STORED.format(hours.closesAt()),
				hours.closedMessage());
	}
}
