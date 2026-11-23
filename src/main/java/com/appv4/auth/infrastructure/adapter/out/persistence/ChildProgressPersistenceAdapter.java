package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildProgressPort;

/**
 * Persiste la progression enfant (BACK-17).
 *
 * <p><b>Business :</b> upsert d'une fiche XP / streak / semaine.
 * <p><b>Technique :</b> JPA sur {@code back_child_progress}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildProgressPort}.
 */
@Component
@Profile("!test")
public class ChildProgressPersistenceAdapter implements ChildProgressPort {

	private final BackChildProgressJpaRepository repository;
	private final BackChildJpaRepository children;

	public ChildProgressPersistenceAdapter(BackChildProgressJpaRepository repository, BackChildJpaRepository children) {
		this.children = children;
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredProgress> findByChildId(long childId) {
		return repository.findById(childId).map(this::toStored);
	}

	@Override
	@Transactional
	public Optional<StoredProgress> findForUpdate(long childId) {
		children.lockForProgressUpdate(childId);
		return findByChildId(childId);
	}

	@Override
	@Transactional
	public StoredProgress save(StoredProgress progress) {
		BackChildProgressEntity entity = repository.findById(progress.childId())
				.orElseGet(() -> new BackChildProgressEntity(
						progress.childId(),
						progress.xpTotal(),
						progress.currentStreak(),
						progress.lastPlayDate(),
						progress.weekStartDate(),
						progress.weekDays()));
		entity.apply(
				progress.xpTotal(),
				progress.currentStreak(),
				progress.lastPlayDate(),
				progress.weekStartDate(),
				progress.weekDays());
		return toStored(repository.save(entity));
	}

	private StoredProgress toStored(BackChildProgressEntity entity) {
		return new StoredProgress(
				entity.getChildId(),
				entity.getXpTotal(),
				entity.getCurrentStreak(),
				entity.getLastPlayDate(),
				entity.getWeekStartDate(),
				entity.getWeekDays());
	}
}
