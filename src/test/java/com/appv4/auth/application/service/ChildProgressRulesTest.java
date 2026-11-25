package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;
import com.appv4.auth.application.service.ChildProgressRules.ChildProgressViewSnapshot;

class ChildProgressRulesTest {

	@Test
	void xpGainIsScoreDividedByTen() {
		assertThat(ChildProgressRules.xpGain(100)).isEqualTo(10);
		assertThat(ChildProgressRules.xpGain(45)).isEqualTo(4);
		assertThat(ChildProgressRules.xpGain(0)).isEqualTo(0);
	}

	@Test
	void leagueFollowsXpThresholds() {
		assertThat(ChildProgressRules.leagueLabel(0)).isEqualTo("Bronze");
		assertThat(ChildProgressRules.leagueLabel(49)).isEqualTo("Bronze");
		assertThat(ChildProgressRules.leagueLabel(50)).isEqualTo("Argent");
		assertThat(ChildProgressRules.leagueLabel(150)).isEqualTo("Or");
		assertThat(ChildProgressRules.leagueLabel(350)).isEqualTo("Diamant");
	}

	@Test
	void firstPlayOfDayStartsStreakAtOne() {
		StoredProgress next = ChildProgressRules.afterAttempt(
				ChildProgressRules.empty(7L), 100, LocalDate.of(2026, 11, 18));
		assertThat(next.xpTotal()).isEqualTo(10);
		assertThat(next.currentStreak()).isEqualTo(1);
		assertThat(next.lastPlayDate()).isEqualTo(LocalDate.of(2026, 11, 18));
		assertThat(next.weekDays().charAt(2)).isEqualTo('1'); // mercredi
	}

	@Test
	void consecutiveDayIncrementsStreak() {
		StoredProgress current = new StoredProgress(
				7L, 10, 1, LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 16), "0100000");
		StoredProgress next = ChildProgressRules.afterAttempt(current, 80, LocalDate.of(2026, 11, 18));
		assertThat(next.currentStreak()).isEqualTo(2);
		assertThat(next.xpTotal()).isEqualTo(18);
	}

	@Test
	void sameDayDoesNotDoubleStreak() {
		StoredProgress current = new StoredProgress(
				7L, 10, 3, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 16), "0010000");
		StoredProgress next = ChildProgressRules.afterAttempt(current, 100, LocalDate.of(2026, 11, 18));
		assertThat(next.currentStreak()).isEqualTo(3);
		assertThat(next.xpTotal()).isEqualTo(20);
	}

	@Test
	void gapResetsStreakToOne() {
		StoredProgress current = new StoredProgress(
				7L, 40, 5, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9), "0000001");
		StoredProgress next = ChildProgressRules.afterAttempt(current, 50, LocalDate.of(2026, 11, 18));
		assertThat(next.currentStreak()).isEqualTo(1);
	}

	@Test
	void visibleStreakBreaksWhenYesterdayMissed() {
		assertThat(ChildProgressRules.visibleStreak(4, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 18)))
				.isEqualTo(0);
		assertThat(ChildProgressRules.visibleStreak(4, LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18)))
				.isEqualTo(4);
	}

	@Test
	void weekDaysResetOnNewIsoWeek() {
		StoredProgress current = new StoredProgress(
				7L, 0, 0, LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 9), "1111111");
		StoredProgress next = ChildProgressRules.afterAttempt(current, 0, LocalDate.of(2026, 11, 16));
		assertThat(next.weekStartDate()).isEqualTo(LocalDate.of(2026, 11, 16));
		assertThat(next.weekDays()).isEqualTo("1000000");
	}

	@Test
	void weekYellowCountsKeepOneQuestionPerDayAndHideTheFuture() {
		LocalDate wednesday = LocalDate.of(2026, 11, 18);
		List<ChildProgressRules.YellowGain> gains = List.of(
				new ChildProgressRules.YellowGain(11L, LocalDate.of(2026, 11, 16)),
				new ChildProgressRules.YellowGain(12L, wednesday),
				new ChildProgressRules.YellowGain(12L, wednesday));

		assertThat(ChildProgressRules.weekYellowCounts(gains, wednesday))
				.containsExactly(1, 0, 1, null, null, null, null);
	}

	@Test
	void toViewExposesSevenBooleans() {
		StoredProgress stored = new StoredProgress(
				7L, 60, 2, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 16), "0010000");
		ChildProgressViewSnapshot view = ChildProgressRules.toView(stored, LocalDate.of(2026, 11, 18));
		assertThat(view.xpTotal()).isEqualTo(60);
		assertThat(view.leagueLabel()).isEqualTo("Argent");
		assertThat(view.weekDays()).isEqualTo(List.of(false, false, true, false, false, false, false));
	}
}
