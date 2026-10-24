package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Fiches enfant d'un parent (SCRUM-156).
 *
 * <p><b>Business :</b> un parent ne voit que ses enfants. Le code PIN n'est jamais relu.
 * <p><b>Technique :</b> table {@code back_children}. {@code pin_hash} seulement.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/parent/children}.
 */
public interface ChildPort {

	ChildSaved insert(NewChild child);

	List<ChildSaved> findByParent(long parentUserId);

	Optional<ChildPhoto> findPhoto(long parentUserId, long childId);

	/**
	 * @param pinHash hash du PIN, jamais les 4 chiffres
	 */
	record NewChild(
			long parentUserId,
			String firstName,
			String lastName,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			String avatarCode,
			byte[] photo,
			String photoContentType,
			String pinHash) {
	}

	record ChildSaved(
			long id,
			long parentUserId,
			String firstName,
			String lastName,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			String avatarCode,
			boolean hasPhoto) {
	}

	record ChildPhoto(byte[] bytes, String contentType) {
	}
}
