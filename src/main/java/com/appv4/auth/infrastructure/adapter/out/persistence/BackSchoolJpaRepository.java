package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Lecture de l'annuaire d'écoles (SCRUM-163).
 */
public interface BackSchoolJpaRepository extends JpaRepository<BackSchoolEntity, Long> {

	List<BackSchoolEntity> findAllByOrderByIdAsc();

	@Query("""
			select school from BackSchoolEntity school
			where school.id > :afterId and (:country = '' or lower(school.country) = :country)
			  and cast(function('translate', lower(school.name), 'àáâãäåçèéêëìíîïñòóôõöùúûüýÿ',
			    'aaaaaaceeeeiiiinooooouuuuyy') as string) like :pattern escape '\\'
			order by school.id asc
			""")
	List<BackSchoolEntity> search(@Param("country") String country, @Param("pattern") String pattern,
			@Param("afterId") long afterId, Pageable page);

}
