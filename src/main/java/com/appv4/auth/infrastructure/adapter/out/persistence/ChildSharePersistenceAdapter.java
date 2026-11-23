package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildSharePort;
import com.appv4.auth.application.port.out.ChildSharePort.StoredTransfer;

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
	private final BackChildShareRequestJpaRepository demandes;
	private final BackChildTransferJpaRepository transfers;

	public ChildSharePersistenceAdapter(
			BackChildJpaRepository children,
			BackChildHolderJpaRepository holders,
			BackChildInvitationJpaRepository invitations,
			BackChildShareRequestJpaRepository demandes,
			BackChildTransferJpaRepository transfers) {
		this.children = children;
		this.holders = holders;
		this.invitations = invitations;
		this.demandes = demandes;
		this.transfers = transfers;
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
	@Transactional(readOnly = true)
	public boolean pendingDemande(long senderId, long referentId, String firstName, String lastName) {
		return demandes.findPending(senderId, referentId, firstName, lastName).isPresent();
	}

	@Override
	@Transactional
	public StoredDemande saveDemande(long senderId, long referentId, String firstName, String lastName) {
		BackChildShareRequestEntity saved = demandes.save(
				new BackChildShareRequestEntity(senderId, referentId, firstName, lastName));
		return toDemande(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredDemande> demandesForReferent(long referentId) {
		return demandes.findByReferentUserIdAndStatus(referentId, BackChildShareRequestEntity.PENDING).stream()
				.map(ChildSharePersistenceAdapter::toDemande)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredDemande> findDemande(long id) {
		return demandes.findById(id).map(ChildSharePersistenceAdapter::toDemande);
	}

	@Override
	@Transactional
	public void markDemande(long id, String status) {
		demandes.findById(id).ifPresent(row -> row.setStatus(status));
	}

	@Override
	@Transactional
	public void grantShare(long childId, long userId) {
		if (!holders.existsByChildIdAndUserId(childId, userId)) {
			holders.save(new BackChildHolderEntity(childId, userId, false));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public int pendingDemandesForChild(long referentId, String firstName, String lastName) {
		return demandes.countPendingForName(referentId, firstName, lastName);
	}

	@Override
	@Transactional
	public void cancelDemandes(long referentId, String firstName, String lastName) {
		for (BackChildShareRequestEntity row : demandes.findPendingForName(referentId, firstName, lastName)) {
			row.setStatus(BackChildShareRequestEntity.CANCELLED);
		}
	}

	private static StoredDemande toDemande(BackChildShareRequestEntity row) {
		return new StoredDemande(
				row.getId(),
				row.getSenderUserId(),
				row.getReferentUserId(),
				row.getFirstName(),
				row.getLastName(),
				row.getStatus());
	}

	@Override
	@Transactional(readOnly = true)
	public List<Long> sharedHolderIds(long childId) {
		long parentId = children.findById(childId).map(BackChildEntity::getParentUserId).orElse(-1L);
		return holders.findByChildId(childId).stream()
				.map(BackChildHolderEntity::getUserId)
				.filter(userId -> userId != parentId)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean sharedHolder(long childId, long userId) {
		return sharedHolderIds(childId).contains(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredTransfer> pendingTransfer(long childId) {
		return transfers.findByChildIdAndStatus(childId, BackChildTransferEntity.PENDING).stream()
				.findFirst()
				.map(ChildSharePersistenceAdapter::toTransfer);
	}

	@Override
	@Transactional
	public StoredTransfer saveTransfer(long childId, long fromUserId, long toUserId, String message) {
		BackChildTransferEntity saved = transfers.save(
				new BackChildTransferEntity(childId, fromUserId, toUserId, message));
		return toTransfer(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredTransfer> findTransfer(long id) {
		return transfers.findById(id).map(ChildSharePersistenceAdapter::toTransfer);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredTransfer> transfersFor(long userId) {
		return transfers.findPendingForUser(userId).stream()
				.map(ChildSharePersistenceAdapter::toTransfer)
				.toList();
	}

	@Override
	@Transactional
	public void markTransfer(long id, String status) {
		transfers.findById(id).ifPresent(row -> row.setStatus(status));
	}

	@Override
	@Transactional
	public void cancelTransfers(long childId) {
		for (BackChildTransferEntity row : transfers.findByChildIdAndStatus(childId, BackChildTransferEntity.PENDING)) {
			row.setStatus(BackChildTransferEntity.CANCELLED);
		}
	}

	@Override
	@Transactional
	public void becomeReferent(long childId, long newReferentId, long previousReferentId) {
		BackChildEntity child = children.findById(childId).orElseThrow();
		holders.findByChildIdAndUserId(childId, previousReferentId)
				.ifPresentOrElse(row -> row.setReferent(false), () -> {
					if (previousReferentId != newReferentId) {
						holders.save(new BackChildHolderEntity(childId, previousReferentId, false));
					}
				});
		holders.findByChildIdAndUserId(childId, newReferentId).ifPresent(holders::delete);
		child.assignParent(newReferentId);
	}

	private static StoredTransfer toTransfer(BackChildTransferEntity row) {
		return new StoredTransfer(
				row.getId(),
				row.getChildId(),
				row.getFromUserId(),
				row.getToUserId(),
				row.getMessage(),
				row.getStatus());
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
