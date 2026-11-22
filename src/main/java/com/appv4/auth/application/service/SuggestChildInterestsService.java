package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.SuggestChildInterestsUseCase;
import com.appv4.auth.application.port.out.ChildInterestSuggestionPort;
import com.appv4.auth.application.port.out.ChildInterestSuggestionPort.SuggestableInterest;

/**
 * Choisit les centres d'intérêt proposés pendant la saisie.
 *
 * <p><b>Business :</b> un mot n'est proposé que s'il est porté par deux parents différents,
 * chacun sur un enfant encore actif et encore affiché. L'orthographe est celle de la copie
 * la plus ancienne. À moment égal, c'est le plus petit numéro.
 * <p><b>Technique :</b> le préfixe et le tri utilisent la forme comparée. Huit mots au plus.
 * <p><b>Métier / flow :</b> port in appelé par {@code GET /api/int/parent/interest-suggestions}.
 */
public class SuggestChildInterestsService implements SuggestChildInterestsUseCase {

	static final int MINIMUM_QUERY = 2;
	static final int MAXIMUM_RESULTS = 8;

	private final ChildInterestSuggestionPort suggestions;

	public SuggestChildInterestsService(ChildInterestSuggestionPort suggestions) {
		this.suggestions = suggestions;
	}

	@Override
	public List<String> suggest(String activeRole, String query) {
		if (!ActiveRoles.PARENT.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
		String prefix = ChildInterestList.form(ChildInterestList.reduce(query));
		if (prefix.length() < MINIMUM_QUERY) {
			return List.of();
		}
		Map<String, List<SuggestableInterest>> groups = new LinkedHashMap<>();
		for (SuggestableInterest copy : suggestions.findAll()) {
			if (!copy.active() || copy.hidden()) {
				continue;
			}
			String form = ChildInterestList.form(copy.text());
			if (!form.startsWith(prefix)) {
				continue;
			}
			groups.computeIfAbsent(form, ignored -> new ArrayList<>()).add(copy);
		}
		List<String> forms = new ArrayList<>();
		Map<String, String> spelling = new LinkedHashMap<>();
		for (Map.Entry<String, List<SuggestableInterest>> entry : groups.entrySet()) {
			if (parentsOf(entry.getValue()).size() < 2) {
				continue;
			}
			forms.add(entry.getKey());
			spelling.put(entry.getKey(), oldest(entry.getValue()).text());
		}
		forms.sort(ChildInterestList::compareForms);
		if (forms.size() > MAXIMUM_RESULTS) {
			forms = forms.subList(0, MAXIMUM_RESULTS);
		}
		List<String> words = new ArrayList<>();
		for (String form : forms) {
			words.add(spelling.get(form));
		}
		return List.copyOf(words);
	}

	private static List<Long> parentsOf(List<SuggestableInterest> copies) {
		List<Long> parents = new ArrayList<>();
		for (SuggestableInterest copy : copies) {
			if (!parents.contains(copy.parentUserId())) {
				parents.add(copy.parentUserId());
			}
		}
		return parents;
	}

	private static SuggestableInterest oldest(List<SuggestableInterest> copies) {
		return copies.stream()
				.min(Comparator.comparing(SuggestableInterest::writtenAt).thenComparingLong(SuggestableInterest::id))
				.orElseThrow();
	}
}
