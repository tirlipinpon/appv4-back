package com.appv4.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class AppHoursTest {

	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	@Test
	void leDefautEstOuvertDe6hA22hEtLaPhraseCite6h() {
		AppHours hours = AppHours.defaults();

		assertThat(hours.opensAt()).isEqualTo(LocalTime.of(6, 0));
		assertThat(hours.closesAt()).isEqualTo(LocalTime.of(22, 0));
		assertThat(hours.closed(LocalTime.of(5, 59))).isTrue();
		assertThat(hours.closed(LocalTime.of(6, 0))).isFalse();
		assertThat(hours.closed(LocalTime.of(21, 59))).isFalse();
		assertThat(hours.closed(LocalTime.of(22, 0))).isTrue();
		assertThat(hours.closedMessage()).isEqualTo("L'app est fermée jusqu'à 6 h.");
	}

	@Test
	void uneOuvertureA8h30CiteLesMinutes() {
		AppHours hours = new AppHours(LocalTime.of(8, 30), LocalTime.of(18, 0));

		assertThat(hours.closed(LocalTime.of(8, 29))).isTrue();
		assertThat(hours.closed(LocalTime.of(8, 30))).isFalse();
		assertThat(hours.closed(LocalTime.of(18, 0))).isTrue();
		assertThat(hours.closedMessage()).isEqualTo("L'app est fermée jusqu'à 8 h 30.");
	}

	@Test
	void laFermetureApresMinuitRemonteALaVeille() {
		AppHours hours = AppHours.defaults();
		Instant morning = Instant.parse("2026-11-23T04:59:00Z");

		assertThat(hours.closedSince(morning, PARIS))
				.isEqualTo(Instant.parse("2026-11-22T21:00:00Z"));
	}

	@Test
	void leDebutDoitResterAvantLaFin() {
		assertThatThrownBy(() -> new AppHours(LocalTime.of(18, 0), LocalTime.of(8, 0)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new AppHours(LocalTime.of(8, 0), LocalTime.of(8, 0)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
