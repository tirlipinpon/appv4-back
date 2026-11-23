package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Lien entre un enfant et une matière de professeur (SCRUM-162).
 *
 * <p><b>Business :</b> le parent a ajouté cette matière à cet enfant.
 * Retirer le lien ne retire pas la matière.
 * <p><b>Technique :</b> table {@code back_child_subject_links}. Pas un objet du domaine.
 */
@Entity
@Table(
		name = "back_child_subject_links",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_back_child_subject_links_child_subject",
				columnNames = { "child_id", "subject_id" }))
public class BackChildSubjectLinkEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	/** Vrai si le parent a retiré une matière de la classe : elle ne revient pas toute seule. */
	@Column(nullable = false)
	private boolean excluded;

	protected BackChildSubjectLinkEntity() {
	}

	public BackChildSubjectLinkEntity(long childId, long subjectId, boolean excluded) {
		this.childId = childId;
		this.subjectId = subjectId;
		this.excluded = excluded;
	}

	public long getChildId() {
		return childId;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public boolean isExcluded() {
		return excluded;
	}

	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}
}
