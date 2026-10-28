package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class QuestionContentBatchTest {

	@Test
	void newStructureUsesItsOwnStoreAndSplitsLargeIdLists() {
		BackSubjectQuestionJpaRepository questions = mock(BackSubjectQuestionJpaRepository.class);
		BackSubjectQuestionFormJpaRepository forms = mock(BackSubjectQuestionFormJpaRepository.class);
		QuestionContentStore store = mock(QuestionContentStore.class);
		when(store.formCode()).thenReturn("custom_image");
		List<BackSubjectQuestionEntity> entities = new ArrayList<>();
		for (long id = 1; id <= 501; id++) {
			BackSubjectQuestionEntity entity = new BackSubjectQuestionEntity(4, "custom_image", "Image");
			ReflectionTestUtils.setField(entity, "id", id);
			entities.add(entity);
		}
		when(questions.findBySubjectIdOrderByIdAsc(4)).thenReturn(entities);
		when(store.load(anyList())).thenAnswer(invocation -> {
			List<Long> ids = invocation.getArgument(0);
			return ids.stream().collect(Collectors.toMap(Function.identity(), id -> new ImageContent("image-" + id)));
		});
		TeacherQuestionPersistenceAdapter adapter = new TeacherQuestionPersistenceAdapter(questions, forms, List.of(store));
		List<StoredQuestion> result = adapter.findBySubject(4);
		assertThat(result).hasSize(501);
		assertThat(result.get(500).content()).isEqualTo(new ImageContent("image-501"));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Long>> batches = ArgumentCaptor.forClass(List.class);
		verify(store, times(2)).load(batches.capture());
		assertThat(batches.getAllValues()).extracting(List::size).containsExactly(500, 1);
		verifyNoInteractions(forms);
	}

	@Test
	void missingStoreFailsInsteadOfReturningAnEmptyQuestion() {
		BackSubjectQuestionJpaRepository questions = mock(BackSubjectQuestionJpaRepository.class);
		when(questions.findBySubjectIdOrderByIdAsc(4))
				.thenReturn(List.of(new BackSubjectQuestionEntity(4, "unknown", "X")));
		TeacherQuestionPersistenceAdapter adapter = new TeacherQuestionPersistenceAdapter(
				questions, mock(BackSubjectQuestionFormJpaRepository.class), List.of());
		assertThatThrownBy(() -> adapter.findBySubject(4)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void duplicateStoresFailAtStartup() {
		QuestionContentStore store = mock(QuestionContentStore.class);
		when(store.formCode()).thenReturn("qcm");
		assertThatThrownBy(() -> new TeacherQuestionPersistenceAdapter(
				mock(BackSubjectQuestionJpaRepository.class),
				mock(BackSubjectQuestionFormJpaRepository.class), List.of(store, store)))
				.isInstanceOf(IllegalStateException.class);
	}

	private record ImageContent(String imageId) implements StoredContent {
	}
}
