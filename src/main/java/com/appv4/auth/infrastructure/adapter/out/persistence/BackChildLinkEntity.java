package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Lien entre le nouvel enfant et l'enfant de départ (SCRUM-158).
 *
 * <p><b>Business :</b> les deux fiches montrent la même phrase. « Aucun » n'a pas de ligne.
 * <p><b>Technique :</b> table {@code back_child_links}. Le sujet est le nouvel enfant.
 * <p><b>Métier / flow :</b> écrit après la création de la fiche, relu avec la liste du parent.
 */
@Entity
@Table(name = "back_child_links")
public class BackChildLinkEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "parent_user_id", nullable = false)
	private long parentUserId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_child_id", nullable = false)
	private BackChildEntity subject;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "related_child_id", nullable = false)
	private BackChildEntity related;

	@Column(nullable = false, length = 32)
	private String relation;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected BackChildLinkEntity() {
	}

	public BackChildLinkEntity(
			long parentUserId,
			BackChildEntity subject,
			BackChildEntity related,
			String relation) {
		this.parentUserId = parentUserId;
		this.subject = subject;
		this.related = related;
		this.relation = relation;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public long getParentUserId() {
		return parentUserId;
	}

	public BackChildEntity getSubject() {
		return subject;
	}

	public BackChildEntity getRelated() {
		return related;
	}

	public String getRelation() {
		return relation;
	}

	/** Le mot change. La paire d'enfants reste la même. */
	public void changeRelation(String relation) {
		this.relation = relation;
	}
}
