package com.appv4.hello.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Colonnes communes des tables hello (front_ / back_).
 * Pas un objet de domaine — infra uniquement.
 */
@MappedSuperclass
abstract class HelloMessageEntityBase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String text;

	protected HelloMessageEntityBase() {
	}

	Long getId() {
		return id;
	}

	String getText() {
		return text;
	}

	void setText(String text) {
		this.text = text;
	}
}
