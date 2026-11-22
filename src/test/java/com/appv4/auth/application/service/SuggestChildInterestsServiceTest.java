package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.port.out.ChildInterestSuggestionPort.SuggestableInterest;

class SuggestChildInterestsServiceTest {

	private final List<SuggestableInterest> copies = new ArrayList<>();
	private SuggestChildInterestsService service;

	@BeforeEach
	void setUp() {
		copies.clear();
		service = new SuggestChildInterestsService(() -> List.copyOf(copies));
	}

	@Test
	void suggestsTheOldestSpellingSharedByTwoParents() {
		copies.add(shown(1, 10, "Pokémon", "2026-03-01T10:00:00Z"));
		copies.add(shown(2, 20, "pokemon", "2026-03-02T10:00:00Z"));

		assertThat(service.suggest("parent", "pok")).containsExactly("Pokémon");
		assertThat(service.suggest("parent", "po")).containsExactly("Pokémon");
		assertThat(service.suggest("parent", "p")).isEmpty();
		assertThat(service.suggest("parent", "")).isEmpty();
		assertThat(service.suggest("parent", null)).isEmpty();
	}

	@Test
	void prefixMatchesTheStartAndKeepsTheSmallerNumberWhenMomentsMatch() {
		Instant same = Instant.parse("2026-04-01T10:00:00Z");
		copies.add(shown(4, 20, "pokemon", same));
		copies.add(shown(3, 10, "Pokémon", same));
		copies.add(shown(5, 10, "star wars", "2026-04-02T10:00:00Z"));
		copies.add(shown(6, 20, "Star Wars", "2026-04-03T10:00:00Z"));

		assertThat(service.suggest("parent", "pok")).containsExactly("Pokémon");
		assertThat(service.suggest("parent", "sta")).containsExactly("star wars");
		assertThat(service.suggest("parent", "war")).isEmpty();
	}

	@Test
	void oneFamilyDoesNotSuggestAndInactiveCopiesDoNotCount() {
		copies.add(shown(1, 10, "dinosaures", "2026-01-01T00:00:00Z"));
		copies.add(shown(2, 10, "dinosaures", "2026-01-02T00:00:00Z"));
		assertThat(service.suggest("parent", "dino")).isEmpty();

		copies.add(shown(3, 20, "oeuf", "2026-02-01T00:00:00Z"));
		copies.add(new SuggestableInterest(4, 30, "œuf", Instant.parse("2026-01-01T00:00:00Z"), false, false));
		assertThat(service.suggest("parent", "oeu")).isEmpty();
		copies.remove(copies.size() - 1);
		copies.add(new SuggestableInterest(5, 30, "œuf", Instant.parse("2026-01-01T00:00:00Z"), true, true));
		assertThat(service.suggest("parent", "oeu")).isEmpty();

		copies.add(shown(6, 30, "œuf", "2026-01-01T00:00:00Z"));
		assertThat(service.suggest("parent", "oeu")).containsExactly("œuf");
	}

	@Test
	void returnsEightWordsSortedByComparedForm() {
		for (int index = 0; index < 11; index++) {
			String word = "ba" + (char) ('a' + index);
			copies.add(shown(index + 1L, index + 1L, word, "2026-05-01T00:00:00Z"));
			copies.add(shown(100 + index, 100 + index, word, "2026-05-02T00:00:00Z"));
		}
		assertThat(service.suggest("parent", "ba")).containsExactly(
				"baa", "bab", "bac", "bad", "bae", "baf", "bag", "bah");
	}

	@Test
	void refusesARoleThatIsNotParent() {
		assertThatThrownBy(() -> service.suggest("teacher", "pok")).hasMessage(SpaceAccess.DENIED);
		assertThatThrownBy(() -> service.suggest("child", "pok")).hasMessage(SpaceAccess.DENIED);
		assertThatThrownBy(() -> service.suggest("admin", "pok")).hasMessage(SpaceAccess.DENIED);
	}

	private static SuggestableInterest shown(long id, long parentId, String text, String moment) {
		return shown(id, parentId, text, Instant.parse(moment));
	}

	private static SuggestableInterest shown(long id, long parentId, String text, Instant moment) {
		return new SuggestableInterest(id, parentId, text, moment, true, false);
	}
}
