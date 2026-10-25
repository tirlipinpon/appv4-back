package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.port.in.ParentChildrenUseCase.NewChildCommand;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildLinkView;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildPort.NewChild;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.result.ParentChildrenView.ChildCard;

class ParentChildrenServiceTest {

	private MemoryChildren children;
	private List<CatalogSchool> catalog;
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
		service = new ParentChildrenService(children, () -> catalog, encoder);
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
				.hasMessage(SpaceHello.DENIED);
		assertThat(children.all).isEmpty();
	}

	@Test
	void photoReplacesAvatarAndATextFileIsRefused() {
		String jpeg = Base64.getEncoder().encodeToString(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3});
		NewChildCommand withPhoto = new NewChildCommand(
				"Noah",
				"Martin",
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
				null);
		ChildCard card = service.add(1L, "parent", withPhoto);
		assertThat(card.avatarCode()).isNull();
		assertThat(card.hasPhoto()).isTrue();
		assertThat(service.photo(1L, "parent", card.id()).contentType()).isEqualTo("image/jpeg");

		NewChildCommand text = new NewChildCommand(
				"Noah", "Martin", "8", "Belgique", "3e primaire", null, null,
				Base64.getEncoder().encodeToString("bonjour".getBytes()), "text/plain", "9090", "9090", null, null);
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
				firstName, lastName, age, country, grade, schoolId, avatar, null, null, pin, confirmation, null, null);
	}

	@Test
	void creatingFromAChildShowsTheSamePhraseAndLeavesTheSourceUntouched() {
		ChildCard lea = service.add(1L, "parent",
				command("Léa", "Martin", "7", "France", "CE1", null, "fox", "4821", "4821"));
		ChildSaved sourceBefore = children.all.get(0);

		ChildCard noah = service.add(1L, "parent",
				from(lea.id(), "Noah", "Martin", "8", "France", "CM1", "fox", "1357", "1357", "FRERE"));

		assertThat(noah.linkPhrases()).containsExactly("Noah est le frère de Léa");
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
		assertThat(cards.get(1).linkPhrases()).containsExactly("Noah est le frère de Léa");
		assertThat(cards.get(2).linkPhrases()).isEmpty();
		assertThat(cards.get(3).linkPhrases()).containsExactly("Emma est la cousine de Léa");
		assertThat(children.inserted.get(0).pinHash()).isEqualTo("hash:4821");
		assertThat(service.list(2L, "parent").children()).isEmpty();
	}

	@Test
	void kinshipPhrasesUseTheClosedList() {
		assertThat(ChildKinship.phrase("Nina", "SOEUR", "Léa")).isEqualTo("Nina est la sœur de Léa");
		assertThat(ChildKinship.phrase("Lino", "DEMI_FRERE", "Léa")).isEqualTo("Lino est le demi-frère de Léa");
		assertThat(ChildKinship.phrase("Noe", "DEMI_SOEUR", "Léa")).isEqualTo("Noe est la demi-sœur de Léa");
		assertThat(ChildKinship.phrase("Leo", "JUMEAU", "Léa")).isEqualTo("Leo est le jumeau de Léa");
		assertThat(ChildKinship.phrase("Lina", "JUMELLE", "Léa")).isEqualTo("Lina est la jumelle de Léa");
		assertThat(ChildKinship.phrase("Paul", "COUSIN", "Léa")).isEqualTo("Paul est le cousin de Léa");
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
				.hasMessage(SpaceHello.DENIED);

		assertThat(children.all).hasSize(1);
		assertThat(children.links).isEmpty();
		assertThat(children.inserted).extracting(NewChild::pinHash).containsExactly("hash:4821");
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
				firstName, lastName, age, country, grade, null, avatar, null, null, pin, confirmation, sourceId, link);
	}

	private static final class MemoryChildren implements ChildPort {
		private final List<ChildSaved> all = new ArrayList<>();
		private final List<NewChild> inserted = new ArrayList<>();
		private final List<StoredLink> links = new ArrayList<>();
		private NewChild last;
		private long nextId = 1;

		@Override
		public ChildSaved insert(NewChild child) {
			last = child;
			inserted.add(child);
			ChildSaved saved = new ChildSaved(
					nextId++,
					child.parentUserId(),
					child.firstName(),
					child.lastName(),
					child.age(),
					child.curriculum(),
					child.grade(),
					child.schoolId(),
					child.avatarCode(),
					child.photo() != null);
			all.add(saved);
			return saved;
		}

		@Override
		public java.util.Optional<ChildSaved> findOwned(long parentUserId, long childId) {
			return all.stream()
					.filter(child -> child.id() == childId && child.parentUserId() == parentUserId)
					.findFirst();
		}

		@Override
		public void insertLink(long parentUserId, long subjectChildId, long relatedChildId, String relation) {
			links.add(new StoredLink(parentUserId, subjectChildId, relatedChildId, relation));
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
						related.id(),
						related.firstName(),
						link.relation()));
			}
			return found;
		}

		@Override
		public List<ChildSaved> findByParent(long parentUserId) {
			return all.stream().filter(child -> child.parentUserId() == parentUserId).toList();
		}

		private ChildSaved byId(long childId) {
			return all.stream().filter(child -> child.id() == childId).findFirst().orElseThrow();
		}

		private record StoredLink(long parentUserId, long subjectChildId, long relatedChildId, String relation) {
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
	}
}
