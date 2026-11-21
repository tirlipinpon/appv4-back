package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clé d'une variante choisie (BACK-67).
 */
public class BackChildAvatarPinId implements Serializable {

	private long childId;

	private String zoneCode;

	public BackChildAvatarPinId() {
	}

	public BackChildAvatarPinId(long childId, String zoneCode) {
		this.childId = childId;
		this.zoneCode = zoneCode;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BackChildAvatarPinId that)) {
			return false;
		}
		return childId == that.childId && Objects.equals(zoneCode, that.zoneCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(childId, zoneCode);
	}
}
