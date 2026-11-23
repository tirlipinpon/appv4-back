package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Une copie de centre d'intérêt sur la fiche d'un enfant (BACK-72).
 *
 * <p><b>Business :</b> le parent écrit ce que l'enfant aime. On garde son orthographe
 * et le moment de cette copie.
 * <p><b>Technique :</b> table {@code back_child_interests}. Pas de forme comparée.
 * <p><b>Métier / flow :</b> écrite avec la fiche, relue avec elle, effacée si la fiche disparaît.
 */
@Entity
@Table(name = "back_child_interests")
public class BackChildInterestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(nullable = false, length = 40)
	private String label;

	@Column(name = "written_at", nullable = false)
	private Instant writtenAt;

	protected BackChildInterestEntity() {
	}

	public BackChildInterestEntity(long childId, String label, Instant writtenAt) {
		this.childId = childId;
		this.label = label;
		this.writtenAt = writtenAt;
	}

	public String getLabel() {
		return label;
	}

	public Instant getWrittenAt() {
		return writtenAt;
	}
}
