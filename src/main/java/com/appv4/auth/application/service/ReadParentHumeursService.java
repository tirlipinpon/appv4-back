package com.appv4.auth.application.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadParentHumeursUseCase;
import com.appv4.auth.application.port.in.ReadParentHumeursUseCase.Humeur;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.HumeurAnswerPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort.StoredAnswer;

/**
 * Lecture parent de la dernière humeur (BACK-71).
 *
 * <p><b>Business :</b> la date et l'heure sont celles de Bruxelles. Une fermeture
 * se lit « Fermée sans réponse ». La copie du choix ne bouge plus.
 * <p><b>Technique :</b> les enfants sont ceux déjà listés pour ce parent.
 * <p><b>Métier / flow :</b> port in de {@code GET /api/int/parent/humeurs}.
 */
public class ReadParentHumeursService implements ReadParentHumeursUseCase {

	static final String CLOSED = "Fermée sans réponse";
	private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

	private final ChildPort children;
	private final HumeurAnswerPort answers;

	public ReadParentHumeursService(ChildPort children, HumeurAnswerPort answers) {
		this.children = children;
		this.answers = answers;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ParentChildHumeur> list(long parentUserId, String activeRole) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
		return children.findByParent(parentUserId).stream().map(this::toChild).toList();
	}

	private ParentChildHumeur toChild(ChildSaved child) {
		List<StoredAnswer> rows = answers.history(child.id());
		if (rows == null) {
			rows = List.of();
		}
		List<Humeur> humeurs = rows.stream().map(this::toHumeur).toList();
		return new ParentChildHumeur(child.id(), child.firstName(), child.lastName(), humeurs);
	}

	private Humeur toHumeur(StoredAnswer answer) {
		var when = answer.answeredAt().atZone(BRUSSELS);
		String date = DATE.format(when);
		String time = TIME.format(when);
		if (answer.closed()) {
			return new Humeur(null, null, CLOSED, date, time);
		}
		return new Humeur(answer.icon(), answer.word(), null, date, time);
	}
}
