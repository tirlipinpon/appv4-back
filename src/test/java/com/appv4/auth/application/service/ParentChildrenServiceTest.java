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
				"Noah", "Martin", "8", "Belgique", "3e primaire", null, "fox", jpeg, "image/jpeg", "9090", "9090");
		ChildCard card = service.add(1L, "parent", withPhoto);
		assertThat(card.avatarCode()).isNull();
		assertThat(card.hasPhoto()).isTrue();
		assertThat(service.photo(1L, "parent", card.id()).contentType()).isEqualTo("image/jpeg");

		NewChildCommand text = new NewChildCommand(
				"Noah", "Martin", "8", "Belgique", "3e primaire", null, null,
				Base64.getEncoder().encodeToString("bonjour".getBytes()), "text/plain", "9090", "9090");
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
		return new NewChildCommand(firstName, lastName, age, country, grade, schoolId, avatar, null, null, pin, confirmation);
	}

	private static final class MemoryChildren implements ChildPort {
		private final List<ChildSaved> all = new ArrayList<>();
		private final List<NewChild> inserted = new ArrayList<>();
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
		public List<ChildSaved> findByParent(long parentUserId) {
			return all.stream().filter(child -> child.parentUserId() == parentUserId).toList();
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
