package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.FormeRatee;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.ParentProgress;
import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.out.AppHoursPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.LectureEssai;
import com.appv4.auth.application.service.question.ClassificationQuestionForm;
import com.appv4.auth.application.service.question.MemoryQuestionForm;
import com.appv4.auth.application.service.question.QcmQuestionForm;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

class ReadParentProgressServiceTest {

	private static final Instant NOW = ZonedDateTime.of(2026, 11, 22, 23, 30, 0, 0, ReadChildProgressService.PROGRESS_ZONE)
			.toInstant();

	private final ChildPort children = mock(ChildPort.class);
	private final ChildQuestionAttemptPort attempts = mock(ChildQuestionAttemptPort.class);
	private final SejourUseCase sejours = mock(SejourUseCase.class);
	private final List<LectureEssai> essais = new ArrayList<>();
	private final List<Long> seconds = new ArrayList<>();
	private ReadParentProgressService service;

	@BeforeEach
	void akira() {
		essais.clear();
		seconds.clear();
		when(children.findOwned(1L, 7L)).thenReturn(Optional.of(child(true)));
		when(attempts.lectureEssais(7L)).thenAnswer(call -> List.copyOf(essais));
		when(sejours.countableSeconds(7L)).thenAnswer(call -> List.copyOf(seconds));
		service = service(AppHours.defaults());
	}

	@Test
	void sansEssaiNiSejourLesQuatreBlocsSontVides() {
		ParentProgress progress = service.read(1L, "parent", 7L);

		assertThat(progress.bestTimeSlotLabel()).isNull();
		assertThat(progress.averageSessionMinutes()).isNull();
		assertThat(progress.trend()).isNull();
		assertThat(progress.repeatedErrorPatterns()).isEmpty();
	}

	@Test
	void unAutreParentEstIntrouvable() {
		assertThatThrownBy(() -> service.read(2L, "parent", 7L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("Cet enfant est introuvable.");
	}

	@Test
	void unProfesseurEstRefuseCommeLesAutresRoutesParent() {
		assertThatThrownBy(() -> service.read(1L, "teacher", 7L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SpaceAccess.DENIED);
	}

	@Test
	void unEnfantInactifVisibleResteLisible() {
		when(children.findOwned(1L, 7L)).thenReturn(Optional.of(child(false)));

		assertThat(service.read(1L, "parent", 7L).repeatedErrorPatterns()).isEmpty();
	}

	@Test
	void leSoirGagneQuandLaReussiteYEstMeilleure() {
		rate(false, 10, 0);
		rate(false, 10, 0);
		rate(false, 10, 0);
		rate(true, 20, 0);

		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Soir");
	}

	@Test
	void unSeulEchecLeMatinNommeLeMatin() {
		rate(false, 9, 0);

		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Matin");
	}

	@Test
	void onzeHeuresCinquanteNeufEstLeMatinEtMidiEstLApresMidi() {
		rate(true, 11, 59);
		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Matin");

		essais.clear();
		rate(true, 12, 0);
		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Après-midi");
	}

	@Test
	void dixHuitHeuresEstLeSoirTantQueLAppEstOuverte() {
		rate(true, 18, 0);

		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Soir");
	}

	@Test
	void laFermetureADixHuitHeuresLaisseLeSoirDehors() {
		service = service(new AppHours(LocalTime.of(8, 0), LocalTime.of(18, 0)));
		rate(false, 18, 0);
		rate(false, 19, 0);
		rate(true, 8, 0);

		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Matin");
	}

	@Test
	void unEssaiDansLaNuitNeFaitPasUnCreneau() {
		rate(true, 23, 0);

		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isNull();
	}

	@Test
	void aTauxEgalLePlusDEssaisGagne() {
		rate(true, 10, 0);
		rate(false, 10, 0);
		rate(true, 20, 0);
		rate(true, 20, 0);
		rate(false, 20, 0);
		rate(false, 20, 0);

		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Soir");
	}

	@Test
	void aTauxEtNombreEgauxLeCreneauPlusTardGagne() {
		rate(true, 10, 0);
		rate(false, 10, 0);
		rate(true, 14, 0);
		rate(false, 14, 0);

		assertThat(service.read(1L, "parent", 7L).bestTimeSlotLabel()).isEqualTo("Après-midi");
	}

	@Test
	void quatreVingtDixEtTrenteSecondesFontUneMinute() {
		seconds.add(90L);
		seconds.add(30L);

		assertThat(service.read(1L, "parent", 7L).averageSessionMinutes()).isEqualTo(1);
	}

	@Test
	void vingtSecondesDonnentZeroEtTrenteSecondesDonnentUneMinute() {
		seconds.add(20L);
		assertThat(service.read(1L, "parent", 7L).averageSessionMinutes()).isZero();

		seconds.clear();
		seconds.add(30L);
		assertThat(service.read(1L, "parent", 7L).averageSessionMinutes()).isEqualTo(1);
	}

	@Test
	void quatreVingtNeufSecondesFontUneMinuteEtQuatreVingtDixEnFontDeux() {
		seconds.add(89L);
		assertThat(service.read(1L, "parent", 7L).averageSessionMinutes()).isEqualTo(1);

		seconds.clear();
		seconds.add(90L);
		assertThat(service.read(1L, "parent", 7L).averageSessionMinutes()).isEqualTo(2);
	}

	@Test
	void unSejourSousUneSecondeNeComptePas() {
		seconds.add(0L);
		seconds.add(90L);

		assertThat(service.read(1L, "parent", 7L).averageSessionMinutes()).isEqualTo(2);
	}

	@Test
	void uneFenetreVideLaisseLaTendanceAbsente() {
		essai(true, NOW.minus(SuccessWindows.WEEK).plusSeconds(60), "qcm");

		assertThat(service.read(1L, "parent", 7L).trend()).isNull();
	}

	@Test
	void desTauxEgauxSontStablesSurSeptJours() {
		essai(true, recent(), "qcm");
		essai(false, recent(), "qcm");
		essai(true, previous(), "qcm");
		essai(false, previous(), "qcm");

		var trend = service.read(1L, "parent", 7L).trend();

		assertThat(trend.direction()).isEqualTo("stable");
		assertThat(trend.deltaSuccessPoints()).isEqualByComparingTo(new BigDecimal("0.0"));
		assertThat(trend.windowLabel()).isEqualTo("7 jours");
	}

	@Test
	void huitPointsDeMoinsSontUneBaisseEtSeptNeufRestentStables() {
		fill(920, 1000, recent());
		fill(1000, 1000, previous());
		var baisse = service.read(1L, "parent", 7L).trend();
		assertThat(baisse.direction()).isEqualTo("baisse");
		assertThat(baisse.deltaSuccessPoints()).isEqualByComparingTo("-8.0");

		essais.clear();
		fill(921, 1000, recent());
		fill(1000, 1000, previous());
		var stable = service.read(1L, "parent", 7L).trend();
		assertThat(stable.direction()).isEqualTo("stable");
		assertThat(stable.deltaSuccessPoints()).isEqualByComparingTo("-7.9");
	}

	@Test
	void septNeufCinqPointsDePlusRestentStablesEtHuitSontUneHausse() {
		fill(10000, 10000, recent());
		fill(9205, 10000, previous());
		var stable = service.read(1L, "parent", 7L).trend();
		assertThat(stable.direction()).isEqualTo("stable");
		assertThat(stable.deltaSuccessPoints()).isEqualByComparingTo("7.9");

		essais.clear();
		fill(1000, 1000, recent());
		fill(920, 1000, previous());
		var hausse = service.read(1L, "parent", 7L).trend();
		assertThat(hausse.direction()).isEqualTo("hausse");
		assertThat(hausse.deltaSuccessPoints()).isEqualByComparingTo("8.0");
	}

	@Test
	void lEssaiPileSurLaCoupureResteDansLaFenetreAncienne() {
		Instant cut = NOW.minus(SuccessWindows.WEEK);
		essai(true, cut, "qcm");
		essai(false, cut.plusSeconds(1), "qcm");
		essai(false, cut.minus(SuccessWindows.WEEK).plusSeconds(1), "qcm");

		assertThat(service.read(1L, "parent", 7L).trend().direction()).isEqualTo("baisse");
	}

	@Test
	void unSeulEchecNeFaitPasUneFormeRatee() {
		essai(false, recent(), "qcm");

		assertThat(service.read(1L, "parent", 7L).repeatedErrorPatterns()).isEmpty();
	}

	@Test
	void deuxQcmRatesDonnentChoixMultiples() {
		essai(false, recent(), "qcm");
		essai(false, previous(), "qcm");
		essai(true, recent(), "qcm");

		assertThat(service.read(1L, "parent", 7L).repeatedErrorPatterns())
				.containsExactly(new FormeRatee("qcm", 2, "Choix multiples"));
	}

	@Test
	void laFormeLaPlusRateePasseDevantPuisLOrdreFrancais() {
		essai(false, recent(), "memory");
		essai(false, recent(), "memory");
		essai(false, recent(), "memory");
		essai(false, recent(), "qcm");
		essai(false, recent(), "qcm");

		assertThat(service.read(1L, "parent", 7L).repeatedErrorPatterns())
				.extracting(FormeRatee::label)
				.containsExactly("Memory", "Choix multiples");

		essais.clear();
		essai(false, recent(), "classement");
		essai(false, recent(), "classement");
		essai(false, recent(), "qcm");
		essai(false, recent(), "qcm");

		assertThat(service.read(1L, "parent", 7L).repeatedErrorPatterns())
				.extracting(FormeRatee::label)
				.containsExactly("Choix multiples", "Classement");
	}

	@Test
	void uneFormeInconnueDuCatalogueEstAbsente() {
		for (int i = 0; i < 5; i++) {
			essai(false, recent(), "inconnu");
		}

		assertThat(service.read(1L, "parent", 7L).repeatedErrorPatterns()).isEmpty();
	}

	private ReadParentProgressService service(AppHours hours) {
		AppHoursPort port = mock(AppHoursPort.class);
		when(port.current()).thenReturn(hours);
		return new ReadParentProgressService(
				children,
				attempts,
				sejours,
				port,
				new QuestionFormRegistry(List.of(
						new QcmQuestionForm(), new MemoryQuestionForm(), new ClassificationQuestionForm())),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static ChildSaved child(boolean active) {
		return new ChildSaved(7L, 1L, "Akira", "Iris", "F", 8, "fr", "CE2", 1L, null, false, active);
	}

	private void rate(boolean passed, int hour, int minute) {
		essai(passed, paris(hour, minute), "qcm");
	}

	private void essai(boolean passed, Instant at, String form) {
		essais.add(new LectureEssai(at, passed, form));
	}

	private void fill(int passed, int total, Instant at) {
		for (int i = 0; i < passed; i++) {
			essai(true, at.plusSeconds(i), "qcm");
		}
		for (int i = passed; i < total; i++) {
			essai(false, at.plusSeconds(i), "qcm");
		}
	}

	private static Instant recent() {
		return NOW.minus(SuccessWindows.WEEK).plusSeconds(3600);
	}

	private static Instant previous() {
		return NOW.minus(SuccessWindows.WEEK).minus(SuccessWindows.WEEK).plusSeconds(3600);
	}

	private static Instant paris(int hour, int minute) {
		return ZonedDateTime.of(2026, 11, 22, hour, minute, 0, 0, ReadChildProgressService.PROGRESS_ZONE).toInstant();
	}
}
