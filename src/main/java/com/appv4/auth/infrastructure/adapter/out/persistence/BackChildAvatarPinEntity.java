package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Variante choisie sur une zone (BACK-67).
 *
 * <p><b>Business :</b> une zone sans ligne reste dessinée par la graine.
 * <p><b>Technique :</b> table {@code back_child_avatar_pins}.
 * <p><b>Métier / flow :</b> écriture d'avatar → ces lignes.
 */
@Entity
@Table(name = "back_child_avatar_pins")
@IdClass(BackChildAvatarPinId.class)
public class BackChildAvatarPinEntity {

	@Id
	@Column(name = "child_id", nullable = false)
	private long childId;

	@Id
	@Column(name = "zone_code", nullable = false, length = 64)
	private String zoneCode;

	@Column(name = "variant_code", nullable = false, length = 64)
	private String variantCode;

	protected BackChildAvatarPinEntity() {
	}

	public BackChildAvatarPinEntity(long childId, String zoneCode, String variantCode) {
		this.childId = childId;
		this.zoneCode = zoneCode;
		this.variantCode = variantCode;
	}

	public String getZoneCode() {
		return zoneCode;
	}

	public String getVariantCode() {
		return variantCode;
	}
}
