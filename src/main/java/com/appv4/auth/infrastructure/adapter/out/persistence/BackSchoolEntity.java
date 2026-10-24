package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Une école de l'annuaire d'exemple (SCRUM-163).
 *
 * <p><b>Business :</b> le professeur choisit dans cette liste. On n'invente pas
 * un nom libre. Le pays n'est pas la norme des classes.
 * <p><b>Technique :</b> table {@code back_schools}. Pas un objet du domaine.
 */
@Entity
@Table(
		name = "back_schools",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_back_schools_name_street_postal",
				columnNames = { "name", "street", "postal_code" }))
public class BackSchoolEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false, length = 80)
	private String country;

	@Column(name = "teaching_types", length = 400)
	private String teachingTypes;

	@Column(length = 120)
	private String network;

	@Column(length = 255)
	private String street;

	@Column(name = "postal_code", length = 32)
	private String postalCode;

	@Column(length = 64)
	private String phone;

	@Column(length = 120)
	private String city;

	@Column(length = 120)
	private String province;

	@Column(length = 120)
	private String denomination;

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCountry() {
		return country;
	}

	public String getTeachingTypes() {
		return teachingTypes;
	}
}
