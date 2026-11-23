package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.NewMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;
import com.appv4.auth.application.result.ChildTodayView;

class ChildDailyMissionServiceTest {

	private static final ZoneId ZONE = ReadChildProgressService.PROGRESS_ZONE;

	private final ChildLoginPort childLoginPort = Mockito.mock(ChildLoginPort.class);
	private final ChildSessionTokenPort childSessionTokenPort = Mockito.mock(ChildSessionTokenPort.class);
	private final ChildSubjectPort childSubjectPort = Mockito.mock(ChildSubjectPort.class);
	private final TeacherQuestionPort teacherQuestionPort = Mockito.mock(TeacherQuestionPort.class);
	private final MemoryMissionPort missionPort = new MemoryMissionPort();
	private final AtomicReference<Instant> now = new AtomicReference<>();
	private ChildDailyMissionService service;

	@BeforeEach
	void setUp() {
		now.set(LocalDate.of(2026, 11, 18).atTime(15, 0).atZone(ZONE).toInstant());
		Clock clock = new Clock() {
			@Override
			public ZoneId getZone() {
				return ZONE;
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return Clock.fixed(instant(), zone);
			}

			@Override
			public Instant instant() {
				return now.get();
			}
		};
		when(childSessionTokenPort.read("token")).thenReturn(Optional.of(new ChildSession(7L)));
		when(childLoginPort.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		service = new ChildDailyMissionService(
				childLoginPort,
				childSessionTokenPort,
				childSubjectPort,
				teacherQuestionPort,
				missionPort,
				clock);
	}

	@Test
	void sameDayKeepsTheSameMissionId() {
		stubMathAndWords();

		ChildTodayView first = service.today("token");
		ChildTodayView second = service.today("token");

		assertThat(first.mission()).isNotNull();
		assertThat(second.mission().id()).isEqualTo(first.mission().id());
		assertThat(first.mission().objective()).isEqualTo("Joue 2 questions aujourd'hui");
		assertThat(first.mission().target()).isEqualTo(2);
		assertThat(first.mission().rewardXp()).isEqualTo(20);
		assertThat(first.mission().status()).isEqualTo(ChildDailyMissionRules.STATUS_DOING);
		assertThat(first.mission().playSubjectId()).isEqualTo(4L);
		assertThat(first.mission().playQuestionId()).isEqualTo(11L);
		assertThat(first.mission().secondsRemaining()).isEqualTo(9 * 3600);
		assertThat(missionPort.inserts).isEqualTo(1);
	}

	@Test
	void nextDayCreatesAnotherMission() {
		stubMathAndWords();

		long firstId = service.today("token").mission().id();
		now.set(LocalDate.of(2026, 11, 19).atTime(9, 0).atZone(ZONE).toInstant());
		long secondId = service.today("token").mission().id();

		assertThat(secondId).isNotEqualTo(firstId);
		assertThat(missionPort.inserts).isEqualTo(2);
	}

	@Test
	void usefulAttemptAdvancesMatchingTaskUntilTarget() {
		stubMathAndWords();
		service.today("token");

		service.recordUsefulAttempt(7L, 4L, 100, true);
		ChildTodayView after = service.today("token");

		assertThat(after.mission().progress()).isEqualTo(1);
		assertThat(after.mission().tasks().get(0).status()).isEqualTo(ChildDailyMissionRules.STATUS_DONE);
		assertThat(after.mission().tasks().get(0).playable()).isFalse();
		assertThat(after.mission().playSubjectId()).isEqualTo(5L);
		assertThat(after.mission().status()).isEqualTo(ChildDailyMissionRules.STATUS_DOING);

		service.recordUsefulAttempt(7L, 4L, 100, true);
		assertThat(service.today("token").mission().progress()).isEqualTo(1);
	}

	@Test
	void zeroScoreDoesNotAdvance() {
		stubMathAndWords();
		service.today("token");

		service.recordUsefulAttempt(7L, 4L, 0, false);

		assertThat(service.today("token").mission().progress()).isZero();
	}

	@Test
	void noPlayableSubjectReturnsEmptyMission() {
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of());

		ChildTodayView view = service.today("token");

		assertThat(view.mission()).isNull();
		assertThat(missionPort.inserts).isZero();
	}

	@Test
	void finishedMissionHasNoPlayLink() {
		stubMathAndWords();
		service.today("token");
		service.recordUsefulAttempt(7L, 4L, 100, true);
		service.recordUsefulAttempt(7L, 5L, 40, false);

		ChildTodayView view = service.today("token");

		assertThat(view.mission().status()).isEqualTo(ChildDailyMissionRules.STATUS_MISSION_DONE);
		assertThat(view.mission().statusLabel()).isEqualTo("Terminée");
		assertThat(view.mission().playable()).isFalse();
		assertThat(view.mission().playSubjectId()).isNull();
		assertThat(view.mission().playQuestionId()).isNull();
	}

	private void stubMathAndWords() {
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false), new StoredLink(5L, false)));
		when(childSubjectPort.visibleByIds(any())).thenReturn(List.of(
				new CatalogSubject(4L, "Maths", "Mme X", 1L, "CE1", "math"),
				new CatalogSubject(5L, "Mots", "M. Y", 1L, "CE1", "words")));
		when(teacherQuestionPort.firstQuestionIdByForm(4L, PlayableQuestionForm.codes())).thenReturn(Optional.of(11L));
		when(teacherQuestionPort.firstQuestionIdByForm(5L, PlayableQuestionForm.codes())).thenReturn(Optional.of(21L));
	}

	private static final class MemoryMissionPort implements ChildDailyMissionPort {

		private final List<StoredMission> rows = new ArrayList<>();
		private final AtomicLong ids = new AtomicLong(1);
		private int inserts;

		@Override
		public Optional<StoredMission> findByChildAndDate(long childId, LocalDate missionDate) {
			return rows.stream()
					.filter(row -> row.childId() == childId && row.missionDate().equals(missionDate))
					.findFirst();
		}

		@Override
		public StoredMission insert(NewMission mission) {
			Optional<StoredMission> existing = findByChildAndDate(mission.childId(), mission.missionDate());
			if (existing.isPresent()) {
				return existing.get();
			}
			inserts++;
			List<StoredTask> tasks = new ArrayList<>();
			for (var task : mission.tasks()) {
				tasks.add(new StoredTask(
						ids.getAndIncrement(),
						task.position(),
						task.label(),
						task.targetCount(),
						0,
						task.rewardXp(),
						task.subjectId(),
						task.questionId()));
			}
			StoredMission stored = new StoredMission(
					ids.getAndIncrement(), mission.childId(), mission.missionDate(), mission.objective(), tasks);
			rows.add(stored);
			return stored;
		}

		@Override
		public void setProgress(long taskId, int progressCount) {
			for (int i = 0; i < rows.size(); i++) {
				StoredMission mission = rows.get(i);
				List<StoredTask> next = new ArrayList<>();
				boolean changed = false;
				for (StoredTask task : mission.tasks()) {
					if (task.id() == taskId) {
						next.add(new StoredTask(
								task.id(),
								task.position(),
								task.label(),
								task.targetCount(),
								progressCount,
								task.rewardXp(),
								task.subjectId(),
								task.questionId()));
						changed = true;
					} else {
						next.add(task);
					}
				}
				if (changed) {
					rows.set(i, new StoredMission(
							mission.id(), mission.childId(), mission.missionDate(), mission.objective(), next));
				}
			}
		}
	}
}
