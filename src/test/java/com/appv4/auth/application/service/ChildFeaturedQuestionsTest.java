package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildBadgePort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.NewAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageClaim;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.CountedLink;
import com.appv4.auth.application.port.out.ChildSubjectPort.FollowingChild;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.ChildSubjectPort.SubjectChildLink;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildFeaturedQuestionsView;
import com.appv4.auth.application.result.ChildFeaturedQuestionsView.FeaturedQuestion;

class ChildFeaturedQuestionsTest {

	private final ChildSessionTokenPort session = mock(ChildSessionTokenPort.class);
	private final ChildLoginPort children = mock(ChildLoginPort.class);
	private final MemorySubjects subjects = new MemorySubjects();
	private final MemoryQuestions questions = new MemoryQuestions();
	private final MemoryAttempts attempts = new MemoryAttempts();
	private ReadChildFeaturedQuestionsService featured;

	@BeforeEach
	void leaConnectee() {
		when(session.read("lea")).thenReturn(Optional.of(new ChildSession(7L)));
		when(children.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		featured = new ReadChildFeaturedQuestionsService(children, session, subjects, questions, attempts);
	}

	@Test
	void troisQuestionsEligiblesReviennentDansLOrdreEtLaQuatriemeResteDehors() {
		subjects.show(new CatalogSubject(5L, "Mots", "M. Y", 1L, "CE1", "words"));
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.link(7L, 5L, false);
		subjects.link(7L, 4L, false);
		questions.add(5L, new QuestionSummary(22L, "qcm", "Quel est le contraire de haut ?"));
		questions.add(4L, new QuestionSummary(12L, "qcm", "Combien font 3 + 1 ?"));
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		questions.add(5L, new QuestionSummary(21L, "qcm", "Quel est le contraire de grand ?"));

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(
						tuple(4L, 11L, "Combien font 2 + 2 ?"),
						tuple(4L, 12L, "Combien font 3 + 1 ?"),
						tuple(5L, 21L, "Quel est le contraire de grand ?"));
		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectName)
				.containsExactly("Mathématiques", "Mathématiques", "Mots");
	}

	@Test
	void uneEtoileJauneLaisseEntrerLaQuestionSuivante() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.show(new CatalogSubject(5L, "Mots", "M. Y", 1L, "CE1", "words"));
		subjects.link(7L, 4L, false);
		subjects.link(7L, 5L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		questions.add(4L, new QuestionSummary(12L, "qcm", "Combien font 3 + 1 ?"));
		questions.add(5L, new QuestionSummary(21L, "qcm", "Quel est le contraire de grand ?"));
		questions.add(5L, new QuestionSummary(22L, "qcm", "Quel est le contraire de haut ?"));
		attempts.keep(7L, 11L, QuestionMark.YELLOW, 1);

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(
						tuple(4L, 12L, "Combien font 3 + 1 ?"),
						tuple(5L, 21L, "Quel est le contraire de grand ?"),
						tuple(5L, 22L, "Quel est le contraire de haut ?"));
	}

	@Test
	void uneEtoileNoireEtUneMarqueAbsenteRestent() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.link(7L, 4L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		questions.add(4L, new QuestionSummary(12L, "qcm", "Combien font 3 + 1 ?"));
		attempts.keep(7L, 11L, QuestionMark.BLACK, 0);

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(
						tuple(4L, 11L, "Combien font 2 + 2 ?"),
						tuple(4L, 12L, "Combien font 3 + 1 ?"));
	}

	@Test
	void unLienRetireUneMatiereInvisibleEtUneFormeNonJouableRestentDehors() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.show(new CatalogSubject(5L, "Mots", "M. Y", 1L, "CE1", "words"));
		subjects.hide(new CatalogSubject(6L, "Cachée", "Mme X", 1L, "CE1", "math"));
		subjects.deactivate(new CatalogSubject(7L, "Arrêtée", "Mme X", 1L, "CE1", "math"));
		subjects.show(new CatalogSubject(8L, "Sciences", "Mme X", 1L, "CE1", "math"));
		subjects.show(new CatalogSubject(9L, "Histoire", "M. Y", 1L, "CE1", "words"));
		subjects.link(7L, 4L, false);
		subjects.link(7L, 5L, true);
		subjects.link(7L, 6L, false);
		subjects.link(7L, 7L, false);
		subjects.link(7L, 8L, false);
		subjects.link(7L, 9L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		questions.add(5L, new QuestionSummary(21L, "qcm", "Matière retirée"));
		questions.add(6L, new QuestionSummary(31L, "qcm", "Matière cachée"));
		questions.add(7L, new QuestionSummary(41L, "qcm", "Matière arrêtée"));
		questions.add(8L, new QuestionSummary(51L, "dictee", "Forme fermée"));
		questions.add(9L, new QuestionSummary(61L, "qcm", "Quel est le contraire de grand ?"));

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(
						tuple(4L, 11L, "Combien font 2 + 2 ?"),
						tuple(9L, 61L, "Quel est le contraire de grand ?"));
	}

	@Test
	void uneQuestionSansLeconEtSansPointsResteSiElleNestPasJaune() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.link(7L, 4L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Question sans leçon"));
		questions.add(4L, new QuestionSummary(12L, "qcm", "Question sans points"));

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(
						tuple(4L, 11L, "Question sans leçon"),
						tuple(4L, 12L, "Question sans points"));
	}

	@Test
	void laMemeReserveRelueDeuxFoisRendLaMemeListe() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.show(new CatalogSubject(5L, "Mots", "M. Y", 1L, "CE1", "words"));
		subjects.link(7L, 4L, false);
		subjects.link(7L, 5L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		questions.add(4L, new QuestionSummary(12L, "qcm", "Combien font 3 + 1 ?"));
		questions.add(5L, new QuestionSummary(21L, "qcm", "Quel est le contraire de grand ?"));
		attempts.keep(7L, 12L, QuestionMark.YELLOW, 2);

		ChildFeaturedQuestionsView first = featured.read("lea");
		ChildFeaturedQuestionsView second = featured.read("lea");

		assertThat(first.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(
						tuple(4L, 11L, "Combien font 2 + 2 ?"),
						tuple(5L, 21L, "Quel est le contraire de grand ?"));
		assertThat(second.questions()).containsExactlyElementsOf(first.questions());
	}

	@Test
	void laLectureNeCreeNiMissionNiEssaiNiMarqueNiBadge() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.link(7L, 4L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		attempts.keep(7L, 11L, QuestionMark.BLACK, 0);

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(tuple(4L, 11L, "Combien font 2 + 2 ?"));
		assertThat(attempts.inserted()).isEmpty();
		assertThat(attempts.markWrites()).isZero();
		assertThat(attempts.findMark(7L, 11L)).contains(new StoredMark(QuestionMark.BLACK, 0));
		assertThat(ReadChildFeaturedQuestionsService.class.getDeclaredConstructors()[0].getParameterTypes())
				.doesNotContain(ChildDailyMissionPort.class, ChildBadgePort.class);
	}

	@Test
	void unJetonAbsentOuAdulteEstRefuseCommeLaListeDesMatieres() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.link(7L, 4L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		attempts.keep(7L, 11L, QuestionMark.BLACK, 0);
		when(session.read("adulte")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> featured.read(null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildLearningSubjectsService.INVALID_SESSION_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.UNAUTHORIZED);
		assertThatThrownBy(() -> featured.read("adulte"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildLearningSubjectsService.INVALID_SESSION_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.UNAUTHORIZED);
		assertThat(attempts.inserted()).isEmpty();
		assertThat(attempts.markWrites()).isZero();
		assertThat(attempts.findMark(7L, 11L)).contains(new StoredMark(QuestionMark.BLACK, 0));
	}

	@Test
	void uneFicheArreteeEstRefuseeCommeLaListeDesMatieres() {
		when(children.findActiveById(7L)).thenReturn(Optional.empty());
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.link(7L, 4L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));

		assertThatThrownBy(() -> featured.read("lea"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildLearningSubjectsService.INVALID_SESSION_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.UNAUTHORIZED);
		assertThat(attempts.inserted()).isEmpty();
		assertThat(attempts.markWrites()).isZero();
	}

	@Test
	void leJetonDUnAutreEnfantNeRendQueSesQuestions() {
		when(session.read("nao")).thenReturn(Optional.of(new ChildSession(8L)));
		when(children.findActiveById(8L)).thenReturn(Optional.of(new ChildLoginCandidate(8L, "Nao", "hash")));
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.show(new CatalogSubject(5L, "Mots", "M. Y", 1L, "CE1", "words"));
		subjects.link(7L, 4L, false);
		subjects.link(8L, 5L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		questions.add(5L, new QuestionSummary(21L, "qcm", "Quel est le contraire de grand ?"));

		ChildFeaturedQuestionsView view = featured.read("nao");

		assertThat(view.questions())
				.extracting(FeaturedQuestion::subjectId, FeaturedQuestion::questionId, FeaturedQuestion::prompt)
				.containsExactly(tuple(5L, 21L, "Quel est le contraire de grand ?"));
	}

	@Test
	void sansMatiereLieeLaLectureReussitAvecUneListeVide() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions()).isEmpty();
	}

	@Test
	void toutesLesQuestionsJaunesLaLectureReussitAvecUneListeVide() {
		subjects.show(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math"));
		subjects.link(7L, 4L, false);
		questions.add(4L, new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"));
		questions.add(4L, new QuestionSummary(12L, "qcm", "Combien font 3 + 1 ?"));
		attempts.keep(7L, 11L, QuestionMark.YELLOW, 1);
		attempts.keep(7L, 12L, QuestionMark.YELLOW, 4);

		ChildFeaturedQuestionsView view = featured.read("lea");

		assertThat(view.questions()).isEmpty();
	}

	private static final class MemorySubjects implements ChildSubjectPort {

		private final List<CatalogSubject> catalog = new ArrayList<>();
		private final Set<Long> hidden = new HashSet<>();
		private final Set<Long> inactive = new HashSet<>();
		private final List<Link> links = new ArrayList<>();

		void show(CatalogSubject subject) {
			catalog.add(subject);
		}

		void hide(CatalogSubject subject) {
			catalog.add(subject);
			hidden.add(subject.id());
		}

		void deactivate(CatalogSubject subject) {
			catalog.add(subject);
			inactive.add(subject.id());
		}

		void link(long childId, long subjectId, boolean excluded) {
			links.add(new Link(childId, subjectId, excluded));
		}

		@Override
		public List<CatalogSubject> visibleSubjects() {
			List<CatalogSubject> rows = new ArrayList<>();
			for (CatalogSubject subject : catalog) {
				if (!hidden.contains(subject.id()) && !inactive.contains(subject.id())) {
					rows.add(subject);
				}
			}
			return List.copyOf(rows);
		}

		@Override
		public List<StoredLink> linksOf(long childId) {
			List<StoredLink> rows = new ArrayList<>();
			for (Link link : links) {
				if (link.childId == childId) {
					rows.add(new StoredLink(link.subjectId, link.excluded));
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
			throw new UnsupportedOperationException();
		}

		@Override
		public void ensureDefault(long childId, long subjectId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean deleteLink(long childId, long subjectId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<FollowingChild> activeInClass(long schoolId, String grade) {
			return List.of();
		}

		@Override
		public List<SubjectChildLink> linksOfSubject(long subjectId) {
			return List.of();
		}

		private record Link(long childId, long subjectId, boolean excluded) {
		}
	}

	private static final class MemoryQuestions implements TeacherQuestionPort {

		private final Map<Long, List<QuestionSummary>> bySubject = new HashMap<>();

		void add(long subjectId, QuestionSummary summary) {
			bySubject.computeIfAbsent(subjectId, id -> new ArrayList<>()).add(summary);
		}

		@Override
		public List<StoredQuestion> findBySubject(long subjectId, long afterId, int limit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<QuestionSummary> findSummariesBySubject(long subjectId, long afterId, int limit) {
			return bySubject.getOrDefault(subjectId, List.of()).stream()
					.filter(question -> question.id() > afterId)
					.sorted(Comparator.comparingLong(QuestionSummary::id))
					.limit(limit)
					.toList();
		}

		@Override
		public Optional<Long> firstQuestionIdByForm(long subjectId, java.util.Set<String> formCodes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<Long, Long> countBySubjectIds(List<Long> subjectIds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public StoredQuestion insert(long subjectId, String formCode, String prompt, DraftContent content) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<StoredQuestion> findInSubject(long subjectId, long questionId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public StoredQuestion replace(long questionId, String prompt, DraftContent content) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void ensureForm(long subjectId, String formCode, String label, int sortOrder) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void delete(long questionId) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MemoryAttempts implements ChildQuestionAttemptPort {

		private final Map<String, StoredMark> committed = new HashMap<>();
		private final List<NewAttempt> inserted = new ArrayList<>();
		private int markWriteCount;

		List<NewAttempt> inserted() {
			return List.copyOf(inserted);
		}

		int markWrites() {
			return markWriteCount;
		}

		void keep(long childId, long questionId, QuestionMark code, int yellowCount) {
			committed.put(key(childId, questionId), new StoredMark(code, yellowCount));
		}

		@Override
		public StoredAttempt insert(NewAttempt attempt) {
			inserted.add(attempt);
			return new StoredAttempt(
					inserted.size(), attempt.childId(), attempt.subjectId(), attempt.questionId(),
					attempt.formCode(), attempt.scorePercent(), attempt.passed());
		}

		@Override
		public Optional<StoredAttempt> lockAndFind(long childId, UUID submissionId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<PageClaim> claim(
				UUID playId, long childId, long subjectId, long questionId, int maxAttempts) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int attemptsUsed(UUID playId, long childId, long subjectId, long questionId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<StoredMark> findMark(long childId, long questionId) {
			return Optional.ofNullable(committed.get(key(childId, questionId)));
		}

		@Override
		public Optional<StoredMark> lockMark(long childId, long questionId) {
			return findMark(childId, questionId);
		}

		@Override
		public void saveMark(long childId, long questionId, QuestionMark code, int yellowCount) {
			markWriteCount++;
			committed.put(key(childId, questionId), new StoredMark(code, yellowCount));
		}

		@Override
		public Map<Long, StoredMark> marksFor(long childId, java.util.Collection<Long> questionIds) {
			Map<Long, StoredMark> found = new HashMap<>();
			if (questionIds == null) {
				return found;
			}
			for (Long questionId : questionIds) {
				StoredMark mark = committed.get(key(childId, questionId));
				if (mark != null) {
					found.put(questionId, mark);
				}
			}
			return found;
		}

		@Override
		public Map<Long, StoredMark> allMarks(long childId) {
			Map<Long, StoredMark> found = new HashMap<>();
			String prefix = childId + ":";
			committed.forEach((key, mark) -> {
				if (key.startsWith(prefix)) {
					found.put(Long.parseLong(key.substring(prefix.length())), mark);
				}
			});
			return found;
		}

		private static String key(long childId, long questionId) {
			return childId + ":" + questionId;
		}
	}
}
