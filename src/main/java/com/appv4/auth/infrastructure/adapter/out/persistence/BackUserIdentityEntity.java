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
import jakarta.persistence.UniqueConstraint;

/**
 * Mapping JPA de {@code back_user_identities}.
 *
 * <p><b>Métier :</b> « ce compte Google/Facebook/Apple est lié à tel {@code back_users} ».
 * {@code provider_subject} = l'id stable chez le provider (pas l'email, qui peut changer).
 */
@Entity
@Table(
		name = "back_user_identities",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_back_user_identities_provider_subject",
				columnNames = { "provider", "provider_subject" }))
public class BackUserIdentityEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private BackUserEntity user;

	@Column(nullable = false, length = 32)
	private String provider;

	@Column(name = "provider_subject", nullable = false, length = 255)
	private String providerSubject;

	@Column(name = "email_at_provider", length = 320)
	private String emailAtProvider;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public BackUserEntity getUser() {
		return user;
	}

	public void setUser(BackUserEntity user) {
		this.user = user;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getProviderSubject() {
		return providerSubject;
	}

	public void setProviderSubject(String providerSubject) {
		this.providerSubject = providerSubject;
	}

	public String getEmailAtProvider() {
		return emailAtProvider;
	}

	public void setEmailAtProvider(String emailAtProvider) {
		this.emailAtProvider = emailAtProvider;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
