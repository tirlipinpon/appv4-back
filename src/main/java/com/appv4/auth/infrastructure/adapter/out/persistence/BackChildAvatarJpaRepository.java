package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * JPA {@code back_child_avatars} (BACK-66).
 */
public interface BackChildAvatarJpaRepository extends JpaRepository<BackChildAvatarEntity, Long> {

	// Une seule lecture cohérente, y compris pour un avatar sans variantes.
	@Query("""
			select avatar.childId as childId, avatar.styleCode as styleCode, avatar.presetId as presetId,
			       pin.zoneCode as zoneCode, pin.variantCode as variantCode
			from BackChildAvatarEntity avatar
			left join BackChildAvatarPinEntity pin on pin.childId = avatar.childId
			where avatar.childId in :childIds
			order by avatar.childId, pin.zoneCode
			""")
	List<AvatarRow> findAvatarsWithPins(@Param("childIds") List<Long> childIds);

	interface AvatarRow {
		long getChildId();

		String getStyleCode();

		String getPresetId();

		String getZoneCode();

		String getVariantCode();
	}
}
