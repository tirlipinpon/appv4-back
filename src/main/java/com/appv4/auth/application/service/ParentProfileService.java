package com.appv4.auth.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ParentProfileUseCase;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.result.ParentProfileView;

/**
 * Fiche du compte côté parent. La même ligne sert au professeur : les écoles ne sont pas touchées.
 *
 * <p><b>Business :</b> enregistrer ici se revoit dans l'espace professeur, et l'inverse.
 * <p><b>Technique :</b> écrit le nom, le mail de contact, le téléphone et l'avatar. N'appelle pas
 * le remplacement des écoles.
 * <p><b>Métier / flow :</b> port in parent → contrôles de {@link AccountContact} → port out du compte.
 */
public class ParentProfileService implements ParentProfileUseCase {

	private final TeacherProfilePort teacherProfilePort;

	public ParentProfileService(TeacherProfilePort teacherProfilePort) {
		this.teacherProfilePort = teacherProfilePort;
	}

	@Override
	@Transactional(readOnly = true)
	public ParentProfileView load(long userId, String activeRole) {
		requireParent(activeRole);
		return toView(account(userId));
	}

	@Override
	@Transactional
	public ParentProfileView update(
			long userId,
			String activeRole,
			String displayName,
			String contactEmail,
			String phone,
			String avatarCode) {
		requireParent(activeRole);
		TeacherAccount account = account(userId);
		String name = AccountContact.name(displayName);
		String email = AccountContact.merge(contactEmail, account.contactEmail(), AccountContact::email);
		String nextPhone = AccountContact.merge(phone, account.phone(), AccountContact::phone);
		String avatar = AccountContact.merge(avatarCode, account.avatarCode(), AccountContact::avatar);
		teacherProfilePort.updateContact(userId, name, email, nextPhone, avatar);
		return toView(new TeacherAccount(account.email(), name, email, nextPhone, avatar));
	}

	private TeacherAccount account(long userId) {
		return teacherProfilePort.findAccount(userId)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.NOT_FOUND,
						TeacherProfileService.ACCOUNT_MISSING));
	}

	private static void requireParent(String activeRole) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceHello.DENIED);
		}
	}

	private static ParentProfileView toView(TeacherAccount account) {
		return new ParentProfileView(
				account.email(),
				account.displayName(),
				account.contactEmail(),
				account.phone(),
				account.avatarCode(),
				AccountContact.CHOICES);
	}
}
