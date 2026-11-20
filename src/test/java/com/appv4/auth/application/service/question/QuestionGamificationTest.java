package com.appv4.auth.application.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;

class QuestionGamificationTest {

	@Test
	void aMissingSettingBecomesOneEasyAttempt() {
		QuestionGamification.Resolved resolved = QuestionGamification.defaults();

		assertThat(resolved.maxAttempts()).isEqualTo(1);
		assertThat(resolved.difficulty()).isEqualTo("facile");
		assertThat(resolved.difficultyLabel()).isEqualTo("Facile");
		assertThat(resolved.customizePoints()).isFalse();
		assertThat(resolved.gamificationDisabled()).isFalse();
		assertThat(resolved.points()).containsExactly(10);
	}

	@Test
	void turningGamificationOffKeepsThePoints() {
		QuestionGamification.Resolved resolved = QuestionGamification.resolve(1, "facile", false, true, null);

		assertThat(resolved.gamificationDisabled()).isTrue();
		assertThat(resolved.points()).containsExactly(10);
	}

	@Test
	void customPointsMustMatchTheAttemptCount() {
		assertThatThrownBy(() -> QuestionGamification.resolve(2, "modere", true, false, List.of(20)))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(QuestionGamification.POINTS_LENGTH);
	}
}
