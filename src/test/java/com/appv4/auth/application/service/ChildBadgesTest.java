package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AttemptTrace;
import com.appv4.auth.application.port.out.ChildBadgePort;
import com.appv4.auth.application.port.out.ChildBadgePort.StoredBadge;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.result.ChildBadgesView;
import com.appv4.auth.application.result.ChildBadgesView.Badge;

class ChildBadgesTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

	private final ChildSessionTokenPort session = mock(ChildSessionTokenPort.class);
	private final ChildLoginPort children = mock(ChildLoginPort.class);
	private final ChildQuestionAttemptPort attempts = mock(ChildQuestionAttemptPort.class);
	private final ChildSubjectPort subjects = mock(ChildSubjectPort.class);
	private final TeacherQuestionPort questions = mock(TeacherQuestionPort.class);
	private final MemoryBadges stored = new MemoryBadges();
	private final MemoryMissions missionPort = new MemoryMissions();
	private final List<AttemptTrace> history = new ArrayList<>();
	private final Map<Long, StoredMark> marks = new HashMap<>();
	private ReadChildBadgesService badges;

	@BeforeEach
	void child() {
		when(session.read("lea")).thenReturn(Optional.of(new ChildSession(7L)));
		when(session.read("autre")).thenReturn(Optional.of(new ChildSession(8L)));
		when(children.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Akira", "hash")));
		when(children.findActiveById(8L)).thenReturn(Optional.of(new ChildLoginCandidate(8L, "Nao", "hash")));
		when(attempts.allMarks(7L)).thenAnswer(invocation -> Map.copyOf(marks));
		when(attempts.allMarks(8L)).thenReturn(Map.of());
		when(attempts.traces(7L)).thenAnswer(invocation -> List.copyOf(history));
		when(attempts.traces(8L)).thenReturn(List.of());
		badges = new ReadChildBadgesService(
				children, session, attempts, stored, subjects, questions, missionPort,
				Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void sansEtoileLaCollectionMontreNeufBadgesAZero() {
		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges()).extracting(Badge::name).containsExactly(
				"Première étoile jaune",
				"Matière au jaune",
				"Étoiles jaunes",
				"Rattrapage",
				"Bonnes réponses d'affilée",
				"Bonnes réponses du jour",
				"Jours d'affilée",
				"Jours joués",
				"Missions terminées");
		Badge stars = view.badges().get(2);
		assertThat(stars.level()).isZero();
		assertThat(stars.retainedValue()).isZero();
		assertThat(stars.nextThreshold()).isEqualTo(5);
		assertThat(stars.unlockedOn()).isNull();
		assertThat(view.badges().get(3).nextThreshold()).isEqualTo(1);
		assertThat(view.badges().get(7).nextThreshold()).isEqualTo(3);
		assertThat(view.badges().get(8).nextThreshold()).isEqualTo(1);
		assertThat(view.gainedToday()).isEmpty();
		assertThat(view.notice()).isNull();
	}

	@Test
	void quatreEtoilesRestentAuNiveauZeroSansDate() {
		yellow(1, 4);

		Badge stars = badges.badges("lea").badges().get(2);

		assertThat(stars.level()).isZero();
		assertThat(stars.retainedValue()).isEqualTo(4);
		assertThat(stars.nextThreshold()).isEqualTo(5);
		assertThat(stars.unlockedOn()).isNull();
		assertThat(badges.badges("lea").gainedToday()).isEmpty();
	}

	@Test
	void uneSommeAncienneDeCinqNeDatentPasAujourdhui() {
		yellow(1, 5);

		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges().get(0).level()).isEqualTo(1);
		assertThat(view.badges().get(0).unlockedOn()).isNull();
		assertThat(view.badges().get(2).level()).isEqualTo(1);
		assertThat(view.badges().get(2).retainedValue()).isEqualTo(5);
		assertThat(view.badges().get(2).nextThreshold()).isEqualTo(7);
		assertThat(view.badges().get(2).unlockedOn()).isNull();
		assertThat(view.gainedToday()).isEmpty();
	}

	@Test
	void laCinquiemeEtoileDuJourEntreDansLeBloc() {
		yellow(1, 4);
		badges.badges("lea");
		marks.put(2L, new StoredMark(QuestionMark.YELLOW, 1));

		badges.afterAttempt(7L, 4L, 2L, QuestionMark.NONE, QuestionMark.YELLOW);
		ChildBadgesView view = badges.badges("lea");

		Badge stars = view.badges().get(2);
		assertThat(stars.level()).isEqualTo(1);
		assertThat(stars.retainedValue()).isEqualTo(5);
		assertThat(stars.unlockedOn()).isEqualTo(TODAY);
		assertThat(view.gainedToday()).extracting(ChildBadgesView.Gained::name).containsExactly("Étoiles jaunes");
		assertThat(view.badges().get(0).unlockedOn()).isNull();
	}

	@Test
	void uneMatiereTouteJauneMonteAuPremierPassage() {
		openMath(1L);
		yellow(1L, 1);

		badges.afterAttempt(7L, 4L, 1L, QuestionMark.NONE, QuestionMark.YELLOW);
		ChildBadgesView view = badges.badges("lea");

		Badge subject = view.badges().get(1);
		assertThat(subject.level()).isEqualTo(1);
		assertThat(subject.unlockedOn()).isEqualTo(TODAY);
		assertThat(view.notice()).isNull();
		assertThat(view.gainedToday()).extracting(ChildBadgesView.Gained::name).contains("Matière au jaune");
	}

	@Test
	void uneRepriseJauneNeFaitPasMonterLaMatiere() {
		openMath(1L);
		yellow(1L, 1);
		badges.afterAttempt(7L, 4L, 1L, QuestionMark.NONE, QuestionMark.YELLOW);

		yellow(1L, 2);
		badges.afterAttempt(7L, 4L, 1L, QuestionMark.YELLOW, QuestionMark.YELLOW);

		assertThat(badges.badges("lea").badges().get(1).level()).isEqualTo(1);
	}

	@Test
	void desQuestionsAjouteesLaissentLeNiveauEtAvertissent() {
		openMath(1L);
		yellow(1L, 1);
		badges.afterAttempt(7L, 4L, 1L, QuestionMark.NONE, QuestionMark.YELLOW);
		when(questions.questionIds(4L)).thenReturn(List.of(1L, 2L));

		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges().get(1).level()).isEqualTo(1);
		assertThat(view.notice().subjectName()).isEqualTo("Mathématiques");
		assertThat(view.notice().remainingCount()).isEqualTo(1);
		assertThat(view.notice().label()).isEqualTo("Mathématiques : 1 question n'est pas encore jaune.");
	}

	@Test
	void rejaunirLesQuestionsAjouteesMonteDunNiveau() {
		openMath(1L);
		yellow(1L, 1);
		badges.afterAttempt(7L, 4L, 1L, QuestionMark.NONE, QuestionMark.YELLOW);
		when(questions.questionIds(4L)).thenReturn(List.of(1L, 2L));
		yellow(2L, 1);

		badges.afterAttempt(7L, 4L, 2L, QuestionMark.BLACK, QuestionMark.YELLOW);
		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges().get(1).level()).isEqualTo(2);
		assertThat(view.notice()).isNull();
	}

	@Test
	void retirerLesQuestionsAjouteesNAugmentePasLeNiveau() {
		openMath(1L, 2L);
		yellow(1L, 1);
		yellow(2L, 1);
		badges.afterAttempt(7L, 4L, 2L, QuestionMark.NONE, QuestionMark.YELLOW);
		when(questions.questionIds(4L)).thenReturn(List.of(1L));

		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges().get(1).level()).isEqualTo(1);
		assertThat(view.notice()).isNull();
	}

	@Test
	void uneMatiereFermeeGardeLeNiveauSansAvertissementNiTransfert() {
		openMath(1L);
		yellow(1L, 1);
		badges.afterAttempt(7L, 4L, 1L, QuestionMark.NONE, QuestionMark.YELLOW);
		when(subjects.linksOf(7L)).thenReturn(List.of());
		openWords(3L);
		yellow(3L, 1);
		badges.afterAttempt(7L, 5L, 3L, QuestionMark.NONE, QuestionMark.YELLOW);

		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges().get(1).level()).isEqualTo(1);
		assertThat(view.notice()).isNull();
	}

	@Test
	void laRepriseNeDonnePasLaMatiereMemeSiElleEstTouteJaune() {
		openMath(1L);
		yellow(1L, 1);

		assertThat(badges.badges("lea").badges().get(1).level()).isZero();
	}

	@Test
	void unAutreEnfantNeVoitPasCesEtoiles() {
		yellow(1, 5);
		badges.badges("lea");

		Badge stars = badges.badges("autre").badges().get(2);

		assertThat(stars.level()).isZero();
		assertThat(stars.retainedValue()).isZero();
	}

	@Test
	void sansJetonLaLectureEstRefusee() {
		assertThatThrownBy(() -> badges.badges(""))
				.isInstanceOf(AuthDomainException.class);
	}

	private void openMath(long... questionIds) {
		when(subjects.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(subjects.visibleByIds(List.of(4L))).thenReturn(List.of(math()));
		when(questions.questionIds(4L)).thenReturn(ids(questionIds));
	}

	private void openWords(long... questionIds) {
		when(subjects.visibleByIds(List.of(5L))).thenReturn(List.of(
				new CatalogSubject(5L, "Mots", "M. Y", 1L, "CE1", "words")));
		when(questions.questionIds(5L)).thenReturn(ids(questionIds));
	}

	private static List<Long> ids(long... questionIds) {
		return java.util.Arrays.stream(questionIds).boxed().toList();
	}

	private static CatalogSubject math() {
		return new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math");
	}

	@Test
	void unRattrapageCompteUneFoisEtGardeSaDate() {
		history.add(trace(1L, 18, QuestionMark.BLACK, false, false));
		history.add(trace(1L, 19, QuestionMark.YELLOW, true, true));
		history.add(trace(1L, 20, QuestionMark.YELLOW, true, true));

		Badge badge = badges.badges("lea").badges().get(3);

		assertThat(badge.level()).isEqualTo(1);
		assertThat(badge.retainedValue()).isEqualTo(1);
		assertThat(badge.unlockedOn()).isEqualTo(LocalDate.of(2026, 11, 19));
		assertThat(badges.badges("lea").gainedToday()).extracting(ChildBadgesView.Gained::code)
				.doesNotContain(ReadChildBadgesService.RATTRAPAGE);
	}

	@Test
	void deuxJoursDEchecsAvancentLesJoursJouesEtPasLaSerie() {
		history.add(trace(1L, 18, null, false, false));
		history.add(trace(1L, 19, null, false, false));

		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges().get(7).retainedValue()).isEqualTo(2);
		assertThat(view.badges().get(7).level()).isZero();
		assertThat(view.badges().get(7).nextThreshold()).isEqualTo(3);
		assertThat(view.badges().get(6).level()).isZero();
		assertThat(view.badges().get(6).retainedValue()).isZero();
	}

	@Test
	void deuxJoursPayesDeSuiteDonnentLaSerieSansEntrerDansAujourdhui() {
		history.add(trace(1L, 18, QuestionMark.YELLOW, true, true));
		history.add(trace(1L, 19, QuestionMark.YELLOW, true, true));

		Badge streak = badges.badges("lea").badges().get(6);

		assertThat(streak.level()).isEqualTo(1);
		assertThat(streak.unlockedOn()).isEqualTo(LocalDate.of(2026, 11, 19));
		assertThat(badges.badges("lea").gainedToday()).isEmpty();
	}

	@Test
	void troisReponsesPayeesPuisUnEchecGardentLeNiveau() {
		history.add(trace(1L, 18, QuestionMark.YELLOW, true, true));
		history.add(trace(2L, 18, QuestionMark.YELLOW, true, true));
		history.add(trace(3L, 18, QuestionMark.YELLOW, true, true));
		badges.badges("lea");
		history.add(trace(3L, 18, QuestionMark.YELLOW, false, false));

		ChildBadgesView view = badges.badges("lea");

		assertThat(view.badges().get(4).level()).isEqualTo(1);
		assertThat(view.badges().get(4).retainedValue()).isEqualTo(3);
		assertThat(view.badges().get(5).level()).isEqualTo(1);
		assertThat(view.badges().get(5).retainedValue()).isEqualTo(3);
	}

	@Test
	void unRejeuJauneNonPayeNeCassePasLEnchainement() {
		history.add(trace(1L, 18, QuestionMark.YELLOW, true, true));
		history.add(trace(1L, 18, QuestionMark.YELLOW, true, false));
		history.add(trace(2L, 18, QuestionMark.YELLOW, true, true));
		history.add(trace(3L, 18, QuestionMark.YELLOW, true, true));

		assertThat(badges.badges("lea").badges().get(4).level()).isEqualTo(1);
		assertThat(badges.badges("lea").badges().get(4).retainedValue()).isEqualTo(3);
	}

	@Test
	void unEssaiSansPayNeNourritPasLesBonnesReponses() {
		history.add(trace(1L, 18, QuestionMark.YELLOW, true, null));
		history.add(trace(2L, 18, QuestionMark.YELLOW, true, null));
		history.add(trace(3L, 18, QuestionMark.YELLOW, true, null));

		assertThat(badges.badges("lea").badges().get(4).level()).isZero();
		assertThat(badges.badges("lea").badges().get(6).level()).isZero();
	}

	@Test
	void uneMissionFinieResteApresLaDisparitionDeSesTaches() {
		missionPort.missions.add(doneMission(LocalDate.of(2026, 11, 18)));

		assertThat(badges.badges("lea").badges().get(8).level()).isEqualTo(1);
		assertThat(badges.badges("lea").badges().get(8).unlockedOn()).isEqualTo(LocalDate.of(2026, 11, 18));

		missionPort.missions.clear();

		assertThat(badges.badges("lea").badges().get(8).level()).isEqualTo(1);
	}

	private static AttemptTrace trace(
			long questionId, int day, QuestionMark mark, boolean passed, Boolean pay) {
		Instant at = LocalDate.of(2026, 11, day).atStartOfDay(ReadChildBadgesService.ZONE).toInstant();
		return new AttemptTrace(questionId, at, mark, passed, pay);
	}

	private static StoredMission doneMission(LocalDate date) {
		return new StoredMission(1L, 7L, date, "Joue 1 question aujourd'hui", List.of(
				new StoredTask(1L, 1, "Question", 1, 1, 10, 4L, 1L)));
	}

	private void yellow(long questionId, int count) {
		marks.put(questionId, new StoredMark(QuestionMark.YELLOW, count));
	}

	private static final class MemoryBadges implements ChildBadgePort {
		private final Map<Long, Map<String, StoredBadge>> rows = new HashMap<>();
		private final Map<Long, Boolean> ready = new HashMap<>();

		@Override
		public boolean initialized(long childId) {
			return ready.getOrDefault(childId, false);
		}

		@Override
		public void markInitialized(long childId) {
			ready.put(childId, true);
		}

		@Override
		public Map<String, StoredBadge> load(long childId) {
			return Map.copyOf(rows.getOrDefault(childId, Map.of()));
		}

		@Override
		public void save(long childId, StoredBadge badge) {
			rows.computeIfAbsent(childId, ignored -> new HashMap<>()).put(badge.code(), badge);
		}
	}

	private static final class MemoryMissions implements ChildDailyMissionPort {
		private final List<StoredMission> missions = new ArrayList<>();
		private final List<LocalDate> days = new ArrayList<>();

		@Override
		public Optional<StoredMission> findByChildAndDate(long childId, LocalDate missionDate) {
			return missions.stream().filter(mission -> mission.missionDate().equals(missionDate)).findFirst();
		}

		@Override
		public StoredMission insert(NewMission mission) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setProgress(long taskId, int progressCount) {
		}

		@Override
		public List<StoredMission> missionsOf(long childId) {
			return List.copyOf(missions);
		}

		@Override
		public void rememberFinishedDay(long childId, LocalDate missionDate) {
			if (!days.contains(missionDate)) {
				days.add(missionDate);
			}
		}

		@Override
		public List<LocalDate> finishedDays(long childId) {
			return List.copyOf(days);
		}
	}
}
