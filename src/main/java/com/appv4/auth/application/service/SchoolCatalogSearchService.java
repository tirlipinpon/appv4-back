package com.appv4.auth.application.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;

/** Searches the school directory without returning the whole catalogue. */
public class SchoolCatalogSearchService {
	private final SchoolCatalogPort catalog;

	public SchoolCatalogSearchService(SchoolCatalogPort catalog) {
		this.catalog = catalog;
	}

	public SchoolPage search(String role, String country, String query, long afterId, int limit) {
		if (!ActiveRoles.PARENT.equals(role) && !ActiveRoles.TEACHER.equals(role)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceHello.DENIED);
		}
		String countryKey = country == null ? "" : country.trim().toLowerCase(Locale.ROOT);
		String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		if (text.length() > 100 || countryKey.length() > 80) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, "Recherche trop longue.");
		}
		text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		int size = Math.max(1, Math.min(limit, 40));
		List<CatalogSchool> rows = catalog.search(countryKey, text, Math.max(0, afterId), size + 1);
		boolean more = rows.size() > size;
		List<CatalogSchool> page = more ? rows.subList(0, size) : rows;
		Long next = more ? page.get(page.size() - 1).id() : null;
		return new SchoolPage(page.stream().map(school -> new SchoolOption(school.id(), school.name(),
				school.country(), SchoolCurricula.offeredBy(school.country(), school.teachingTypes()))).toList(), next);
	}

	public record SchoolPage(List<SchoolOption> schools, Long nextCursor) {
	}

	public record SchoolOption(long id, String name, String country, List<String> grades) {
	}
}
