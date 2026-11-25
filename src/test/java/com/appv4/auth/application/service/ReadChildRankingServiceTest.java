package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.RankingGroups;
import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredAvatar;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredPin;
import com.appv4.auth.application.port.out.ChildRankingCasesPort;
import com.appv4.auth.application.port.out.ChildRankingCasesPort.RankingSelection;
import com.appv4.auth.application.port.out.ChildRankingRosterPort;
import com.appv4.auth.application.port.out.ChildRankingRosterPort.ChildPhoto;
import com.appv4.auth.application.port.out.ChildRankingRosterPort.VisibleChild;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.result.ChildRankingView;

class ReadChildRankingServiceTest {

	private MemoryRoster roster;
	private MemoryCases cases;
	private MemoryAvatars avatars;
	private MemoryTokens tokens;
	private ReadChildRankingService service;

	@BeforeEach
	void setUp() {
		roster = new MemoryRoster();
		cases = new MemoryCases();
		avatars = new MemoryAvatars();
		tokens = new MemoryTokens();
		service = new ReadChildRankingService(tokens, roster, cases, avatars, CharacterAvatarCatalog.load());
	}

	@Test
	void classeOrdersByXpAndSharesAPlace() {
		roster.add(child(1, "Lina", 120, "2026-01-03T00:00:00Z"));
		roster.add(child(2, "Akira", 90, "2026-01-02T00:00:00Z"));
		roster.add(child(3, "Noé", 90, "2026-01-04T00:00:00Z"));
		tokens.login(2);

		ChildRankingView view = service.read("token", null);

		assertThat(view.scope()).isEqualTo("classe");
		assertThat(view.groupLabel()).isEqualTo("CE1 · École Nouvelle");
		assertThat(view.phrase()).isNull();
		assertThat(view.scopes()).extracting(chip -> chip.code(), chip -> chip.selected())
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("classe", true),
						org.assertj.core.groups.Tuple.tuple("annee", false),
						org.assertj.core.groups.Tuple.tuple("pays", false),
						org.assertj.core.groups.Tuple.tuple("tous", false),
						org.assertj.core.groups.Tuple.tuple("age", false));
		assertThat(view.cards()).extracting(
				card -> card.firstName(), card -> card.place(), card -> card.mine(), card -> card.face())
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("Lina", 1, false, "animal"),
						org.assertj.core.groups.Tuple.tuple("Akira", 2, true, "animal"),
						org.assertj.core.groups.Tuple.tuple("Noé", 2, false, "animal"));
	}

	@Test
	void eleventhCardIsTheCallerWithTheRealPlace() {
		for (int index = 1; index <= 10; index++) {
			roster.add(child(index, "Camarade " + index, 100 - index, "2026-01-01T00:00:00Z"));
		}
		roster.add(child(11, "Akira", 1, "2026-02-01T00:00:00Z"));
		tokens.login(11);

		ChildRankingView view = service.read("token", "pays");

		assertThat(view.cards()).hasSize(11);
		assertThat(view.cards().get(9).firstName()).isEqualTo("Camarade 10");
		assertThat(view.cards().get(10).firstName()).isEqualTo("Akira");
		assertThat(view.cards().get(10).place()).isEqualTo(11);
		assertThat(view.cards().get(10).pinned()).isTrue();
		assertThat(view.cards().get(10).mine()).isTrue();
	}

	@Test
	void noSchoolOnClasseReturnsOnlyTheSchoolPhrase() {
		roster.add(new VisibleChild(
				2, "Akira", 8, "CE1", null, null, null, 90, "bear", false,
				Instant.parse("2026-01-02T00:00:00Z")));
		roster.add(child(1, "Lina", 120, "2026-01-03T00:00:00Z"));
		tokens.login(2);

		ChildRankingView view = service.read("token", "classe");

		assertThat(view.cards()).isEmpty();
		assertThat(view.phrase()).isEqualTo(RankingGroups.SCHOOL_PHRASE);
		assertThat(view.groupLabel()).isEmpty();
	}

	@Test
	void uncheckedCaseHidesTheCallerAndKeepsTheOthers() {
		roster.add(child(1, "Lina", 120, "2026-01-03T00:00:00Z"));
		roster.add(child(2, "Akira", 90, "2026-01-02T00:00:00Z"));
		cases.save(2, new RankingSelection(false, true, true, true, true, true));
		tokens.login(2);

		ChildRankingView view = service.read("token", "classe");

		assertThat(view.cards()).extracting(card -> card.firstName()).containsExactly("Lina");
		assertThat(view.phrase()).isEqualTo(RankingGroups.HIDDEN_PHRASE);
	}

	@Test
	void savedCharacterWinsOverTheAnimal() {
		roster.add(child(2, "Akira", 90, "2026-01-02T00:00:00Z"));
		avatars.save(2, "lorelei", null, List.of(new StoredPin("mouth", "happy01")));
		tokens.login(2);

		ChildRankingView view = service.read("token", "age");

		assertThat(view.cards()).hasSize(1);
		assertThat(view.cards().getFirst().face()).isEqualTo("personnage");
		assertThat(view.cards().getFirst().styleCode()).isEqualTo("lorelei");
		assertThat(view.cards().getFirst().place()).isEqualTo(1);
		assertThat(view.phrase()).isNull();
	}

	@Test
	void unknownGroupAndMissingTokenAreRefused() {
		roster.add(child(2, "Akira", 90, "2026-01-02T00:00:00Z"));
		tokens.login(2);
		assertThatThrownBy(() -> service.read("token", "amitie"))
				.hasMessage(RankingGroups.UNKNOWN);
		assertThatThrownBy(() -> service.read("nope", "classe"))
				.hasMessage(ReadChildRankingService.INVALID_SESSION_MESSAGE);
	}

	@Test
	void photoOfSomeoneOutsideTheGroupIsMissing() {
		roster.add(child(1, "Lina", 120, "2026-01-03T00:00:00Z"));
		roster.add(new VisibleChild(
				4, "Sam", 8, "CM1", 99L, "Autre", "France", 50, null, true, Instant.parse("2026-01-01T00:00:00Z")));
		roster.photos.put(4L, new ChildPhoto(new byte[]{1}, "image/png"));
		tokens.login(1);

		assertThatThrownBy(() -> service.photo("token", "classe", 4L))
				.hasMessage(ReadChildRankingService.PHOTO_MISSING_MESSAGE);
	}

	private static VisibleChild child(long id, String name, int xp, String created) {
		return new VisibleChild(
				id, name, 8, "CE1", 10L, "École Nouvelle", "Belgique", xp, "fox", false, Instant.parse(created));
	}

	private static final class MemoryTokens implements ChildSessionTokenPort {
		private Long childId;

		void login(long childId) {
			this.childId = childId;
		}

		@Override
		public com.appv4.auth.application.result.JwtTokenResult issue(long childId, String firstName) {
			return null;
		}

		@Override
		public Optional<ChildSession> read(String rawToken) {
			if ("token".equals(rawToken) && childId != null) {
				return Optional.of(new ChildSession(childId));
			}
			return Optional.empty();
		}
	}

	private static final class MemoryCases implements ChildRankingCasesPort {
		private final Map<Long, RankingSelection> saved = new LinkedHashMap<>();

		@Override
		public RankingSelection selection(long childId) {
			return saved.getOrDefault(childId, RankingSelection.shown());
		}

		@Override
		public Map<Long, RankingSelection> selection(java.util.Collection<Long> childIds) {
			Map<Long, RankingSelection> found = new LinkedHashMap<>();
			for (Long childId : childIds) {
				found.put(childId, selection(childId));
			}
			return found;
		}

		@Override
		public void save(long childId, RankingSelection selection) {
			saved.put(childId, selection);
		}
	}

	private static final class MemoryAvatars implements ChildAvatarPort {
		private final Map<Long, StoredAvatar> saved = new LinkedHashMap<>();

		@Override
		public Map<Long, StoredAvatar> findByChildIds(List<Long> childIds) {
			Map<Long, StoredAvatar> found = new LinkedHashMap<>();
			for (Long childId : childIds) {
				if (saved.containsKey(childId)) {
					found.put(childId, saved.get(childId));
				}
			}
			return found;
		}

		@Override
		public void save(long childId, String styleCode, String presetId, List<StoredPin> pins) {
			saved.put(childId, new StoredAvatar(childId, styleCode, presetId, pins));
		}
	}

	private static final class MemoryRoster implements ChildRankingRosterPort {
		private final List<VisibleChild> children = new ArrayList<>();
		private final Map<Long, ChildPhoto> photos = new LinkedHashMap<>();

		void add(VisibleChild child) {
			children.add(child);
		}

		@Override
		public List<VisibleChild> visibleChildren() {
			return List.copyOf(children);
		}

		@Override
		public Optional<ChildPhoto> photo(long childId) {
			return Optional.ofNullable(photos.get(childId));
		}
	}
}
