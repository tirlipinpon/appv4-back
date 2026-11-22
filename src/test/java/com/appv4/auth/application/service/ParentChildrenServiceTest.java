package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.port.in.ParentChildrenUseCase.NewChildCommand;
import com.appv4.auth.application.port.in.ReadParentHumeursUseCase.ParentChildHumeur;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.out.ChildSharePort;
import com.appv4.auth.application.port.out.ChildSharePort.StoredInvitation;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredAvatar;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredPin;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildChanges;
import com.appv4.auth.application.port.out.ChildPort.ChildLinkView;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildPort.InterestCopy;
import com.appv4.auth.application.port.out.ChildPort.NewChild;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.FollowingChild;
import com.appv4.auth.application.port.out.ChildSubjectPort.SubjectChildLink;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.result.ParentChildrenView.ChildCard;
import com.appv4.auth.application.result.ParentChildrenView.KinshipMark;
import com.appv4.auth.application.result.ChildAvatarView;

class ParentChildrenServiceTest {

	private MemoryChildren children;
	private AuthUserAccountPort accounts;
	private List<CatalogSchool> catalog;
	private ChildAvatarPort avatars;
	private ManualClock clock;
	private ParentChildrenService service;

	@BeforeEach
	void setUp() {
		children = new MemoryChildren();
		catalog = new ArrayList<>();
		PasswordEncoder encoder = new PasswordEncoder() {
			@Override
			public String encode(CharSequence rawPassword) {
				return "hash:" + rawPassword;
			}

			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				return encodedPassword.equals("hash:" + rawPassword);
			}
		};
		NoChildSubjects links = new NoChildSubjects();
		TeacherQuestionPort questions = mock(TeacherQuestionPort.class);
		when(questions.countBySubjectIds(anyList())).thenReturn(Map.of());
		avatars = mock(ChildAvatarPort.class);
		when(avatars.findByChildId(anyLong())).thenReturn(Optional.empty());
		clock = new ManualClock(Instant.parse("2026-03-01T10:00:00Z"));
		accounts = mock(AuthUserAccountPort.class);
		service = new ParentChildrenService(
				children, () -> catalog, encoder, links,
				new ChildSubjectService(children, links, () -> catalog, questions),
				avatars,
				CharacterAvatarCatalog.load(),
				clock,
				children,
				accounts);
	}

	@Test
	void listReadsTheCharactersOfItsOwnFamilyInOneBatch() {
		ChildCard created = service.add(1L, "parent", command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));
		ChildCard sibling = service.add(1L, "parent",
				command("Noah", "Martin", "8", "France", "CE1", null, "bear", "1144", "1144"));
		service.add(2L, "parent", command("Camille", "Dubois", "7", "France", "CE1", null, "fox", "4821", "4821"));

		assertThat(created.character().styleCode()).isEqualTo("lorelei");
		assertThat(created.character().options()).isEmpty();
		assertThat(created.character().pins()).isEmpty();

		StoredAvatar avatar = new StoredAvatar(created.id(), "lorelei", "bare",
				List.of(new StoredPin("mouth", "happy01"), new StoredPin("hair", "nope")));
		List<Long> familyIds = List.of(created.id(), sibling.id());
		when(avatars.findByChildIds(familyIds)).thenReturn(Map.of(created.id(), avatar));
		clearInvocations(avatars);
		List<ChildCard> family = service.list(1L, "parent").children();

		assertThat(family).extracting(ChildCard::id).containsExactly(created.id(), sibling.id());
		ChildCard saved = family.getFirst();
		assertThat(saved.character().styleCode()).isEqualTo("lorelei");
		assertThat(saved.character().options()).containsKey("backgroundColor");
		assertThat(saved.character().pins()).extracting(pin -> pin.zoneCode()).containsExactly("mouth");
		assertThat(family.getLast().character()).isEqualTo(created.character());
		verify(avatars).findByChildIds(familyIds);
		verifyNoMoreInteractions(avatars);

		when(avatars.findByChildId(created.id())).thenReturn(Optional.of(avatar));
		ChildLoginPort login = mock(ChildLoginPort.class);
		when(login.findActiveById(created.id())).thenReturn(Optional.of(new ChildLoginCandidate(created.id(), "Léa", "hash")));
		ChildSessionTokenPort tokens = mock(ChildSessionTokenPort.class);
		when(tokens.read("lea")).thenReturn(Optional.of(new ChildSession(created.id())));
		var profiles = new ReadChildAvatarService(
				login, tokens, mock(ChildProgressPort.class), avatars, CharacterAvatarCatalog.load());
		ChildAvatarView profile = profiles.avatar("lea");
		assertThat(profile.styleCode()).isEqualTo(saved.character().styleCode());
		assertThat(profile.presets()).filteredOn(preset -> preset.id().equals(profile.presetId()))
				.singleElement().satisfies(preset -> assertThat(preset.options()).isEqualTo(saved.character().options()));
		assertThat(profile.zones()).filteredOn(zone -> zone.selectedCode() != null)
				.extracting(ChildAvatarView.ZoneCard::selectedCode)
				.containsExactly(saved.character().pins().getFirst().variantCode());

		when(login.findActiveById(sibling.id())).thenReturn(Optional.of(new ChildLoginCandidate(sibling.id(), "Noah", "hash")));
		when(tokens.read("noah")).thenReturn(Optional.of(new ChildSession(sibling.id())));
		ChildAvatarView defaultProfile = profiles.avatar("noah");
		assertThat(defaultProfile.styleCode()).isEqualTo(family.getLast().character().styleCode());
		assertThat(defaultProfile.presetId()).isNull();
		assertThat(defaultProfile.zones()).allMatch(zone -> zone.selectedCode() == null);
	}

	@Test
	void parentAddsAChildWithAvatarAndFranceGradeWithoutSchool() {
		ChildCard card = service.add(1L, "parent", command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));

		assertThat(card.firstName()).isEqualTo("Léa");
		assertThat(card.grade()).isEqualTo("CE1");
		assertThat(card.curriculum()).isEqualTo(SchoolCurricula.FRANCE);
		assertThat(card.schoolId()).isNull();
		assertThat(card.avatarCode()).isEqualTo("fox");
		assertThat(card.hasPhoto()).isFalse();
		assertThat(children.last.pinHash()).isEqualTo("hash:4821").isNotEqualTo("4821");
		assertThat(service.list(1L, "parent").children()).extracting(ChildCard::firstName).containsExactly("Léa");
		assertThat(service.list(2L, "parent").children()).isEmpty();
	}

	@Test
	void sameNameIsASecondChildAndBelgiumGradeStaysOnItsOwnCard() {
		service.add(1L, "parent", command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));
		service.add(1L, "parent", command("Léa", "Martin", "5", "France", "Grande section (GS)", null, "bear", "1111", "1111"));

		assertThat(service.list(1L, "parent").children()).hasSize(2);
		assertThat(children.all).extracting(ChildSaved::grade).containsExactly("CE1", "Grande section (GS)");
	}

	@Test
	void schoolLimitsGradesToWhatItOffers() {
		catalog.add(new CatalogSchool(4L, "Ecole Nouvelle", "Belgique", "Maternelle ordinaire, Primaire ordinaire"));

		ChildCard saved = service.add(1L, "parent",
				command("Noah", "Martin", "8", "Belgique", "3e primaire", 4L, "owl", "9090", "9090"));
		assertThat(saved.schoolName()).isEqualTo("Ecole Nouvelle");
		assertThat(saved.grade()).isEqualTo("3e primaire");
		catalog.add(new CatalogSchool(5L, "Other school", "Belgique", "Primaire"));
		assertThat(service.list(1L, "parent").schools()).extracting(item -> item.id()).containsExactly(4L);

		assertThatThrownBy(() -> service.add(1L, "parent",
				command("Noah", "Martin", "8", "Belgique", "1re secondaire", 4L, "owl", "9090", "9090")))
				.hasMessage(ParentChildrenService.GRADE_NOT_OFFERED);
		assertThatThrownBy(() -> service.add(1L, "parent",
				command("Noah", "Martin", "8", "Belgique", "CP", 4L, "owl", "9090", "9090")))
				.hasMessage(ParentChildrenService.GRADE_NOT_OFFERED);
		assertThat(children.all).hasSize(1);
	}

	@Test
	void blankNameBadAgeAndPinDoNotCreateAChild() {
		assertRefused("  ", "Martin", "7", "fox", "4821", "4821", ParentChildrenService.FIRST_NAME_REQUIRED);
		assertRefused("Léa", " ", "7", "fox", "4821", "4821", ParentChildrenService.LAST_NAME_REQUIRED);
		assertRefused("Léa", "Martin", "1", "fox", "4821", "4821", ParentChildrenService.AGE_INVALID);
		assertRefused("Léa", "Martin", "sept", "fox", "4821", "4821", ParentChildrenService.AGE_INVALID);
		assertRefused("Léa", "Martin", "18", "fox", "12", "12", ParentChildrenService.PIN_FORMAT);
		assertRefused("Léa", "Martin", "2", "fox", "4821", "4822", ParentChildrenService.PIN_MISMATCH);
		assertRefused("Léa", "Martin", "7", null, "4821", "4821", ParentChildrenService.IMAGE_REQUIRED);
		NewChildCommand teacherAttempt = command(
				"Léa", "Martin", "7", "France", "CP", null, "fox", "4821", "4821");
		assertThatThrownBy(() -> service.add(1L, "teacher", teacherAttempt))
				.hasMessage(SpaceAccess.DENIED);
		assertThat(children.all).isEmpty();
	}

	@Test
	void photoReplacesAvatarAndATextFileIsRefused() {
		String jpeg = Base64.getEncoder().encodeToString(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3});
		NewChildCommand withPhoto = new NewChildCommand(
				"Noah",
				"Martin",
				"GARCON",
				"8",
				"Belgique",
				"3e primaire",
				null,
				"fox",
				jpeg,
				"image/jpeg",
				"9090",
				"9090",
				null,
				null,
				List.of());
		ChildCard card = service.add(1L, "parent", withPhoto);
		assertThat(card.avatarCode()).isNull();
		assertThat(card.hasPhoto()).isTrue();
		assertThat(service.photo(1L, "parent", card.id()).contentType()).isEqualTo("image/jpeg");

		NewChildCommand text = new NewChildCommand(
				"Noah", "Martin", "GARCON", "8", "Belgique", "3e primaire", null, null,
				Base64.getEncoder().encodeToString("bonjour".getBytes()),
				"text/plain", "9090", "9090", null, null, List.of());
		assertThatThrownBy(() -> service.add(1L, "parent", text))
				.hasMessage(ParentChildrenService.NOT_AN_IMAGE);
	}

	private void assertRefused(
			String firstName,
			String lastName,
			String age,
			String avatar,
			String pin,
			String confirmation,
			String message) {
		assertThatThrownBy(() -> service.add(
				1L,
				"parent",
				command(firstName, lastName, age, "France", "CP", null, avatar, pin, confirmation)))
				.hasMessage(message);
	}

	private static NewChildCommand command(
			String firstName,
			String lastName,
			String age,
			String country,
			String grade,
			Long schoolId,
			String avatar,
			String pin,
			String confirmation) {
		return new NewChildCommand(
				firstName,
				lastName,
				"FILLE",
				age,
				country,
				grade,
				schoolId,
				avatar,
				null,
				null,
				pin,
				confirmation,
				null,
				null,
				List.of());
	}

	@Test
	void creatingFromAChildShowsTheSamePhraseAndLeavesTheSourceUntouched() {
		ChildCard lea = service.add(1L, "parent",
				command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));
		ChildSaved sourceBefore = children.all.get(0);

		ChildCard noah = service.add(1L, "parent",
				from(lea.id(), "Noah", "Martin", "8", "France", "CM1", "fox", "1357", "1357", "FRERE"));

		assertThat(noah.linkPhrases()).containsExactly("Léa est la sœur de Noah");
		assertThat(noah.links()).containsExactly(new KinshipMark(lea.id(), "Sœur"));
		assertThat(noah.schoolId()).isNull();
		assertThat(children.all.get(0)).isEqualTo(sourceBefore);
		assertThat(children.inserted.get(0).pinHash()).isEqualTo("hash:4821");
		assertThat(children.inserted.get(1).pinHash()).isEqualTo("hash:1357");

		service.add(1L, "parent",
				from(lea.id(), "Tom", "Martin", "7", "France", "CE1", "fox", "2468", "2468", "AUCUN"));
		service.add(1L, "parent",
				from(lea.id(), "Emma", "Durand", "7", "Belgique", "2e primaire", "owl", "9753", "9753", "COUSINE"));

		List<ChildCard> cards = service.list(1L, "parent").children();
		assertThat(cards).extracting(ChildCard::firstName).containsExactly("Léa", "Noah", "Tom", "Emma");
		assertThat(cards.get(0).age()).isEqualTo(7);
		assertThat(cards.get(0).grade()).isEqualTo("CE1");
		assertThat(cards.get(0).linkPhrases()).containsExactly(
				"Noah est le frère de Léa",
				"Emma est la cousine de Léa");
		assertThat(cards.get(1).linkPhrases()).containsExactly("Léa est la sœur de Noah");
		assertThat(cards.get(2).linkPhrases()).isEmpty();
		assertThat(cards.get(3).linkPhrases()).containsExactly("Léa est la cousine de Emma");
		assertThat(cards.get(0).links()).containsExactly(
				new KinshipMark(cards.get(1).id(), "Frère"),
				new KinshipMark(cards.get(3).id(), "Cousine"));
		assertThat(cards.get(1).links()).containsExactly(new KinshipMark(cards.get(0).id(), "Sœur"));
		assertThat(cards.get(2).links()).isEmpty();
		assertThat(cards.get(3).links()).containsExactly(new KinshipMark(cards.get(0).id(), "Cousine"));
		assertThat(children.inserted.get(0).pinHash()).isEqualTo("hash:4821");
		assertThat(service.list(2L, "parent").children()).isEmpty();
	}

	@Test
	void kinshipPhrasesUseTheClosedList() {
		assertThat(ChildKinship.phrase("Nina", "FILLE", "SOEUR", "Léa")).isEqualTo("Nina est la sœur de Léa");
		assertThat(ChildKinship.phrase("Lino", "GARCON", "DEMI_SOEUR", "Léa")).isEqualTo("Lino est le demi-frère de Léa");
		assertThat(ChildKinship.phrase("Noe", "FILLE", "DEMI_FRERE", "Léa")).isEqualTo("Noe est la demi-sœur de Léa");
		assertThat(ChildKinship.phrase("Leo", "GARCON", "JUMELLE", "Léa")).isEqualTo("Leo est le jumeau de Léa");
		assertThat(ChildKinship.phrase("Lina", "FILLE", "JUMEAU", "Léa")).isEqualTo("Lina est la jumelle de Léa");
		assertThat(ChildKinship.phrase("Paul", "GARCON", "COUSINE", "Léa")).isEqualTo("Paul est le cousin de Léa");
		assertThat(ChildKinship.word("SOEUR", "GARCON")).isEqualTo("Frère");
		assertThat(ChildKinship.word("FRERE", "FILLE")).isEqualTo("Sœur");
		assertThat(ChildKinship.word("COUSIN", "NON_GENRE")).isEqualTo("Cousin/cousine");
	}

	@Test
	void refusedCopyCreatesNeitherChildNorLink() {
		ChildCard lea = service.add(1L, "parent",
				command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));

		assertThatThrownBy(() -> service.add(1L, "parent",
				from(lea.id(), " ", "Martin", "7", "France", "CE1", "fox", "1111", "1111", "FRERE")))
				.hasMessage(ParentChildrenService.FIRST_NAME_REQUIRED);
		assertThatThrownBy(() -> service.add(1L, "parent",
				from(lea.id(), "Lina", "Martin", "7", "France", "CE1", "fox", "1111", "1112", "FRERE")))
				.hasMessage(ParentChildrenService.PIN_MISMATCH);
		assertThatThrownBy(() -> service.add(1L, "parent",
				from(lea.id(), "Lina", "Martin", "7", "France", "CE1", "fox", "22", "22", "FRERE")))
				.hasMessage(ParentChildrenService.PIN_FORMAT);
		assertThatThrownBy(() -> service.add(1L, "parent",
				from(lea.id(), "Lina", "Martin", "7", "France", "CE1", "fox", "abcd", "abcd", "FRERE")))
				.hasMessage(ParentChildrenService.PIN_FORMAT);
		assertThatThrownBy(() -> service.add(1L, "parent",
				from(lea.id(), "Nico", "Martin", "1", "France", "CE1", "fox", "3333", "3333", "JUMEAU")))
				.hasMessage(ParentChildrenService.AGE_INVALID);
		assertThatThrownBy(() -> service.add(1L, "parent",
				from(lea.id(), "Nico", "Martin", "7", "Belgique", " ", "fox", "3333", "3333", "JUMEAU")))
				.hasMessage(ParentChildrenService.GRADE_REQUIRED);
		assertThatThrownBy(() -> service.add(1L, "parent",
				from(lea.id(), "Ami", "Martin", "7", "France", "CE1", "fox", "4444", "4444", "AMI")))
				.hasMessage(ParentChildrenService.LINK_UNKNOWN);
		assertThatThrownBy(() -> service.add(1L, "parent",
				from(null, "Nina", "Martin", "7", "France", "CE1", "fox", "8642", "8642", "SOEUR")))
				.hasMessage(ParentChildrenService.LINK_NEEDS_CHILD);
		assertThatThrownBy(() -> service.add(2L, "parent",
				from(lea.id(), "Nina", "Martin", "7", "France", "CE1", "fox", "8642", "8642", "SOEUR")))
				.hasMessage(ParentChildrenService.CHILD_UNKNOWN);
		assertThatThrownBy(() -> service.add(1L, "teacher",
				from(lea.id(), "Nina", "Martin", "7", "France", "CE1", "fox", "8642", "8642", "SOEUR")))
				.hasMessage(SpaceAccess.DENIED);

		assertThat(children.all).hasSize(1);
		assertThat(children.links).isEmpty();
		assertThat(children.inserted).extracting(NewChild::pinHash).containsExactly("hash:4821");
	}

	@Test
	void updateKeepsThePinAndHideRemovesTheCardFromTheList() {
		ChildCard created = service.add(1L, "parent",
				command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));
		NewChildCommand rename = new NewChildCommand(
				"Léa", "Durand", "FILLE", "8", "France", "CP", null, null, null, null, "", "", null, null, List.of());

		ChildCard updated = service.update(1L, "parent", created.id(), rename);

		assertThat(updated.lastName()).isEqualTo("Durand");
		assertThat(updated.active()).isTrue();
		assertThat(children.inserted.get(0).pinHash()).isEqualTo("hash:4821");
		service.deactivate(1L, "parent", created.id());
		assertThat(service.list(1L, "parent").children().get(0).active()).isFalse();
		service.reactivate(1L, "parent", created.id());
		assertThat(service.list(1L, "parent").children().get(0).active()).isTrue();
		service.hide(1L, "parent", created.id());
		assertThat(service.list(1L, "parent").children()).isEmpty();
		assertThatThrownBy(() -> service.update(1L, "parent", created.id(), rename))
				.hasMessage(ParentChildrenService.CHILD_UNKNOWN);
	}

	@Test
	void updateLinksAnExistingChildAndChangesTheWordWithoutASecondRow() {
		ChildCard lea = service.add(1L, "parent",
				command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));
		ChildCard noah = service.add(1L, "parent",
				new NewChildCommand(
						"Noah", "Martin", "GARCON", "8", "France", "CM1",
						null, "cat", null, null, "1357", "1357", null, null, List.of()));

		ChildCard linked = service.update(1L, "parent", noah.id(),
				rename(noah, lea.id(), "FRERE"));

		assertThat(linked.links()).containsExactly(new KinshipMark(lea.id(), "Sœur"));
		assertThat(service.update(1L, "parent", noah.id(), rename(noah, null, null)).links())
				.containsExactly(new KinshipMark(lea.id(), "Sœur"));
		assertThat(service.list(1L, "parent").children().get(0).links())
				.containsExactly(new KinshipMark(noah.id(), "Frère"));

		ChildCard changed = service.update(1L, "parent", lea.id(),
				rename(lea, noah.id(), "COUSIN"));

		assertThat(changed.links()).containsExactly(new KinshipMark(noah.id(), "Cousin"));
		assertThat(children.links).hasSize(1);
		assertThat(service.update(1L, "parent", noah.id(), rename(noah, lea.id(), "AUCUN")).links())
				.isEmpty();
		assertThat(service.list(1L, "parent").children().get(0).links()).isEmpty();
		assertThat(children.links).isEmpty();
		assertThatThrownBy(() -> service.update(1L, "parent", noah.id(), rename(noah, noah.id(), "FRERE")))
				.hasMessage(ParentChildrenService.LINK_SELF);
	}

	private static NewChildCommand rename(ChildCard child, Long otherId, String link) {
		return new NewChildCommand(
				child.firstName(),
				child.lastName(),
				child.sex(),
				String.valueOf(child.age()),
				child.country(),
				child.grade(),
				child.schoolId(),
				null,
				null,
				null,
				"",
				"",
				otherId,
				link,
				List.of());
	}

	private static NewChildCommand from(
			Long sourceId,
			String firstName,
			String lastName,
			String age,
			String country,
			String grade,
			String avatar,
			String pin,
			String confirmation,
			String link) {
		return new NewChildCommand(
				firstName,
				lastName,
				sexFor(link),
				age,
				country,
				grade,
				null,
				avatar,
				null,
				null,
				pin,
				confirmation,
				sourceId,
				link,
				List.of());
	}

	/** Le code du test donne un sexe cohérent. En production, c'est la fiche qui décide. */
	private static String sexFor(String link) {
		if (link == null) {
			return "FILLE";
		}
		return switch (link) {
			case "FRERE", "DEMI_FRERE", "JUMEAU", "COUSIN" -> "GARCON";
			default -> "FILLE";
		};
	}

	@Test
	void parentKeepsInterestsSortedAndSharingOneMoment() {
		ChildCard card = service.add(1L, "parent", fiche(List.of("Pokémon", "formule 1")));

		assertThat(card.interests()).containsExactly("formule 1", "Pokémon");
		assertThat(service.list(1L, "parent").children().getFirst().interests())
				.containsExactly("formule 1", "Pokémon");
		assertThat(children.copies(card.id())).extracting(InterestCopy::text)
				.containsExactly("Pokémon", "formule 1");
		assertThat(children.copies(card.id())).extracting(InterestCopy::writtenAt)
				.containsOnly(Instant.parse("2026-03-01T10:00:00Z"));
	}

	@Test
	void emptyInterestListClearsCopiesAndCollapsedSpacesAreStoredOnce() {
		ChildCard card = service.add(1L, "parent", fiche(List.of("Pokémon")));

		ChildCard cleared = service.update(1L, "parent", card.id(), revise(card, "Léa"));
		assertThat(cleared.interests()).isEmpty();
		assertThat(children.copies(card.id())).isEmpty();

		ChildCard spaced = service.update(1L, "parent", card.id(), revise(card, "Léa", "star  wars"));
		assertThat(spaced.interests()).containsExactly("star wars");
		ChildCard tabbed = service.update(1L, "parent", card.id(), revise(card, "Léa", "star\twars"));
		assertThat(tabbed.interests()).containsExactly("star wars");
		ChildCard broken = service.update(1L, "parent", card.id(), revise(card, "Léa", "star\nwars"));
		assertThat(children.copies(card.id())).extracting(InterestCopy::text).containsExactly("star wars");
		assertThat(broken.interests()).containsExactly("star wars");
		ChildCard separated = service.update(1L, "parent", card.id(), revise(card, "Léa", "star\u2028wars"));
		assertThat(separated.interests()).containsExactly("star wars");
	}

	@Test
	void refusedInterestListLeavesTheFicheAndTheCopiesUntouched() {
		ChildCard card = service.add(1L, "parent", fiche(List.of("foot")));

		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Zoé", "pokemon", "Pokémon")))
				.hasMessage(ChildInterestList.DUPLICATE);
		assertThat(service.list(1L, "parent").children().getFirst().firstName()).isEqualTo("Léa");
		assertThat(children.copies(card.id())).extracting(InterestCopy::text).containsExactly("foot");

		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "a")))
				.hasMessage(ChildInterestList.LENGTH);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "a".repeat(41))))
				.hasMessage(ChildInterestList.LENGTH);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "   ")))
				.hasMessage(ChildInterestList.LENGTH);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "go\uD83D\uDE00")))
				.hasMessage(ChildInterestList.CHARSET);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "tiret\u2014long")))
				.hasMessage(ChildInterestList.CHARSET);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "go\fto")))
				.hasMessage(ChildInterestList.CHARSET);
		assertThatThrownBy(() -> service.add(1L, "parent", fiche(null)))
				.hasMessage(ChildInterestList.MISSING);
		assertThat(children.all).hasSize(1);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", (String[]) null)))
				.hasMessage(ChildInterestList.MISSING);
		assertThat(service.list(1L, "parent").children().getFirst().firstName()).isEqualTo("Léa");
	}

	@Test
	void interestRulesStopAtTheFirstFailureAndKeepDistinctWords() {
		ChildCard card = service.add(1L, "parent", fiche(List.of("foot")));
		List<String> eleven = new ArrayList<>();
		for (int index = 1; index <= 11; index++) {
			eleven.add("mot" + index);
		}
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", eleven.toArray(String[]::new))))
				.hasMessage(ChildInterestList.MAXIMUM);

		List<String> badFirst = new ArrayList<>();
		badFirst.add("x");
		badFirst.addAll(eleven.subList(0, 10));
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", badFirst.toArray(String[]::new))))
				.hasMessage(ChildInterestList.LENGTH);

		ChildCard both = service.update(1L, "parent", card.id(), revise(
				card, "Léa", "dinosaure", "dinosaures", "formule 1", "formule-1", "l'eau", "leau", "sø", "so"));
		assertThat(both.interests()).containsExactly(
				"dinosaure", "dinosaures", "formule 1", "formule-1", "l'eau", "leau", "so", "sø");
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "œufs", "oeufs")))
				.hasMessage(ChildInterestList.DUPLICATE);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "bå", "ba")))
				.hasMessage(ChildInterestList.DUPLICATE);
		assertThatThrownBy(() -> service.update(1L, "parent", card.id(), revise(card, "Léa", "l'eau", "l\u2019eau")))
				.hasMessage(ChildInterestList.DUPLICATE);

		ChildCard numbers = service.update(1L, "parent", card.id(), revise(card, "Léa", "2b", "10"));
		assertThat(numbers.interests()).containsExactly("10", "2b");
	}

	@Test
	void rewritingTheSameInterestKeepsItsMoment() {
		ChildCard card = service.add(1L, "parent", fiche(List.of("pokemon", "foot")));
		Instant first = children.copies(card.id()).getFirst().writtenAt();
		clock.advanceMinutes(5);

		ChildCard renamed = service.update(1L, "parent", card.id(), revise(card, "Léa", "Pokémon"));
		assertThat(renamed.interests()).containsExactly("Pokémon");
		assertThat(children.copies(card.id())).singleElement().satisfies(copy -> {
			assertThat(copy.text()).isEqualTo("Pokémon");
			assertThat(copy.writtenAt()).isEqualTo(first);
		});

		clock.advanceMinutes(5);
		ChildCard next = service.update(1L, "parent", card.id(),
				revise(card, "Léa", "Pokémon", "danse", "judo"));
		assertThat(next.interests()).containsExactly("danse", "judo", "Pokémon");
		assertThat(children.copies(card.id())).extracting(InterestCopy::text)
				.containsExactly("Pokémon", "danse", "judo");
		Instant kept = children.copies(card.id()).getFirst().writtenAt();
		Instant added = children.copies(card.id()).get(1).writtenAt();
		assertThat(kept).isEqualTo(first);
		assertThat(children.copies(card.id()).get(2).writtenAt()).isEqualTo(added).isAfter(first);

		ChildCard straight = service.update(1L, "parent", card.id(), revise(card, "Léa", "l'eau"));
		Instant quoted = children.copies(card.id()).getFirst().writtenAt();
		clock.advanceMinutes(5);
		ChildCard curly = service.update(1L, "parent", card.id(), revise(card, "Léa", "l\u2019eau"));
		assertThat(curly.interests()).containsExactly("l\u2019eau");
		assertThat(children.copies(card.id()).getFirst().writtenAt()).isEqualTo(quoted);
	}

	@Test
	void interestsStayWithTheChildAndDoNotCopyFromASibling() {
		ChildCard lea = service.add(1L, "parent", fiche(List.of("espace")));
		ChildCard noah = service.add(1L, "parent",
				from(lea.id(), "Noah", "Martin", "8", "France", "CM1", "fox", "1357", "1357", "FRERE"));

		assertThat(noah.interests()).isEmpty();
		assertThat(service.list(1L, "parent").children().getFirst().interests()).containsExactly("espace");

		service.deactivate(1L, "parent", lea.id());
		assertThat(service.list(1L, "parent").children().getFirst().interests()).containsExactly("espace");
		service.hide(1L, "parent", lea.id());
		assertThat(children.copies(lea.id())).extracting(InterestCopy::text).containsExactly("espace");
		assertThatThrownBy(() -> service.update(1L, "parent", lea.id(), revise(lea, "Léa", "espace")))
				.hasMessage(ParentChildrenService.CHILD_UNKNOWN);

		assertThatThrownBy(() -> service.update(2L, "parent", noah.id(), revise(noah, "Noah", "foot")))
				.hasMessage(ParentChildrenService.CHILD_UNKNOWN);
		assertThat(children.copies(noah.id())).isEmpty();

		assertThatThrownBy(() -> service.add(1L, "teacher", fiche(List.of("foot"))))
				.hasMessage(SpaceAccess.DENIED);
		assertThatThrownBy(() -> service.list(1L, "child"))
				.hasMessage(SpaceAccess.DENIED);
		assertThatThrownBy(() -> service.update(1L, "admin", noah.id(), revise(noah, "Noah")))
				.hasMessage(SpaceAccess.DENIED);
	}

	@Test
	void anInvitationSharesOneFicheAfterTheNamedEmailAccepts() {
		ChildCard lea = service.add(1L, "parent", fiche(List.of("espace")));
		when(accounts.findByEmail("b@exemple.fr")).thenReturn(Optional.empty());
		when(accounts.findById(2L)).thenReturn(Optional.of(new AuthUserAccount(2L, "b@exemple.fr", "B", true)));

		assertThat(service.invite(1L, "parent", lea.id(), "  B@exemple.fr ").message())
				.isEqualTo(ParentChildrenService.INVITATION_SENT);
		assertThat(service.list(1L, "parent").children().getFirst().referent()).isTrue();
		assertThat(service.list(1L, "parent").children().getFirst().pendingInvitationCount()).isEqualTo(1);
		assertThat(service.list(2L, "parent").children()).isEmpty();
		assertThat(service.pendingInvitations(2L, "parent")).extracting(item -> item.firstName()).containsExactly("Léa");

		long invitationId = service.pendingInvitations(2L, "parent").getFirst().id();
		when(accounts.findById(9L)).thenReturn(Optional.of(new AuthUserAccount(9L, "autre@exemple.fr", "Z", true)));
		assertThatThrownBy(() -> service.acceptInvitation(9L, "parent", invitationId))
				.hasMessage(ParentChildrenService.WRONG_ACCOUNT);
		assertThat(service.list(9L, "parent").children()).isEmpty();

		ChildCard accepted = service.acceptInvitation(2L, "parent", invitationId);
		assertThat(accepted.id()).isEqualTo(lea.id());
		assertThat(service.list(1L, "parent").children()).extracting(ChildCard::id).containsExactly(lea.id());
		assertThat(service.list(2L, "parent").children()).extracting(ChildCard::id).containsExactly(lea.id());
		assertThat(service.list(2L, "parent").children().getFirst().referent()).isFalse();
		assertThat(service.list(2L, "parent").children().getFirst().pendingInvitationCount()).isZero();

		service.update(2L, "parent", lea.id(), revise(lea, "Léa", "espace", "foot"));
		assertThat(service.list(1L, "parent").children().getFirst().interests()).containsExactly("espace", "foot");

		assertThatThrownBy(() -> service.hide(2L, "parent", lea.id()))
				.hasMessage(ParentChildrenService.ONLY_REFERENT);
		assertThatThrownBy(() -> service.deactivate(2L, "parent", lea.id()))
				.hasMessage(ParentChildrenService.ONLY_REFERENT);
		assertThatThrownBy(() -> service.reactivate(2L, "parent", lea.id()))
				.hasMessage(ParentChildrenService.ONLY_REFERENT);
		assertThat(service.list(1L, "parent").children()).extracting(ChildCard::id).containsExactly(lea.id());

		when(accounts.findByEmail("c@exemple.fr")).thenReturn(Optional.empty());
		service.invite(1L, "parent", lea.id(), "c@exemple.fr");
		assertThatThrownBy(() -> service.invite(1L, "parent", lea.id(), "c@exemple.fr"))
				.hasMessage(ParentChildrenService.INVITATION_PENDING);
		when(accounts.findByEmail("b@exemple.fr")).thenReturn(Optional.of(new AuthUserAccount(2L, "b@exemple.fr", "B", true)));
		assertThatThrownBy(() -> service.invite(1L, "parent", lea.id(), "b@exemple.fr"))
				.hasMessage(ParentChildrenService.ALREADY_HOLDS);

		service.deactivate(1L, "parent", lea.id());
		assertThat(service.pendingInvitations(2L, "parent")).isEmpty();
		when(accounts.findById(3L)).thenReturn(Optional.of(new AuthUserAccount(3L, "c@exemple.fr", "C", true)));
		assertThat(service.pendingInvitations(3L, "parent")).extracting(item -> item.childId()).containsExactly(lea.id());

		service.hide(1L, "parent", lea.id());
		assertThat(service.list(1L, "parent").children()).isEmpty();
		assertThat(service.list(2L, "parent").children()).isEmpty();
		assertThat(service.pendingInvitations(3L, "parent")).isEmpty();
	}

	@Test
	void acceptingOneInvitationLeavesTheOtherAndACopyIsANewFiche() {
		ChildCard lea = service.add(1L, "parent", fiche(List.of()));
		when(accounts.findByEmail("b@exemple.fr")).thenReturn(Optional.empty());
		when(accounts.findByEmail("c@exemple.fr")).thenReturn(Optional.empty());
		service.invite(1L, "parent", lea.id(), "b@exemple.fr");
		service.invite(1L, "parent", lea.id(), "c@exemple.fr");
		when(accounts.findById(2L)).thenReturn(Optional.of(new AuthUserAccount(2L, "b@exemple.fr", "B", true)));
		when(accounts.findById(3L)).thenReturn(Optional.of(new AuthUserAccount(3L, "c@exemple.fr", "C", true)));

		service.acceptInvitation(2L, "parent", service.pendingInvitations(2L, "parent").getFirst().id());

		assertThat(service.pendingInvitations(3L, "parent")).hasSize(1);
		ChildCard copy = service.add(2L, "parent",
				from(lea.id(), "Noa", "Martin", "8", "France", "CE1", "fox", "1357", "1357", "AUCUN"));
		assertThat(service.list(1L, "parent").children()).extracting(ChildCard::id).containsExactly(lea.id());
		assertThat(service.list(2L, "parent").children()).extracting(ChildCard::id).containsExactly(lea.id(), copy.id());
		HumeurAnswerPort answers = mock(HumeurAnswerPort.class);
		when(answers.latest(anyLong())).thenReturn(Optional.empty());
		assertThat(new ReadParentHumeursService(children, answers).list(2L, "parent"))
				.extracting(ParentChildHumeur::id)
				.contains(lea.id());
		assertThatThrownBy(() -> service.invite(1L, "teacher", lea.id(), "d@exemple.fr"))
				.hasMessage(SpaceAccess.DENIED);
	}

	private static NewChildCommand fiche(List<String> interests) {
		return new NewChildCommand(
				"Léa", "Martin", "FILLE", "7", "France", "CE1", null, "fox",
				null, null, "4821", "4821", null, null, interests);
	}

	private static NewChildCommand revise(ChildCard child, String firstName, String... words) {
		return new NewChildCommand(
				firstName,
				child.lastName(),
				child.sex(),
				String.valueOf(child.age()),
				child.country(),
				child.grade(),
				child.schoolId(),
				null,
				null,
				null,
				"",
				"",
				null,
				null,
				words == null ? null : List.of(words));
	}

	private static final class MemoryChildren implements ChildPort, ChildSharePort {
		private final List<ChildSaved> all = new ArrayList<>();
		private final List<Share> shares = new ArrayList<>();
		private final List<StoredInvitation> invitations = new ArrayList<>();
		private long nextInvitation = 1;
		private final Map<Long, List<InterestCopy>> interests = new LinkedHashMap<>();
		private final List<NewChild> inserted = new ArrayList<>();
		private final List<StoredLink> links = new ArrayList<>();
		private final List<Boolean> activeFlags = new ArrayList<>();
		private final List<Boolean> hiddenFlags = new ArrayList<>();
		private NewChild last;
		private long nextId = 1;

		@Override
		public ChildSaved insert(NewChild child) {
			last = child;
			inserted.add(child);
			activeFlags.add(true);
			hiddenFlags.add(false);
			ChildSaved saved = new ChildSaved(
					nextId++,
					child.parentUserId(),
					child.firstName(),
					child.lastName(),
					child.sex(),
					child.age(),
					child.curriculum(),
					child.grade(),
					child.schoolId(),
					child.avatarCode(),
					child.photo() != null,
					true);
			all.add(saved);
			return saved;
		}

		@Override
		public java.util.Optional<ChildSaved> findOwned(long parentUserId, long childId) {
			for (int i = 0; i < all.size(); i++) {
				ChildSaved current = all.get(i);
				if (current.id() == childId && !hiddenFlags.get(i) && visibleTo(parentUserId, current)) {
					return java.util.Optional.of(shown(i));
				}
			}
			return java.util.Optional.empty();
		}

		@Override
		public java.util.Optional<ChildSaved> update(long parentUserId, long childId, ChildChanges changes) {
			for (int i = 0; i < all.size(); i++) {
				ChildSaved current = all.get(i);
				if (current.id() != childId || hiddenFlags.get(i) || !visibleTo(parentUserId, current)) {
					continue;
				}
				if (changes.pinHash() != null) {
					NewChild previous = inserted.get(i);
					inserted.set(i, new NewChild(
							previous.parentUserId(),
							changes.firstName(),
							changes.lastName(),
							changes.sex(),
							changes.age(),
							changes.curriculum(),
							changes.grade(),
							changes.schoolId(),
							changes.replaceImage() ? changes.avatarCode() : previous.avatarCode(),
							changes.replaceImage() ? changes.photo() : previous.photo(),
							changes.replaceImage() ? changes.photoContentType() : previous.photoContentType(),
							changes.pinHash()));
				}
				boolean hasPhoto = changes.replaceImage() ? changes.photo() != null : current.hasPhoto();
				String avatar = changes.replaceImage() ? changes.avatarCode() : current.avatarCode();
				ChildSaved saved = new ChildSaved(
						current.id(),
						current.parentUserId(),
						changes.firstName(),
						changes.lastName(),
						changes.sex(),
						changes.age(),
						changes.curriculum(),
						changes.grade(),
						changes.schoolId(),
						avatar,
						hasPhoto,
						activeFlags.get(i));
				all.set(i, saved);
				return java.util.Optional.of(saved);
			}
			return java.util.Optional.empty();
		}

		@Override
		public boolean deactivate(long parentUserId, long childId) {
			return mark(parentUserId, childId, false, null);
		}

		@Override
		public boolean reactivate(long parentUserId, long childId) {
			return mark(parentUserId, childId, true, null);
		}

		@Override
		public boolean hide(long parentUserId, long childId) {
			return mark(parentUserId, childId, null, true);
		}

		@Override
		public void insertLink(long parentUserId, long subjectChildId, long relatedChildId, String relation) {
			links.add(new StoredLink(parentUserId, subjectChildId, relatedChildId, relation));
		}

		@Override
		public void saveLink(long parentUserId, long subjectChildId, long relatedChildId, String relation) {
			for (int i = 0; i < links.size(); i++) {
				StoredLink link = links.get(i);
				boolean direct = link.subjectChildId() == subjectChildId
						&& link.relatedChildId() == relatedChildId;
				boolean reverse = link.subjectChildId() == relatedChildId
						&& link.relatedChildId() == subjectChildId;
				boolean samePair = link.parentUserId() == parentUserId && (direct || reverse);
				if (samePair) {
					links.set(i, new StoredLink(
							parentUserId, link.subjectChildId(), link.relatedChildId(), relation));
					return;
				}
			}
			insertLink(parentUserId, subjectChildId, relatedChildId, relation);
		}

		@Override
		public void deleteLink(long parentUserId, long subjectChildId, long relatedChildId) {
			links.removeIf(link -> {
				boolean direct = link.subjectChildId() == subjectChildId
						&& link.relatedChildId() == relatedChildId;
				boolean reverse = link.subjectChildId() == relatedChildId
						&& link.relatedChildId() == subjectChildId;
				return link.parentUserId() == parentUserId && (direct || reverse);
			});
		}

		@Override
		public List<ChildLinkView> findLinks(long parentUserId) {
			List<ChildLinkView> found = new ArrayList<>();
			for (StoredLink link : links) {
				if (link.parentUserId() != parentUserId) {
					continue;
				}
				ChildSaved subject = byId(link.subjectChildId());
				ChildSaved related = byId(link.relatedChildId());
				found.add(new ChildLinkView(
						subject.id(),
						subject.firstName(),
						subject.sex(),
						related.id(),
						related.firstName(),
						related.sex(),
						link.relation()));
			}
			return found;
		}

		@Override
		public List<ChildSaved> findByParent(long parentUserId) {
			List<ChildSaved> found = new ArrayList<>();
			for (int i = 0; i < all.size(); i++) {
				if (!hiddenFlags.get(i) && visibleTo(parentUserId, all.get(i))) {
					found.add(shown(i));
				}
			}
			return found;
		}

		@Override
		public java.util.Optional<ChildSaved> findVisible(long childId) {
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).id() == childId && !hiddenFlags.get(i)) {
					return java.util.Optional.of(shown(i));
				}
			}
			return java.util.Optional.empty();
		}

		@Override
		public boolean holds(long userId, long childId) {
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).id() == childId && !hiddenFlags.get(i) && visibleTo(userId, all.get(i))) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean isReferent(long userId, long childId) {
			for (int i = 0; i < all.size(); i++) {
				ChildSaved current = all.get(i);
				if (current.id() != childId || hiddenFlags.get(i)) {
					continue;
				}
				if (current.parentUserId() == userId) {
					return true;
				}
				for (Share share : shares) {
					if (share.childId == childId && share.userId == userId && share.referent) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean pending(long childId, String email) {
			return invitations.stream().anyMatch(item -> item.childId() == childId && item.email().equals(email));
		}

		@Override
		public StoredInvitation invite(long childId, String email) {
			StoredInvitation invitation = new StoredInvitation(nextInvitation++, childId, email);
			invitations.add(invitation);
			return invitation;
		}

		@Override
		public List<StoredInvitation> pendingForEmail(String email) {
			return invitations.stream().filter(item -> item.email().equals(email)).toList();
		}

		@Override
		public java.util.Optional<StoredInvitation> findPending(long invitationId) {
			return invitations.stream().filter(item -> item.id() == invitationId).findFirst();
		}

		@Override
		public void accept(long invitationId, long userId) {
			StoredInvitation invitation = findPending(invitationId).orElseThrow();
			invitations.remove(invitation);
			shares.add(new Share(invitation.childId(), userId, false));
		}

		@Override
		public int pendingCount(long childId) {
			int count = 0;
			for (StoredInvitation invitation : invitations) {
				if (invitation.childId() == childId) {
					count++;
				}
			}
			return count;
		}

		public void cancelPending(long childId) {
			invitations.removeIf(item -> item.childId() == childId);
		}

		private boolean visibleTo(long userId, ChildSaved child) {
			if (child.parentUserId() == userId) {
				return true;
			}
			for (Share share : shares) {
				if (share.childId == child.id() && share.userId == userId) {
					return true;
				}
			}
			return false;
		}

		private static final class Share {
			private final long childId;
			private final long userId;
			private final boolean referent;

			private Share(long childId, long userId, boolean referent) {
				this.childId = childId;
				this.userId = userId;
				this.referent = referent;
			}
		}

		private boolean mark(long parentUserId, long childId, Boolean usable, Boolean conceal) {
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).id() != childId || all.get(i).parentUserId() != parentUserId || hiddenFlags.get(i)) {
					continue;
				}
				if (usable != null) {
					activeFlags.set(i, usable);
				}
				if (conceal != null) {
					hiddenFlags.set(i, conceal);
				}
				return true;
			}
			return false;
		}

		private ChildSaved shown(int i) {
			ChildSaved current = all.get(i);
			return new ChildSaved(
					current.id(),
					current.parentUserId(),
					current.firstName(),
					current.lastName(),
					current.sex(),
					current.age(),
					current.curriculum(),
					current.grade(),
					current.schoolId(),
					current.avatarCode(),
					current.hasPhoto(),
					activeFlags.get(i));
		}

		private ChildSaved byId(long childId) {
			return all.stream().filter(child -> child.id() == childId).findFirst().orElseThrow();
		}

		@Override
		public List<InterestCopy> findInterests(long childId) {
			return List.copyOf(interests.getOrDefault(childId, List.of()));
		}

		@Override
		public void replaceInterests(long childId, List<InterestCopy> copies) {
			interests.put(childId, List.copyOf(copies));
		}

		private List<InterestCopy> copies(long childId) {
			return findInterests(childId);
		}

		@Override
		public java.util.Optional<ChildPhoto> findPhoto(long parentUserId, long childId) {
			NewChild match = null;
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).id() == childId && all.get(i).parentUserId() == parentUserId) {
					match = inserted.get(i);
				}
			}
			if (match == null || match.photo() == null) {
				return java.util.Optional.empty();
			}
			return java.util.Optional.of(new ChildPhoto(match.photo(), match.photoContentType()));
		}

		private record StoredLink(long parentUserId, long subjectChildId, long relatedChildId, String relation) {
		}
	}

	private static final class ManualClock extends Clock {
		private Instant instant;

		private ManualClock(Instant instant) {
			this.instant = instant;
		}

		private void advanceMinutes(long minutes) {
			instant = instant.plusSeconds(minutes * 60);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}

	private static final class NoChildSubjects implements ChildSubjectPort {

		@Override
		public java.util.List<CatalogSubject> visibleSubjects() {
			return java.util.List.of();
		}

		@Override
		public List<StoredLink> linksOf(long childId) {
			return java.util.List.of();
		}

		@Override
		public java.util.List<CountedLink> visibleLinksForParent(long parentUserId) {
			return java.util.List.of();
		}

		@Override
		public void ensureDefault(long childId, long subjectId) {
		}

		@Override
		public void save(long childId, long subjectId, boolean excluded) {
		}

		@Override
		public boolean deleteLink(long childId, long subjectId) {
			return false;
		}

		@Override
		public java.util.List<FollowingChild> activeInClass(long schoolId, String grade) {
			return java.util.List.of();
		}

		@Override
		public java.util.List<SubjectChildLink> linksOfSubject(long subjectId) {
			return java.util.List.of();
		}
	}
}
