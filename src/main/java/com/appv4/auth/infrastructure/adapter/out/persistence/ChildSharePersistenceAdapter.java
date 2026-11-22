package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildSharePort;

/**
 * Persiste les détenteurs et les invitations d'une fiche (BACK-74).
 *
 * <p><b>Business :</b> le parent qui a inscrit l'enfant la gère encore. Un email accepté l'a en partage.
 * <p><b>Technique :</b> JPA. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildSharePort}.
 */
@Component
@Profile("!test")
public class ChildSharePersistenceAdapter implements ChildSharePort {

	private final BackChildJpaRepository children;
	private final BackChildHolderJpaRepository holders;
	private final BackChildInvitationJpaRepository invitations;

	public ChildSharePersistenceAdapter(
			BackChildJpaRepository children,
			BackChildHolderJpaRepository holders,
			BackChildInvitationJpaRepository invitations) {
		this.children = children;
		this.holders = holders;
		this.invitations = invitations;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean holds(long userId, long childId) {
		return children.findById(childId)
				.filter(child -> !child.isHidden())
				.filter(child -> child.getParentUserId() == userId || holders.existsByChildIdAndUserId(childId, userId))
				.isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isReferent(long userId, long childId) {
		return children.findById(childId)
				.filter(child -> !child.isHidden())
				.filter(child -> child.getParentUserId() == userId || referentHolder(childId, userId))
				.isPresent();
	}

	private boolean referentHolder(long childId, long userId) {
		return holders.findByChildIdAndUserId(childId, userId)
				.map(BackChildHolderEntity::isReferent)
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean pending(long childId, String email) {
		return invitations.existsByChildIdAndEmailAndStatus(childId, email, BackChildInvitationEntity.PENDING);
	}

	@Override
	@Transactional
	public StoredInvitation invite(long childId, String email) {
		BackChildInvitationEntity saved = invitations.save(new BackChildInvitationEntity(childId, email));
		return new StoredInvitation(saved.getId(), saved.getChildId(), saved.getEmail());
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredInvitation> pendingForEmail(String email) {
		return invitations.findByEmailAndStatus(email, BackChildInvitationEntity.PENDING).stream()
				.map(row -> new StoredInvitation(row.getId(), row.getChildId(), row.getEmail()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredInvitation> findPending(long invitationId) {
		return invitations.findByIdAndStatus(invitationId, BackChildInvitationEntity.PENDING)
				.map(row -> new StoredInvitation(row.getId(), row.getChildId(), row.getEmail()));
	}

	@Override
	@Transactional
	public void accept(long invitationId, long userId) {
		BackChildInvitationEntity invitation = invitations
				.findByIdAndStatus(invitationId, BackChildInvitationEntity.PENDING)
				.orElseThrow();
		invitation.accept();
		if (!holders.existsByChildIdAndUserId(invitation.getChildId(), userId)) {
			holders.save(new BackChildHolderEntity(invitation.getChildId(), userId, false));
		}
	}

	@Override
	@Transactional
	public int pendingCount(long childId) {
		return invitations.findByChildIdAndStatus(childId, BackChildInvitationEntity.PENDING).size();
	}

	@Override
	@Transactional
	public void cancelPending(long childId) {
		for (BackChildInvitationEntity invitation : invitations.findByChildIdAndStatus(
				childId, BackChildInvitationEntity.PENDING)) {
			invitation.cancel();
		}
	}
}
