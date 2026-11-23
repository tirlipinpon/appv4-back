package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.HumeurChoicePort;
import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;

class HumeurLibraryServiceTest {

	private static final long ADMIN = 1L;
	private static final long PARENT = 2L;

	private final ActiveRolePort roles = mock(ActiveRolePort.class);
	private final MemoryChoices choices = new MemoryChoices();
	private HumeurLibraryService library;

	@BeforeEach
	void admin() {
		when(roles.hasAdminRole(ADMIN)).thenReturn(true);
		when(roles.hasAdminRole(PARENT)).thenReturn(false);
		library = new HumeurLibraryService(roles, choices);
	}

	@Test
	void uneBibliothequeVideSeLitVide() {
		assertThat(library.list(ADMIN)).isEmpty();
	}

	@Test
	void unMotSeulSeRelitSansIcone() {
		HumeurChoice saved = library.create(ADMIN, "   ", "  joie  ");

		assertThat(saved.icon()).isNull();
		assertThat(saved.word()).isEqualTo("joie");
		assertThat(library.list(ADMIN)).containsExactly(saved);
	}

	@Test
	void uneIconeSeuleSeRelitSansMot() {
		HumeurChoice saved = library.create(ADMIN, "  🙂  ", null);

		assertThat(saved.icon()).isEqualTo("🙂");
		assertThat(saved.word()).isNull();
		assertThat(library.list(ADMIN)).containsExactly(saved);
	}

	@Test
	void deuxChoixRestentDansLOrdreDeCreation() {
		HumeurChoice first = library.create(ADMIN, null, "calme");
		HumeurChoice second = library.create(ADMIN, "😊", "joie");

		assertThat(library.list(ADMIN)).containsExactly(first, second);
	}

	@Test
	void unChoixVideEstRefuseEtLaListeNeChangePas() {
		HumeurChoice kept = library.create(ADMIN, null, "joie");

		assertThatThrownBy(() -> library.create(ADMIN, null, "  "))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.BAD_REQUEST);
		assertThat(library.list(ADMIN)).containsExactly(kept);
	}

	@Test
	void modifierLeMotGardeLIdentifiantEtLaPlace() {
		HumeurChoice first = library.create(ADMIN, null, "calme");
		HumeurChoice second = library.create(ADMIN, "😊", "joie");

		HumeurChoice updated = library.update(ADMIN, first.id(), null, "fatigue");

		assertThat(updated.id()).isEqualTo(first.id());
		assertThat(updated.word()).isEqualTo("fatigue");
		assertThat(updated.icon()).isNull();
		assertThat(library.list(ADMIN)).containsExactly(updated, second);
	}

	@Test
	void retirerEnleveLeChoix() {
		HumeurChoice kept = library.create(ADMIN, null, "calme");
		HumeurChoice removed = library.create(ADMIN, null, "joie");

		library.delete(ADMIN, removed.id());

		assertThat(library.list(ADMIN)).containsExactly(kept);
	}

	@Test
	void unChoixInconnuNeChangePasLaListe() {
		HumeurChoice kept = library.create(ADMIN, null, "calme");

		assertThatThrownBy(() -> library.update(ADMIN, 99L, null, "joie"))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);
		assertThat(library.list(ADMIN)).containsExactly(kept);
	}

	@Test
	void unParentEstRefuseEtLaListeNeChangePas() {
		assertThatThrownBy(() -> library.create(PARENT, null, "joie"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(HumeurLibraryService.FORBIDDEN);
		assertThat(library.list(ADMIN)).isEmpty();
	}

	private static final class MemoryChoices implements HumeurChoicePort {

		private final List<HumeurChoice> rows = new ArrayList<>();
		private long nextId = 1L;

		@Override
		public List<HumeurChoice> list() {
			return rows.stream().sorted(Comparator.comparingLong(HumeurChoice::id)).toList();
		}

		@Override
		public HumeurChoice insert(String icon, String word) {
			HumeurChoice created = new HumeurChoice(nextId++, icon, word);
			rows.add(created);
			return created;
		}

		@Override
		public Optional<HumeurChoice> find(long id) {
			return rows.stream().filter(row -> row.id() == id).findFirst();
		}

		@Override
		public HumeurChoice update(long id, String icon, String word) {
			for (int index = 0; index < rows.size(); index++) {
				if (rows.get(index).id() == id) {
					HumeurChoice updated = new HumeurChoice(id, icon, word);
					rows.set(index, updated);
					return updated;
				}
			}
			throw new IllegalStateException("choix absent");
		}

		@Override
		public void delete(long id) {
			rows.removeIf(row -> row.id() == id);
		}
	}
}
