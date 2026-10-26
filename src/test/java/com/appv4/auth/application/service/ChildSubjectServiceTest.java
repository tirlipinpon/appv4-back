package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildChanges;
import com.appv4.auth.application.port.out.ChildPort.ChildLinkView;
import com.appv4.auth.application.port.out.ChildPort.ChildPhoto;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildPort.NewChild;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.result.ChildSubjectsView;
import com.appv4.auth.application.result.ChildSubjectsView.SubjectOffer;
import com.appv4.auth.application.result.ParentSubjectsView;

class ChildSubjectServiceTest {

	private MemoryChildren children;
	private MemorySubjects subjects;
	private ChildSubjectService service;

	@BeforeEach
	void setUp() {
		children = new MemoryChildren();
		subjects = new MemorySubjects();
		children.add(saved(3L, 1L, "Léa", 1L, "CE1"));
		children.add(saved(4L, 1L, "Tom", null, "CE1"));
		children.add(saved(8L, 2L, "Nina", 1L, "CE1"));
		subjects.visible.add(new CatalogSubject(10L, "Français", "Prof A", 1L, "CE1"));
		subjects.visible.add(new CatalogSubject(11L, "Mathématiques", "Prof A", 1L, "CE1"));
		subjects.visible.add(new CatalogSubject(12L, "Mathématiques", "Prof A", 1L, "CE2"));
		subjects.visible.add(new CatalogSubject(13L, "Sciences", "Prof B", 1L, "CE1"));
		service = new ChildSubjectService(children, subjects, () -> List.of(
				new CatalogSchool(1L, "École française", "France", "Primaire")));
	}

	@Test
	void emptyCatalogSaysNothingIsOffered() {
		subjects.visible.clear();

		ChildSubjectsView view = one(service.loadAll(1L, "parent"), 3L);

		assertThat(view.firstName()).isEqualTo("Léa");
		assertThat(view.chosen()).isEmpty();
		assertThat(view.proposed()).isEmpty();
	}

	@Test
	void parentAddsClassSubjectsAndAnExtraCourse() {
		ChildSubjectsView offered = one(service.loadAll(1L, "parent"), 3L);
		assertThat(names(offered.chosen(), true)).containsExactly("Français", "Mathématiques", "Sciences");
		assertThat(names(offered.proposed(), false)).containsExactly("Mathématiques");

		ChildSubjectsView added = one(service.add(1L, "parent", 3L, 12L), 3L);

		assertThat(names(added.chosen(), true)).containsExactly("Français", "Mathématiques", "Sciences");
		assertThat(names(added.chosen(), false)).containsExactly("Mathématiques");
		assertThat(added.proposed()).isEmpty();
	}

	@Test
	void sameSubjectCannotBeAddedTwiceAndAnotherChildStartsEmpty() {
		assertThatThrownBy(() -> service.add(1L, "parent", 3L, 10L))
				.hasMessage(ChildSubjectService.ALREADY_LINKED);
		assertThat(one(service.loadAll(1L, "parent"), 4L).chosen()).isEmpty();
		assertThat(one(service.loadAll(1L, "parent"), 4L).proposed()).extracting(SubjectOffer::subjectId)
				.contains(10L);
	}

	@Test
	void removingTheLinkLeavesTheTeacherSubject() {
		ChildSubjectsView left = one(service.remove(1L, "parent", 3L, 11L), 3L);

		assertThat(names(left.chosen(), true)).containsExactly("Français", "Sciences");
		assertThat(names(left.proposed(), true)).containsExactly("Mathématiques");
		assertThat(subjects.visible).extracting(CatalogSubject::id).contains(11L);
	}

	@Test
	void hiddenOrDisabledSubjectDisappearsUntilItIsVisibleAgain() {
		service.loadAll(1L, "parent");
		CatalogSubject french = subjects.visible.remove(0);

		ChildSubjectsView hidden = one(service.loadAll(1L, "parent"), 3L);
		assertThat(hidden.chosen()).extracting(SubjectOffer::subjectId).doesNotContain(10L);
		assertThat(hidden.proposed()).extracting(SubjectOffer::subjectId).doesNotContain(10L);
		assertThat(subjects.linked(3L, 10L)).isTrue();

		subjects.visible.add(0, french);
		assertThat(one(service.loadAll(1L, "parent"), 3L).chosen()).extracting(SubjectOffer::subjectId).contains(10L);
	}

	@Test
	void changingTheYearRecalculatesTheLabel() {
		service.loadAll(1L, "parent");
		children.replaceGrade(3L, "CE2");

		ChildSubjectsView view = one(service.loadAll(1L, "parent"), 3L);

		assertThat(names(view.chosen(), true)).containsExactly("Mathématiques");
		assertThat(view.chosen()).filteredOn(SubjectOffer::scolaire).extracting(SubjectOffer::grade)
				.containsExactly("CE2");
		assertThat(names(view.chosen(), false)).contains("Français", "Mathématiques", "Sciences");
	}

	@Test
	void childWithoutASchoolHasOnlyExtraCourses() {
		ChildSubjectsView view = one(service.loadAll(1L, "parent"), 4L);

		assertThat(view.schoolId()).isNull();
		assertThat(view.proposed()).isNotEmpty().allMatch(offer -> !offer.scolaire());
		ChildSubjectsView added = one(service.add(1L, "parent", 4L, 10L), 4L);
		assertThat(added.chosen()).singleElement().extracting(SubjectOffer::scolaire).isEqualTo(false);
	}

	@Test
	void anotherParentAndATeacherCannotOpenTheList() {
		assertThatThrownBy(() -> service.loadAll(1L, "teacher"))
				.hasMessage(SpaceHello.DENIED);
		assertThatThrownBy(() -> service.add(1L, "parent", 3L, 99L))
				.hasMessage(ChildSubjectService.SUBJECT_UNAVAILABLE);
	}

	private ChildSubjectsView one(ParentSubjectsView view, long childId) {
		for (ChildSubjectsView child : view.children()) {
			if (child.childId() == childId) {
				return child;
			}
		}
		throw new AssertionError(childId);
	}

	private static List<String> names(List<SubjectOffer> offers, boolean scolaire) {
		return offers.stream().filter(offer -> offer.scolaire() == scolaire).map(SubjectOffer::name).toList();
	}

	private static ChildSaved saved(long id, long parentId, String firstName, Long schoolId, String grade) {
		return new ChildSaved(
				id, parentId, firstName, "Martin", "FILLE", 7, "FRANCE", grade, schoolId, "fox", false, true);
	}

	private static final class MemoryChildren implements ChildPort {

		private final List<ChildSaved> rows = new ArrayList<>();

		void add(ChildSaved child) {
			rows.add(child);
		}

		void replaceGrade(long id, String grade) {
			for (int index = 0; index < rows.size(); index++) {
				ChildSaved row = rows.get(index);
				if (row.id() == id) {
					rows.set(index, new ChildSaved(
							row.id(),
							row.parentUserId(),
							row.firstName(),
							row.lastName(),
							row.sex(),
							row.age(),
							row.curriculum(),
							grade,
							row.schoolId(),
							row.avatarCode(),
							row.hasPhoto(),
							row.active()));
				}
			}
		}

		@Override
		public Optional<ChildSaved> findOwned(long parentUserId, long childId) {
			for (ChildSaved row : rows) {
				if (row.id() == childId && row.parentUserId() == parentUserId) {
					return Optional.of(row);
				}
			}
			return Optional.empty();
		}

		@Override
		public ChildSaved insert(NewChild child) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<ChildSaved> findByParent(long parentUserId) {
			List<ChildSaved> owned = new ArrayList<>();
			for (ChildSaved row : rows) {
				if (row.parentUserId() == parentUserId) {
					owned.add(row);
				}
			}
			return owned;
		}

		@Override
		public Optional<ChildSaved> update(long parentUserId, long childId, ChildChanges changes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean deactivate(long parentUserId, long childId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean reactivate(long parentUserId, long childId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hide(long parentUserId, long childId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void insertLink(long parentUserId, long subjectChildId, long relatedChildId, String relation) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveLink(long parentUserId, long subjectChildId, long relatedChildId, String relation) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deleteLink(long parentUserId, long subjectChildId, long relatedChildId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<ChildLinkView> findLinks(long parentUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<ChildPhoto> findPhoto(long parentUserId, long childId) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MemorySubjects implements ChildSubjectPort {

		private final List<CatalogSubject> visible = new ArrayList<>();
		private final List<long[]> links = new ArrayList<>();

		@Override
		public List<CatalogSubject> visibleSubjects() {
			return List.copyOf(visible);
		}

		@Override
		public List<StoredLink> linksOf(long childId) {
			List<StoredLink> rows = new ArrayList<>();
			for (long[] link : links) {
				if (link[0] == childId) {
					rows.add(new StoredLink(link[1], link.length > 2 && link[2] == 1L));
				}
			}
			return rows;
		}

		@Override
		public List<CountedLink> visibleLinksForParent(long parentUserId) {
			return List.of();
		}

		@Override
		public void save(long childId, long subjectId, boolean excluded) {
			for (long[] link : links) {
				if (link[0] == childId && link[1] == subjectId) {
					link[2] = excluded ? 1L : 0L;
					return;
				}
			}
			links.add(new long[]{childId, subjectId, excluded ? 1L : 0L});
		}

		@Override
		public boolean deleteLink(long childId, long subjectId) {
			return links.removeIf(link -> link[0] == childId && link[1] == subjectId);
		}

		boolean linked(long childId, long subjectId) {
			for (StoredLink link : linksOf(childId)) {
				if (link.subjectId() == subjectId && !link.excluded()) {
					return true;
				}
			}
			return false;
		}
	}
}
