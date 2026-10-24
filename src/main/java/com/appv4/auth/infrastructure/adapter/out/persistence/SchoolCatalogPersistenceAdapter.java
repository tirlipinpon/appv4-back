package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.SchoolCatalogPort;

/**
 * Liste les écoles que le professeur peut choisir (SCRUM-163).
 *
 * <p><b>Business :</b> seules les écoles de l'annuaire sont proposées.
 * L'exemple en contient trois, toutes en Belgique. On n'en ajoute pas à la main.
 * <p><b>Technique :</b> lit {@code back_schools}, id et nom, dans l'ordre d'insertion.
 * Absent du profil {@code test} : les tests web n'ont pas Postgres.
 * <p><b>Métier / flow :</b> adapter out du port référentiel, appelé par le profil professeur.
 * Le pays et les types partent avec chaque école : l'écran filtre la liste, puis les classes.
 */
@Component
@Profile("!test")
public class SchoolCatalogPersistenceAdapter implements SchoolCatalogPort {

	private final BackSchoolJpaRepository schoolRepository;

	public SchoolCatalogPersistenceAdapter(BackSchoolJpaRepository schoolRepository) {
		this.schoolRepository = schoolRepository;
	}

	@Override
	public List<CatalogSchool> list() {
		return schoolRepository.findAllByOrderByIdAsc().stream()
				.map(school -> new CatalogSchool(
						school.getId(),
						school.getName(),
						school.getCountry(),
						school.getTeachingTypes()))
				.toList();
	}
}
