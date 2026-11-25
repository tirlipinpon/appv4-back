package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class SansFauteRulesTest {

	@Test
	void threeQuestionsStartAtThirtyThenClimbByFifteen() {
		assertThat(SansFauteRules.bonusXp(3, 0)).isEqualTo(30);
		assertThat(SansFauteRules.bonusXp(3, 1)).isEqualTo(45);
		assertThat(SansFauteRules.bonusXp(3, 2)).isEqualTo(60);
		assertThat(SansFauteRules.bonusXp(3, 3)).isEqualTo(75);
	}

	@Test
	void oneQuestionStartsAtTen() {
		assertThat(SansFauteRules.bonusXp(1, 0)).isEqualTo(10);
		assertThat(SansFauteRules.bonusXp(1, 1)).isEqualTo(15);
	}

	@Test
	void streakStopsWhenYesterdayWasNotSansFaute() {
		LocalDate wednesday = LocalDate.of(2026, 11, 18);
		assertThat(SansFauteRules.streakBefore(List.of(), wednesday)).isZero();
		assertThat(SansFauteRules.streakBefore(List.of(LocalDate.of(2026, 11, 17)), wednesday)).isEqualTo(1);
		assertThat(SansFauteRules.streakBefore(
				List.of(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 17)), wednesday)).isEqualTo(2);
		assertThat(SansFauteRules.streakBefore(List.of(LocalDate.of(2026, 11, 16)), wednesday)).isZero();
	}
}
