package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.FailedEssai;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.FormeRatee;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.MissionDay;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.ParentProgress;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.ProgressDay;
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.MissionEssai;
import com.appv4.auth.application.port.out.QuestionTextPort;
import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.out.AppHoursPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;
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
	private final ChildProgressPort progress = mock(ChildProgressPort.class);
	private final ChildQuestionAttemptPort attempts = mock(ChildQuestionAttemptPort.class);
	private final ChildDailyMissionPort missionPort = mock(ChildDailyMissionPort.class);
	private final QuestionTextPort questionTexts = mock(QuestionTextPort.class);
	private final SejourUseCase sejours = mock(SejourUseCase.class);
	private final List<LectureEssai> essais = new ArrayList<>();
	private final List<StoredMission> storedMissions = new ArrayList<>();
	private final List<LocalDate> finishedMissionDays = new ArrayList<>();
	private final List<MissionEssai> missionEssais = new ArrayList<>();
	private final Map<Long, String> prompts = new HashMap<>();
	private final List<Long> seconds = new ArrayList<>();
	private ReadParentProgressService service;

	@BeforeEach
	void akira() {
		essais.clear();
		seconds.clear();
		storedMissions.clear();
		finishedMissionDays.clear();
		missionEssais.clear();
		prompts.clear();
		when(missionPort.missionsOf(7L)).thenAnswer(call -> List.copyOf(storedMissions));
		when(missionPort.finishedDays(7L)).thenAnswer(call -> List.copyOf(finishedMissionDays));
		when(attempts.missionEssais(7L)).thenAnswer(call -> List.copyOf(missionEssais));
		when(questionTexts.promptsOf(any())).thenAnswer(call -> Map.copyOf(prompts));
		when(children.findOwned(1L, 7L)).thenReturn(Optional.of(child(true)));
		when(progress.findByChildId(7L)).thenReturn(Optional.empty());
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

	@Test
	void sansProgressionLaPhotoEstVideEtLesJoursAussi() {
		ParentProgress progress = service.read(1L, "parent", 7L);

		assertThat(progress.xpTotal()).isZero();
		assertThat(progress.currentStreak()).isZero();
		assertThat(progress.leagueLabel()).isEqualTo("Bronze");
		assertThat(progress.days()).isEmpty();
		assertThat(progress.recentSuccess()).isNull();
		assertThat(progress.bestTimeSlotLabel()).isNull();
	}

	@Test
	void cinquanteXpSansEssaiResteArgent() {
		fiche(50, 4, LocalDate.of(2026, 11, 22));

		ParentProgress read = service.read(1L, "parent", 7L);

		assertThat(read.xpTotal()).isEqualTo(50);
		assertThat(read.leagueLabel()).isEqualTo("Argent");
		assertThat(read.currentStreak()).isEqualTo(4);
		assertThat(read.days()).isEmpty();
	}

	@Test
	void quaranteNeufXpRestentBronzeEtTroisCentCinquanteSontDiamant() {
		fiche(49, 1, LocalDate.of(2026, 11, 22));
		assertThat(service.read(1L, "parent", 7L).leagueLabel()).isEqualTo("Bronze");

		fiche(350, 1, LocalDate.of(2026, 11, 22));
		assertThat(service.read(1L, "parent", 7L).leagueLabel()).isEqualTo("Diamant");
	}

	@Test
	void unRetourAvantHierRameneLaSerieAZero() {
		fiche(10, 4, LocalDate.of(2026, 11, 20));

		assertThat(service.read(1L, "parent", 7L).currentStreak()).isZero();
	}

	@Test
	void unEssaiPayeEtUnRateFontLeJour() {
		fiche(8, 1, LocalDate.of(2026, 11, 22));
		essais.add(new LectureEssai(paris(20, 0), true, "qcm", 80, true));
		essais.add(new LectureEssai(paris(21, 0), false, "qcm", 40, false));

		assertThat(service.read(1L, "parent", 7L).days())
				.containsExactly(new ProgressDay(LocalDate.of(2026, 11, 22), 8, 8, 1, 1));
	}

	@Test
	void leJourVideEntreDeuxEssaisNaPasDeCompte() {
		fiche(10, 1, LocalDate.of(2026, 11, 22));
		essais.add(new LectureEssai(parisOn(20, 10, 0), true, "qcm", 100, true));

		assertThat(service.read(1L, "parent", 7L).days()).containsExactly(
				new ProgressDay(LocalDate.of(2026, 11, 20), 10, 10, 1, 0),
				new ProgressDay(LocalDate.of(2026, 11, 21), 0, 10, null, null),
				new ProgressDay(LocalDate.of(2026, 11, 22), 0, 10, null, null));
	}

	@Test
	void lXpDejaStockeCompleteLePremierJour() {
		fiche(20, 1, LocalDate.of(2026, 11, 22));
		essais.add(new LectureEssai(paris(18, 0), true, "qcm", 80, true));

		assertThat(service.read(1L, "parent", 7L).days())
				.containsExactly(new ProgressDay(LocalDate.of(2026, 11, 22), 8, 20, 1, 0));
	}

	@Test
	void deuxReussisSurTroisFontSoixanteSixSixSurTroisJours() {
		essais.add(new LectureEssai(paris(20, 0), true, "qcm", 0, false));
		essais.add(new LectureEssai(paris(21, 0), true, "qcm", 0, false));
		essais.add(new LectureEssai(paris(22, 0), false, "qcm", 0, false));
		essais.add(new LectureEssai(NOW.minus(ReadParentProgressService.THREE_DAYS), true, "qcm", 0, false));

		var success = service.read(1L, "parent", 7L).recentSuccess();

		assertThat(success.successRate()).isEqualByComparingTo("66.6");
		assertThat(success.attemptCount()).isEqualTo(3);
		assertThat(success.windowLabel()).isEqualTo("3 jours");
	}

	@Test
	void sansMissionLaSemaineDuDimancheEstVide() {
		ParentProgress progress = service.read(1L, "parent", 7L);

		assertThat(progress.dailyMissionSummary()).isNull();
		assertThat(progress.dailyMissionDays()).extracting(MissionDay::date).containsExactly(
				LocalDate.of(2026, 11, 16),
				LocalDate.of(2026, 11, 17),
				LocalDate.of(2026, 11, 18),
				LocalDate.of(2026, 11, 19),
				LocalDate.of(2026, 11, 20),
				LocalDate.of(2026, 11, 21),
				LocalDate.of(2026, 11, 22));
		assertThat(progress.dailyMissionDays()).allSatisfy(day -> {
			assertThat(day.status()).isNull();
			assertThat(day.objective()).isNull();
			assertThat(day.progress()).isNull();
			assertThat(day.target()).isNull();
			assertThat(day.failedEssais()).isNull();
		});
		assertThat(progress.bestTimeSlotLabel()).isNull();
		verify(missionPort, never()).insert(any());
		verify(missionPort, never()).setProgress(anyLong(), anyInt());
		verify(missionPort, never()).rememberFinishedDay(anyLong(), any());
	}

	@Test
	void missionEnCoursPorteLesEssaisRatesDeSesQuestions() {
		storedMissions.add(mission(
				LocalDate.of(2026, 11, 22),
				"Joue 3 questions aujourd'hui",
				task(1, "Maths", 1, 1, 11),
				task(2, "Français", 1, 1, 12),
				task(3, "Anglais", 0, 1, 13)));
		prompts.put(11L, "Exemple de texte : 3 + 4");
		missionEssais.add(new MissionEssai(2, 11, false, paris(18, 5)));
		missionEssais.add(new MissionEssai(1, 11, false, paris(18, 0)));
		missionEssais.add(new MissionEssai(3, 11, true, paris(18, 10)));
		missionEssais.add(new MissionEssai(4, 99, false, paris(18, 2)));
		missionEssais.add(new MissionEssai(5, 11, false, parisOn(15, 23, 30)));
		missionEssais.add(new MissionEssai(6, 11, false, NOW.plusSeconds(1)));

		ParentProgress progress = service.read(1L, "parent", 7L);
		MissionDay today = progress.dailyMissionSummary();

		assertThat(today).isEqualTo(progress.dailyMissionDays().get(6));
		assertThat(today.objective()).isEqualTo("Joue 3 questions aujourd'hui");
		assertThat(today.progress()).isEqualTo(2);
		assertThat(today.target()).isEqualTo(3);
		assertThat(today.status()).isEqualTo("en_cours");
		assertThat(today.statusLabel()).isEqualTo("En cours");
		assertThat(today.failedEssais()).containsExactly(
				new FailedEssai("Maths", "Exemple de texte : 3 + 4", paris(18, 0)),
				new FailedEssai("Maths", "Exemple de texte : 3 + 4", paris(18, 5)));
	}

	@Test
	void missionTermineeSansEchecAUneListeVide() {
		storedMissions.add(mission(
				LocalDate.of(2026, 11, 22),
				"Joue 3 questions aujourd'hui",
				task(1, "Maths", 1, 1, 11),
				task(2, "Français", 1, 1, 12),
				task(3, "Anglais", 1, 1, 13)));

		MissionDay today = service.read(1L, "parent", 7L).dailyMissionSummary();

		assertThat(today.status()).isEqualTo("terminee");
		assertThat(today.statusLabel()).isEqualTo("Terminée");
		assertThat(today.progress()).isEqualTo(3);
		assertThat(today.target()).isEqualTo(3);
		assertThat(today.failedEssais()).isEmpty();
	}

	@Test
	void unJourPasseNonFiniResteEnCours() {
		storedMissions.add(mission(
				LocalDate.of(2026, 11, 21),
				"Joue 1 question aujourd'hui",
				task(1, "Maths", 0, 1, 11)));

		MissionDay day = service.read(1L, "parent", 7L).dailyMissionDays().get(5);

		assertThat(day.date()).isEqualTo(LocalDate.of(2026, 11, 21));
		assertThat(day.progress()).isZero();
		assertThat(day.target()).isEqualTo(1);
		assertThat(day.status()).isEqualTo("en_cours");
		assertThat(service.read(1L, "parent", 7L).dailyMissionSummary()).isNull();
	}

	@Test
	void uneMissionPlusAncienneOuvreLaSemainePrecedente() {
		storedMissions.add(mission(
				LocalDate.of(2026, 11, 12),
				"Joue 2 questions aujourd'hui",
				task(1, "Maths", 2, 2, 11)));

		assertThat(service.read(1L, "parent", 7L).dailyMissionDays()).extracting(MissionDay::date)
				.containsExactly(
						LocalDate.of(2026, 11, 9),
						LocalDate.of(2026, 11, 10),
						LocalDate.of(2026, 11, 11),
						LocalDate.of(2026, 11, 12),
						LocalDate.of(2026, 11, 13),
						LocalDate.of(2026, 11, 14),
						LocalDate.of(2026, 11, 15),
						LocalDate.of(2026, 11, 16),
						LocalDate.of(2026, 11, 17),
						LocalDate.of(2026, 11, 18),
						LocalDate.of(2026, 11, 19),
						LocalDate.of(2026, 11, 20),
						LocalDate.of(2026, 11, 21),
						LocalDate.of(2026, 11, 22));
	}

	@Test
	void questionSupprimeeRecalculeEtLeJourFiniSansTacheResteTermine() {
		storedMissions.add(mission(
				LocalDate.of(2026, 11, 22),
				"Joue 3 questions aujourd'hui",
				task(1, "Maths", 1, 1, 11),
				task(2, "Français", 1, 1, 12)));
		finishedMissionDays.add(LocalDate.of(2026, 11, 21));
		storedMissions.add(new StoredMission(2, 7, LocalDate.of(2026, 11, 21), "Joue 1 question aujourd'hui", List.of()));
		storedMissions.add(new StoredMission(3, 7, LocalDate.of(2026, 11, 20), "Joue 1 question aujourd'hui", List.of()));

		List<MissionDay> days = service.read(1L, "parent", 7L).dailyMissionDays();

		assertThat(days.get(6).status()).isEqualTo("terminee");
		assertThat(days.get(6).progress()).isEqualTo(2);
		assertThat(days.get(6).target()).isEqualTo(2);
		assertThat(days.get(5).status()).isEqualTo("terminee");
		assertThat(days.get(5).progress()).isNull();
		assertThat(days.get(5).target()).isNull();
		assertThat(days.get(5).objective()).isEqualTo("Joue 1 question aujourd'hui");
		assertThat(days.get(5).failedEssais()).isEmpty();
		assertThat(days.get(4).status()).isEqualTo("en_cours");
		assertThat(days.get(4).progress()).isZero();
		assertThat(days.get(4).target()).isZero();
	}

	@Test
	void unMercrediSansMissionCouvreLeLundiAuDimanche() {
		Clock wednesday = Clock.fixed(parisOn(25, 12, 0), ZoneOffset.UTC);
		ReadParentProgressService midweek = new ReadParentProgressService(
				children,
				progress,
				attempts,
				missionPort,
				questionTexts,
				sejours,
				hours(),
				new QuestionFormRegistry(List.of(new QcmQuestionForm())),
				wednesday);

		assertThat(midweek.read(1L, "parent", 7L).dailyMissionDays()).extracting(MissionDay::date)
				.containsExactly(
						LocalDate.of(2026, 11, 23),
						LocalDate.of(2026, 11, 24),
						LocalDate.of(2026, 11, 25),
						LocalDate.of(2026, 11, 26),
						LocalDate.of(2026, 11, 27),
						LocalDate.of(2026, 11, 28),
						LocalDate.of(2026, 11, 29));
	}

	private AppHoursPort hours() {
		AppHoursPort port = mock(AppHoursPort.class);
		when(port.current()).thenReturn(AppHours.defaults());
		return port;
	}

	private static StoredMission mission(LocalDate date, String objective, StoredTask... tasks) {
		return new StoredMission(1, 7, date, objective, List.of(tasks));
	}

	private static StoredTask task(long id, String label, int progress, int target, long questionId) {
		return new StoredTask(id, 1, label, target, progress, 10, 3, questionId);
	}

	private ReadParentProgressService service(AppHours hours) {
		AppHoursPort port = mock(AppHoursPort.class);
		when(port.current()).thenReturn(hours);
		return new ReadParentProgressService(
				children,
				progress,
				attempts,
				missionPort,
				questionTexts,
				sejours,
				port,
				new QuestionFormRegistry(List.of(
						new QcmQuestionForm(), new MemoryQuestionForm(), new ClassificationQuestionForm())),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private void fiche(int xp, int streak, LocalDate lastPlay) {
		when(progress.findByChildId(7L)).thenReturn(Optional.of(
				new StoredProgress(7L, xp, streak, lastPlay, LocalDate.of(2026, 11, 16), "0000000")));
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
		return parisOn(22, hour, minute);
	}

	private static Instant parisOn(int day, int hour, int minute) {
		return ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, ReadChildProgressService.PROGRESS_ZONE).toInstant();
	}
}
