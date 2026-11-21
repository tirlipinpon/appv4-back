package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Style et palette d'un enfant (BACK-66).
 *
 * <p><b>Business :</b> une ligne par fiche, absente tant que l'enfant n'a rien enregistré.
 * <p><b>Technique :</b> table {@code back_child_avatars} ; PK = {@code child_id}.
 * <p><b>Métier / flow :</b> adapter out du port avatar.
 */
@Entity
@Table(name = "back_child_avatars")
public class BackChildAvatarEntity {

	@Id
	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(name = "style_code", nullable = false, length = 64)
	private String styleCode;

	@Column(name = "preset_id", length = 64)
	private String presetId;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected BackChildAvatarEntity() {
	}

	public BackChildAvatarEntity(long childId, String styleCode, String presetId) {
		this.childId = childId;
		this.styleCode = styleCode;
		this.presetId = presetId;
		this.updatedAt = Instant.now();
	}

	public void apply(String styleCode, String presetId) {
		this.styleCode = styleCode;
		this.presetId = presetId;
		this.updatedAt = Instant.now();
	}

	public long getChildId() {
		return childId;
	}

	public String getStyleCode() {
		return styleCode;
	}

	public String getPresetId() {
		return presetId;
	}
}
