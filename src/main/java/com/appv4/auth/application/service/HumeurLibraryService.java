package com.appv4.auth.application.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.HumeurLibraryUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.HumeurChoicePort;
import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;

/**
 * Bibliothèque des humeurs proposée à tous les enfants (BACK-68).
 *
 * <p><b>Business :</b> un choix a une icône, un mot, ou les deux. L'ordre de
 * création ne bouge pas quand on modifie le texte.
 * <p><b>Technique :</b> les espaces seuls comptent comme vides. Le rôle admin
 * est celui de la base, pas seulement du jeton.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/admin/humeurs}.
 */
public class HumeurLibraryService implements HumeurLibraryUseCase {

	static final String FORBIDDEN = "Réservé à l'admin.";
	static final String BLANK = "Un choix a une icône, un mot, ou les deux.";
	static final String NOT_FOUND = "Choix introuvable.";

	private final ActiveRolePort activeRolePort;
	private final HumeurChoicePort humeurChoicePort;

	public HumeurLibraryService(ActiveRolePort activeRolePort, HumeurChoicePort humeurChoicePort) {
		this.activeRolePort = activeRolePort;
		this.humeurChoicePort = humeurChoicePort;
	}

	@Override
	@Transactional(readOnly = true)
	public List<HumeurChoice> list(long callerId) {
		requireAdmin(callerId);
		return humeurChoicePort.list();
	}

	@Override
	@Transactional
	public HumeurChoice create(long callerId, String icon, String word) {
		requireAdmin(callerId);
		ChoiceText text = text(icon, word);
		return humeurChoicePort.insert(text.icon(), text.word());
	}

	@Override
	@Transactional
	public HumeurChoice update(long callerId, long choiceId, String icon, String word) {
		requireAdmin(callerId);
		requirePresent(choiceId);
		ChoiceText text = text(icon, word);
		return humeurChoicePort.update(choiceId, text.icon(), text.word());
	}

	@Override
	@Transactional
	public void delete(long callerId, long choiceId) {
		requireAdmin(callerId);
		requirePresent(choiceId);
		humeurChoicePort.delete(choiceId);
	}

	private void requireAdmin(long callerId) {
		if (!activeRolePort.hasAdminRole(callerId)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, FORBIDDEN);
		}
	}

	private void requirePresent(long choiceId) {
		if (humeurChoicePort.find(choiceId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, NOT_FOUND);
		}
	}

	private static ChoiceText text(String icon, String word) {
		String cleanedIcon = clean(icon);
		String cleanedWord = clean(word);
		if (cleanedIcon == null && cleanedWord == null) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BLANK);
		}
		return new ChoiceText(cleanedIcon, cleanedWord);
	}

	private static String clean(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record ChoiceText(String icon, String word) {
	}
}
