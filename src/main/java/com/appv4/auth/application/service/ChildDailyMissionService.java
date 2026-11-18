package com.appv4.auth.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildTodayUseCase;
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.NewMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.NewTask;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildTodayView;
import com.appv4.auth.application.service.question.play.MemoryChildAnswerGrader;
import com.appv4.auth.application.service.question.play.OrderChildAnswerGrader;
import com.appv4.auth.application.service.question.play.QcmChildAnswerGrader;
import com.appv4.auth.application.service.question.play.TrueFalseChildAnswerGrader;

/**
 * Mission du jour d'un enfant (BACK-18).
 *
 * <p><b>Business :</b> le plan vient des matières liées et ne change pas dans la journée.
 * Une tentative utile fait avancer la tâche de cette matière.
 * <p><b>Technique :</b> crée la ligne du jour au premier besoin, puis relit le même id.
 * <p><b>Métier / flow :</b> GET {@code /api/ext/today} et submit d'une tentative → ce service.
 */
public class ChildDailyMissionService implements ReadChildTodayUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildSubjectPort childSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;
	private final ChildDailyMissionPort childDailyMissionPort;
	private final Clock clock;

	public ChildDailyMissionService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildDailyMissionPort childDailyMissionPort,
			Clock clock) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.childDailyMissionPort = childDailyMissionPort;
		this.clock = clock;
	}

	@Override
	@Transactional
	public ChildTodayView today(String rawToken) {
		long childId = requireActiveChild(rawToken);
		return viewOf(childId);
	}

	/**
	 * Avance la tâche du jour dont la matière est celle de la tentative, si le score compte.
	 */
	@Transactional
	public void recordUsefulAttempt(long childId, long subjectId, int scorePercent, boolean passed) {
		if (!ChildDailyMissionRules.useful(scorePercent, passed)) {
			return;
		}
		Optional<StoredMission> mission = ensure(childId, today());
		if (mission.isEmpty()) {
			return;
		}
		for (StoredTask task : mission.get().tasks()) {
			if (task.subjectId() != subjectId) {
				continue;
			}
			int next = ChildDailyMissionRules.nextProgress(task.progressCount(), task.targetCount());
			if (next != task.progressCount()) {
				childDailyMissionPort.setProgress(task.id(), next);
			}
			return;
		}
	}

	private ChildTodayView viewOf(long childId) {
		Optional<StoredMission> mission = ensure(childId, today());
		if (mission.isEmpty()) {
			return ChildTodayView.empty();
		}
		long seconds = ChildDailyMissionRules.secondsRemaining(
				clock.instant(), ReadChildProgressService.PROGRESS_ZONE);
		return ChildDailyMissionRules.toView(mission.get(), seconds);
	}

	private Optional<StoredMission> ensure(long childId, LocalDate day) {
		Optional<StoredMission> existing = childDailyMissionPort.findByChildAndDate(childId, day);
		if (existing.isPresent()) {
			return existing;
		}
		List<NewTask> tasks = plan(childId);
		if (tasks.isEmpty()) {
			return Optional.empty();
		}
		StoredMission created = childDailyMissionPort.insert(new NewMission(
				childId, day, ChildDailyMissionRules.objective(tasks.size()), tasks));
		return Optional.of(created);
	}

	private List<NewTask> plan(long childId) {
		List<Long> chosenIds = childSubjectPort.linksOf(childId).stream()
				.filter(link -> !link.excluded())
				.map(StoredLink::subjectId)
				.toList();
		if (chosenIds.isEmpty()) {
			return List.of();
		}
		List<NewTask> tasks = new ArrayList<>();
		int position = 1;
		for (CatalogSubject subject : childSubjectPort.visibleByIds(chosenIds)) {
			if (tasks.size() >= ChildDailyMissionRules.MAX_TASKS) {
				break;
			}
			Optional<StoredQuestion> question = firstPlayable(subject.id());
			if (question.isEmpty()) {
				continue;
			}
			tasks.add(new NewTask(
					position++,
					subject.name(),
					ChildDailyMissionRules.TARGET_PER_TASK,
					ChildDailyMissionRules.REWARD_XP,
					subject.id(),
					question.get().id()));
		}
		return tasks;
	}

	private Optional<StoredQuestion> firstPlayable(long subjectId) {
		return teacherQuestionPort.findBySubject(subjectId, 0L, 50).stream()
				.filter(question -> QcmChildAnswerGrader.FORM.equals(question.formCode())
						|| TrueFalseChildAnswerGrader.FORM.equals(question.formCode())
						|| MemoryChildAnswerGrader.FORM.equals(question.formCode())
						|| OrderChildAnswerGrader.FORM.equals(question.formCode()))
				.findFirst();
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(ReadChildProgressService.PROGRESS_ZONE));
	}

	private long requireActiveChild(String rawToken) {
		long childId = childSessionTokenPort.read(rawToken)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE))
				.childId();
		if (childLoginPort.findActiveById(childId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
		}
		return childId;
	}
}
