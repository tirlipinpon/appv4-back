package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.SejourUseCase.ChildSejour;
import com.appv4.auth.application.port.in.SejourUseCase.Journee;
import com.appv4.auth.application.port.in.SejourUseCase.JourneeLine;
import com.appv4.auth.application.port.in.SejourUseCase.JourneeMonth;
import com.appv4.auth.application.port.in.SejourUseCase.Line;
import com.appv4.auth.application.port.in.SejourUseCase.Plafond;
import com.appv4.auth.application.port.in.SejourUseCase.ParentSejour;
import com.appv4.auth.application.port.out.AppHoursPort;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildSharePort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledChild;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledPhoto;
import com.appv4.auth.application.port.out.SejourPort;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.SejourPort.OpenSlice;
import com.appv4.auth.application.port.out.SejourPort.StoredInterval;
import com.appv4.auth.application.port.out.SejourPort.StoredSejour;
import com.appv4.auth.application.port.out.SejourQuestionPort;
import com.appv4.auth.application.port.out.SejourQuestionPort.QuestionCaption;

class SejourServiceTest {

	/** 16:00 à Paris, le 22 novembre 2026. */
	private static final Instant AT_16 = Instant.parse("2026-11-22T15:00:00Z");

	private final ChildSessionTokenPort sessions = mock(ChildSessionTokenPort.class);
	private final ChildLoginPort logins = mock(ChildLoginPort.class);
	private final ChildPort children = mock(ChildPort.class);
	private final MemorySejours store = new MemorySejours();
	private final MoveableClock clock = new MoveableClock(AT_16);
	private final Map<Long, Integer> plafonds = new HashMap<>();
	private SejourService sejour;

	@BeforeEach
	void akira() {
		plafonds.clear();
		when(sessions.read("akira")).thenReturn(Optional.of(new ChildSession(7L)));
		when(logins.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Akira", "hash")));
		when(children.findOwned(1L, 7L)).thenReturn(Optional.of(child(7L, 1L, true)));
		when(children.findPlafond(anyLong()))
				.thenAnswer(call -> Optional.ofNullable(plafonds.get(call.getArgument(0))));
		doAnswer(call -> {
			plafonds.put(call.getArgument(0), call.getArgument(1));
			return null;
		}).when(children).savePlafond(anyLong(), nullable(Integer.class));
		sejour = new SejourService(sessions, logins, children, store, clock);
	}

	@Test
	void lePremierGesteOuvreLeSejour() {
		clock.at(AT_16);
		sejour.signal("akira", "gesture", "tablette", null);

		assertThat(sejour.readChild("akira", "tablette").holds()).isTrue();
		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();

		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "screen", "tablette", "accueil");

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(row.durationLabel()).isEqualTo("30 s");
		assertThat(row.lines()).extracting(Line::label).containsExactly("Accueil");
	}

	@Test
	void unePresenceConfirmeTrenteSecondesEtLaLectureUlterieureResteLa() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));

		assertThat(row.durationLabel()).isEqualTo("30 s");
		assertThat(row.dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(row.timeLabel()).isEqualTo("16:00");
		assertThat(row.lines()).extracting(Line::label, Line::durationLabel).containsExactly(tuple("Accueil", "30 s"));

		clock.at(AT_16.plusSeconds(40));
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("30 s");
	}

	@Test
	void unDepartADixSecondesConfirmeDixSecondesSansDelaiEnsuite() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(10));
		sejour.signal("akira", "leave", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("10 s");
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).lines())
				.extracting(Line::label, Line::durationLabel)
				.containsExactly(tuple("Accueil", "10 s"));

		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("10 s");
	}

	@Test
	void lesPresencesJusquaDeuxMinutesConfirmentPuisCelleDApresNAjouteRien() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);
		clock.at(AT_16.plusSeconds(60));
		sejour.signal("akira", "presence", "tablette", null);
		clock.at(AT_16.plusSeconds(90));
		sejour.signal("akira", "presence", "tablette", null);
		clock.at(AT_16.plusSeconds(120));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");

		clock.at(AT_16.plusSeconds(150));
		sejour.signal("akira", "presence", "tablette", null);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");
	}

	@Test
	void leDernierAppareilTientLeSejourEtLAncienGardeSonDernierSignal() {
		clock.at(AT_16.plusSeconds(180));
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(300));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");
		assertThat(sejour.readChild("akira", "tablette").holds()).isTrue();

		clock.at(AT_16.plusSeconds(600));
		sejour.signal("akira", "screen", "pc", "matieres");

		List<ParentSejour> afterPc = sejour.readParent(1L, "parent", 7L);
		assertThat(afterPc).hasSize(1);
		assertThat(afterPc.get(0).durationLabel()).isEqualTo("2 min");
		assertThat(afterPc.get(0).lines()).extracting(Line::label).containsExactly("Accueil");

		clock.at(AT_16.plusSeconds(660));
		sejour.signal("akira", "presence", "tablette", null);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");
		assertThat(sejour.readChild("akira", "tablette").holds()).isFalse();
		assertThat(sejour.readChild("akira", "pc").holds()).isTrue();

		clock.at(AT_16.plusSeconds(690));
		sejour.signal("akira", "presence", "pc", null);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");

		clock.at(AT_16.plusSeconds(720));
		sejour.signal("akira", "gesture", "tablette", null);
		ChildSejour tablet = sejour.readChild("akira", "tablette");
		assertThat(tablet.holds()).isTrue();
		assertThat(sejour.readChild("akira", "pc").holds()).isFalse();
		assertThat(sejour.readParent(1L, "parent", 7L)).hasSize(1);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");
	}

	@Test
	void deuxPassagesSurAccueilFontUneSeuleLigne() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(20));
		sejour.signal("akira", "screen", "tablette", "matieres");
		clock.at(AT_16.plusSeconds(40));
		sejour.signal("akira", "screen", "tablette", "accueil");
		clock.at(AT_16.plusSeconds(60));
		sejour.signal("akira", "presence", "tablette", null);

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));

		assertThat(row.lines()).extracting(Line::label, Line::durationLabel)
				.containsExactly(tuple("Accueil", "40 s"), tuple("Matières", "20 s"));
		assertThat(row.durationLabel()).isEqualTo("1 min");
	}

	@Test
	void unPassageSousUneSecondeEstAbsent() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusMillis(400));
		sejour.signal("akira", "leave", "tablette", null);

		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();
	}

	@Test
	void uneLigneSousUneSecondeResteHorsDuTotal() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "screen", "tablette", "board");
		clock.at(AT_16.plusMillis(30_400));
		sejour.signal("akira", "leave", "tablette", null);

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(row.lines()).extracting(Line::label, Line::durationLabel).containsExactly(tuple("Accueil", "30 s"));
		assertThat(row.durationLabel()).isEqualTo("30 s");
	}

	@Test
	void unGesteApresDeuxMinutesNeComptePasLeTrou() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(120));
		sejour.signal("akira", "presence", "tablette", null);
		clock.at(AT_16.plusSeconds(180));
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(210));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min 30");
	}

	@Test
	void trenteSecondesSurUneQuestionMontrentLEnonceEtLaMatiere() {
		MemoryQuestions questions = new MemoryQuestions();
		questions.keep(42L, "Combien font 2 + 2 ?", "Maths");
		SejourService jeu = new SejourService(sessions, logins, children, store, clock, questions);

		clock.at(AT_16);
		jeu.signal("akira", "question", "tablette", null, 42L);
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		jeu.signal("akira", "presence", "tablette", null);

		ParentSejour row = seul(jeu.readParent(1L, "parent", 7L));
		assertThat(row.durationLabel()).isEqualTo("30 s");
		assertThat(row.lines()).extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(tuple("Combien font 2 + 2 ?", "Maths", "30 s"));
	}

	@Test
	void quatreMinutesPuisSixSurLaMemeQuestionFontDixMinutes() {
		MemoryQuestions questions = new MemoryQuestions();
		questions.keep(42L, "Combien font 2 + 2 ?", "Maths");
		SejourService jeu = new SejourService(sessions, logins, children, store, clock, questions);

		clock.at(AT_16);
		jeu.signal("akira", "question", "tablette", null, 42L);
		jeu.signal("akira", "gesture", "tablette", null);
		confirmerJusqua(jeu, AT_16.plusSeconds(240));
		clock.at(AT_16.plusSeconds(240));
		jeu.signal("akira", "gesture", "tablette", null);
		jeu.signal("akira", "screen", "tablette", "accueil");
		clock.at(AT_16.plusSeconds(250));
		jeu.signal("akira", "question", "tablette", null, 42L);
		jeu.signal("akira", "gesture", "tablette", null);
		confirmerJusqua(jeu, AT_16.plusSeconds(610));

		ParentSejour row = seul(jeu.readParent(1L, "parent", 7L));
		assertThat(row.lines()).extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(
						tuple("Combien font 2 + 2 ?", "Maths", "10 min"),
						tuple("Accueil", null, "10 s"));
		assertThat(row.durationLabel()).isEqualTo("10 min 10");
	}

	@Test
	void lEnonceCopieResteApresRenommageEtSuppression() {
		MemoryQuestions questions = new MemoryQuestions();
		questions.keep(42L, "Combien font 2 + 2 ?", "Maths");
		SejourService jeu = new SejourService(sessions, logins, children, store, clock, questions);

		clock.at(AT_16);
		jeu.signal("akira", "question", "tablette", null, 42L);
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		jeu.signal("akira", "presence", "tablette", null);

		questions.keep(42L, "Combien font 9 + 9 ?", "Calcul");
		clock.at(AT_16.plusSeconds(60));
		jeu.signal("akira", "presence", "tablette", null);

		assertThat(seul(jeu.readParent(1L, "parent", 7L)).lines())
				.extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(tuple("Combien font 2 + 2 ?", "Maths", "1 min"));

		questions.forget(42L);
		assertThat(seul(jeu.readParent(1L, "parent", 7L)).lines())
				.extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(tuple("Combien font 2 + 2 ?", "Maths", "1 min"));
	}

	@Test
	void lOrdreResteCeluiDeLaPremiereVisite() {
		MemoryQuestions questions = new MemoryQuestions();
		questions.keep(42L, "Combien font 2 + 2 ?", "Maths");
		SejourService jeu = new SejourService(sessions, logins, children, store, clock, questions);

		clock.at(AT_16);
		jeu.signal("akira", "screen", "tablette", "accueil");
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(20));
		jeu.signal("akira", "question", "tablette", null, 42L);
		clock.at(AT_16.plusSeconds(40));
		jeu.signal("akira", "screen", "tablette", "accueil");
		clock.at(AT_16.plusSeconds(60));
		jeu.signal("akira", "presence", "tablette", null);

		assertThat(seul(jeu.readParent(1L, "parent", 7L)).lines())
				.extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(
						tuple("Accueil", null, "40 s"),
						tuple("Combien font 2 + 2 ?", "Maths", "20 s"));
	}

	@Test
	void unPassageQuestionSousUneSecondeNAjoutePasDeLigne() {
		MemoryQuestions questions = new MemoryQuestions();
		questions.keep(42L, "Combien font 2 + 2 ?", "Maths");
		SejourService jeu = new SejourService(sessions, logins, children, store, clock, questions);

		clock.at(AT_16);
		jeu.signal("akira", "screen", "tablette", "accueil");
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		jeu.signal("akira", "question", "tablette", null, 42L);
		clock.at(AT_16.plusMillis(30_400));
		jeu.signal("akira", "leave", "tablette", null);

		ParentSejour row = seul(jeu.readParent(1L, "parent", 7L));
		assertThat(row.lines()).extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(tuple("Accueil", null, "30 s"));
		assertThat(row.durationLabel()).isEqualTo("30 s");
	}

	@Test
	void leBoardEtLaListeQuestionsRestentUneLigne() {
		MemoryQuestions questions = new MemoryQuestions();
		questions.keep(7L, "Combien font 2 + 2 ?", "Maths");
		questions.keep(8L, "Le chat boit", "Français");
		SejourService jeu = new SejourService(sessions, logins, children, store, clock, questions);

		clock.at(AT_16);
		jeu.signal("akira", "screen", "tablette", "board");
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(15));
		jeu.signal("akira", "question", "tablette", null, 7L);
		clock.at(AT_16.plusSeconds(30));
		jeu.signal("akira", "screen", "tablette", "board");
		clock.at(AT_16.plusSeconds(45));
		jeu.signal("akira", "question", "tablette", null, 8L);
		clock.at(AT_16.plusSeconds(60));
		jeu.signal("akira", "screen", "tablette", "questions");
		clock.at(AT_16.plusSeconds(75));
		jeu.signal("akira", "presence", "tablette", null);

		assertThat(seul(jeu.readParent(1L, "parent", 7L)).lines())
				.extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(
						tuple("Board", null, "30 s"),
						tuple("Combien font 2 + 2 ?", "Maths", "15 s"),
						tuple("Le chat boit", "Français", "15 s"),
						tuple("Questions", null, "15 s"));
	}

	@Test
	void unSignalQuestionEstRefuseEtNEcritRien() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);

		assertThatThrownBy(() -> sejour.signal("akira", "question", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.BAD_REQUEST);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("30 s");
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).lines()).hasSize(1);
	}

	@Test
	void unSignalSansAppareilEstRefuseEtNEcritRien() {
		assertThatThrownBy(() -> sejour.signal("akira", "screen", " ", "accueil"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SejourService.DEVICE_REQUIRED);
		assertThatThrownBy(() -> sejour.signal("akira", "gesture", null, null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SejourService.DEVICE_REQUIRED);
		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();
		assertThat(sejour.readChild("akira", "tablette").holds()).isFalse();
		assertThatThrownBy(() -> sejour.readChild("akira", " "))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SejourService.DEVICE_REQUIRED);
	}

	@Test
	void unEcranInconnuEstRefuseEtNEcritRien() {
		assertThatThrownBy(() -> sejour.signal("akira", "screen", "tablette", "salon"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SejourService.SCREEN_UNKNOWN);
		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();
	}

	@Test
	void unJetonAbsentOuAdulteEstRefuseEtNEcritRien() {
		when(sessions.read("")).thenReturn(Optional.empty());
		when(sessions.read("adulte")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> sejour.signal("", "screen", "tablette", "accueil"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginChildService.INVALID_SESSION_MESSAGE);
		assertThatThrownBy(() -> sejour.signal("adulte", "gesture", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginChildService.INVALID_SESSION_MESSAGE);
		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();
	}

	@Test
	void unMauvaisRoleEstRefuseCommeLesAutresRoutesParent() {
		assertThatThrownBy(() -> sejour.readParent(1L, "teacher", 7L))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);
		assertThatThrownBy(() -> sejour.readParent(1L, "child", 7L))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);
	}

	@Test
	void unEnfantInconnuRepondIntrouvable() {
		when(children.findOwned(9L, 7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> sejour.readParent(9L, "parent", 7L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ParentChildrenService.CHILD_UNKNOWN);
	}

	@Test
	void deuxParentsQuiVoientLEnfantLisentLaMemeListe() {
		when(children.findOwned(2L, 7L)).thenReturn(Optional.of(child(7L, 2L, true)));
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(sejour.readParent(2L, "parent", 7L))
				.usingRecursiveComparison()
				.isEqualTo(sejour.readParent(1L, "parent", 7L));
	}

	@Test
	void unProfesseurDeLaClasseLitLeMemeDetailQueLeParent() {
		MemoryQuestions questions = new MemoryQuestions();
		questions.keep(42L, "Combien font 2 + 2 ?", "Maths");
		questions.keep(43L, "Quelle est la capitale ?", "Histoire");
		SejourService jeu = new SejourService(
				sessions, logins, children, store, clock, questions, classesDeAkira());

		clock.at(AT_16);
		jeu.signal("akira", "screen", "tablette", "accueil");
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		jeu.signal("akira", "presence", "tablette", null);
		jeu.signal("akira", "question", "tablette", null, 42L);
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(60));
		jeu.signal("akira", "presence", "tablette", null);
		jeu.signal("akira", "question", "tablette", null, 43L);
		jeu.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(90));
		jeu.signal("akira", "presence", "tablette", null);

		clock.at(paris(2026, 11, 22, 23, 0));
		List<ParentSejour> parent = jeu.readParent(1L, "parent", 7L);
		List<ParentSejour> teacher = jeu.readTeacher(4L, "teacher", 7L);

		assertThat(teacher).usingRecursiveComparison().isEqualTo(parent);
		ParentSejour row = seul(teacher);
		assertThat(row.dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(row.timeLabel()).isEqualTo("16:00");
		assertThat(row.durationLabel()).isEqualTo("1 min 30");
		assertThat(row.lines()).extracting(Line::label, Line::subjectName, Line::durationLabel)
				.containsExactly(
						tuple("Accueil", null, "30 s"),
						tuple("Combien font 2 + 2 ?", "Maths", "30 s"),
						tuple("Quelle est la capitale ?", "Histoire", "30 s"));
		assertThat(jeu.readTeacher(5L, "teacher", 7L))
				.usingRecursiveComparison()
				.isEqualTo(teacher);
		assertThatThrownBy(() -> jeu.readTeacher(9L, "teacher", 7L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherStudentsService.NOT_IN_CLASS)
				.extracting(error -> ((AuthDomainException) error).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);
		assertThatThrownBy(() -> jeu.readTeacher(4L, "parent", 7L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SpaceAccess.DENIED)
				.extracting(error -> ((AuthDomainException) error).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);
		assertThatThrownBy(() -> jeu.readTeacher(4L, "child", 7L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SpaceAccess.DENIED)
				.extracting(error -> ((AuthDomainException) error).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);
	}

	@Test
	void unEnfantDesactiveResteLisible() {
		when(children.findOwned(1L, 7L)).thenReturn(Optional.of(child(7L, 1L, false)));
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("30 s");
	}

	@Test
	void lesSejoursLesPlusRecentsArriventEnPremier() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);
		sejour.signal("akira", "leave", "tablette", null);

		clock.at(AT_16.plusSeconds(3600));
		sejour.signal("akira", "screen", "tablette", "profil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(3640));
		sejour.signal("akira", "presence", "tablette", null);

		List<ParentSejour> rows = sejour.readParent(1L, "parent", 7L);
		assertThat(rows).extracting(ParentSejour::timeLabel, ParentSejour::durationLabel)
				.containsExactly(tuple("17:00", "40 s"), tuple("16:00", "30 s"));
		assertThat(rows).extracting(ParentSejour::dateLabel).containsExactly("22 novembre 2026", "22 novembre 2026");
	}

	@Test
	void chaqueEcranASonLibelleDansLOrdreDeLaPremiereVisite() {
		String[] codes = {"accueil", "matieres", "board", "questions", "recompenses", "classement", "profil"};
		String[] labels = {"Accueil", "Matières", "Board", "Questions", "Récompenses", "Classement", "Profil"};
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", codes[0]);
		sejour.signal("akira", "gesture", "tablette", null);
		for (int i = 1; i < codes.length; i++) {
			clock.at(AT_16.plusSeconds(i * 2L));
			sejour.signal("akira", "screen", "tablette", codes[i]);
		}
		clock.at(AT_16.plusSeconds(codes.length * 2L));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).lines())
				.extracting(Line::label, Line::durationLabel)
				.containsExactly(
						tuple(labels[0], "2 s"),
						tuple(labels[1], "2 s"),
						tuple(labels[2], "2 s"),
						tuple(labels[3], "2 s"),
						tuple(labels[4], "2 s"),
						tuple(labels[5], "2 s"),
						tuple(labels[6], "2 s"));
	}

	@Test
	void quaranteSecondesEtNeufDixiemesSeLisentQuaranteS() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusMillis(40_900));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("40 s");
	}

	@Test
	void quatreVingtDixSecondesSeLisentUneMinuteTrente() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(90));
		sejour.signal("akira", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("1 min 30");
	}

	@Test
	void unAutreEnfantNeRecoitPasCeSejour() {
		when(sessions.read("nao")).thenReturn(Optional.of(new ChildSession(8L)));
		when(logins.findActiveById(8L)).thenReturn(Optional.of(new ChildLoginCandidate(8L, "Nao", "hash")));
		when(children.findOwned(1L, 8L)).thenReturn(Optional.of(child(8L, 1L, true)));
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30));
		sejour.signal("akira", "presence", "tablette", null);
		sejour.signal("nao", "screen", "tablette", "board");
		sejour.signal("nao", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(40));
		sejour.signal("nao", "presence", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).lines())
				.extracting(Line::label, Line::durationLabel)
				.containsExactly(tuple("Accueil", "30 s"));
		assertThat(seul(sejour.readParent(1L, "parent", 8L)).lines())
				.extracting(Line::label, Line::durationLabel)
				.containsExactly(tuple("Board", "10 s"));
	}

	@Test
	void unSejourOuvertA21h59SeFermeA22hEtLAppelSuivantRefuse() {
		clock.at(paris(2026, 11, 22, 21, 59));
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);

		clock.at(paris(2026, 11, 22, 22, 1));
		assertThatThrownBy(() -> sejour.signal("akira", "presence", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");

		clock.at(paris(2026, 11, 22, 23, 0));
		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(row.dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(row.timeLabel()).isEqualTo("21:59");
		assertThat(row.durationLabel()).isEqualTo("1 min");
		assertThat(row.lines()).extracting(Line::label, Line::durationLabel)
				.containsExactly(tuple("Accueil", "1 min"));
	}

	@Test
	void laLectureEnfantA22hRefuseAvecLaPhraseDeLaNuit() {
		clock.at(paris(2026, 11, 22, 21, 59));
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);

		clock.at(paris(2026, 11, 22, 22, 0));
		assertThatThrownBy(() -> sejour.readChild("akira", "tablette"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(row.timeLabel()).isEqualTo("21:59");
		assertThat(row.durationLabel()).isEqualTo("1 min");
	}

	@Test
	void unAppelA5h59FermeA22hSansCompterLaNuit() {
		clock.at(paris(2026, 11, 22, 21, 59));
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);

		clock.at(paris(2026, 11, 23, 5, 59));
		assertThatThrownBy(() -> sejour.signal("akira", "presence", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");
		assertThatThrownBy(() -> sejour.signal("akira", "gesture", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(row.dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(row.timeLabel()).isEqualTo("21:59");
		assertThat(row.durationLabel()).isEqualTo("1 min");
	}

	@Test
	void laNuitNeProlongePasUnEcranSansGeste() {
		clock.at(paris(2026, 11, 22, 21, 50));
		sejour.signal("akira", "screen", "tablette", "accueil");

		clock.at(paris(2026, 11, 22, 22, 1));
		assertThatThrownBy(() -> sejour.signal("akira", "presence", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");

		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();
	}

	@Test
	void uneFermetureRegleeA20hRefuseA20hAvecLaPhraseDe8h() {
		AppHours hours = new AppHours(LocalTime.of(8, 0), LocalTime.of(20, 0));
		SejourService regle = new SejourService(
				sessions, logins, children, new MemorySejours(), clock,
				ignored -> Optional.empty(), null, null, AppHoursPort.fixed(hours));
		clock.at(paris(2026, 11, 22, 19, 59));
		regle.signal("akira", "screen", "tablette", "accueil");
		regle.signal("akira", "gesture", "tablette", null);

		clock.at(paris(2026, 11, 22, 20, 0));
		assertThatThrownBy(() -> regle.signal("akira", "presence", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 8 h.");

		ParentSejour row = seul(regle.readParent(1L, "parent", 7L));
		assertThat(row.timeLabel()).isEqualTo("19:59");
		assertThat(row.durationLabel()).isEqualTo("1 min");
	}

	@Test
	void unSignalA22h00NOuvrePasDeSejour() {
		clock.at(paris(2026, 11, 22, 22, 0));

		assertThatThrownBy(() -> sejour.signal("akira", "gesture", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");
		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();
	}

	@Test
	void laPopupArriveAVingtMinutesEtPasADeux() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);

		clock.at(AT_16.plusSeconds(120));
		ChildSejour tropTot = sejour.readChild("akira", "tablette");
		assertThat(tropTot.holds()).isTrue();
		assertThat(tropTot.message()).isNull();
		assertThat(tropTot.button()).isNull();

		clock.at(AT_16.plusSeconds(20 * 60));
		ChildSejour due = sejour.readChild("akira", "tablette");
		assertThat(due.holds()).isTrue();
		assertThat(due.message()).isEqualTo("Tu es encore là ?");
		assertThat(due.button()).isEqualTo("Je continue");
	}

	@Test
	void unGestePendantLaPopupNeProlongePasLeSejour() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(120));
		sejour.signal("akira", "presence", "tablette", null);

		clock.at(AT_16.plusSeconds(22 * 60));
		sejour.signal("akira", "gesture", "tablette", null);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");
		clock.at(AT_16.plusSeconds(24 * 60));
		ChildSejour encore = sejour.readChild("akira", "tablette");
		assertThat(encore.message()).isEqualTo("Tu es encore là ?");
		assertThat(encore.button()).isEqualTo("Je continue");
	}

	@Test
	void jeContinueASeizeHeuresVingtQuatreGardeLeMemeSejourSansLeTrou() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(120));
		sejour.signal("akira", "presence", "tablette", null);

		clock.at(AT_16.plusSeconds(24 * 60));
		sejour.signal("akira", "reactivate", "tablette", null);

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(row.timeLabel()).isEqualTo("16:00");
		assertThat(row.durationLabel()).isEqualTo("2 min");
		assertThat(sejour.readParent(1L, "parent", 7L)).hasSize(1);

		clock.at(AT_16.plusSeconds(25 * 60));
		sejour.signal("akira", "presence", "tablette", null);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("3 min");

		clock.at(AT_16.plusSeconds(26 * 60));
		ChildSejour apres = sejour.readChild("akira", "tablette");
		assertThat(apres.message()).isNull();
		assertThat(apres.button()).isNull();
	}

	@Test
	void jeContinueAvantLaPopupEstRefuseEtNEcritRien() {
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(120));
		sejour.signal("akira", "presence", "tablette", null);

		clock.at(AT_16.plusSeconds(10 * 60));
		assertThatThrownBy(() -> sejour.signal("akira", "reactivate", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SejourService.SIGNAL_REFUSED);

		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).timeLabel()).isEqualTo("16:00");
		assertThat(sejour.readParent(1L, "parent", 7L)).hasSize(1);

		clock.at(AT_16.plusSeconds(20 * 60));
		ChildSejour due = sejour.readChild("akira", "tablette");
		assertThat(due.message()).isEqualTo("Tu es encore là ?");
		assertThat(due.button()).isEqualTo("Je continue");
	}

	@Test
	void sansBoutonAVingtCinqMinutesLeSejourFinitEtUneNouvelleEntreeEnOuvreUnAutre() {
		when(sessions.issuedAt("akira")).thenReturn(Optional.of(AT_16.minusSeconds(60)));
		clock.at(AT_16);
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(120));
		sejour.signal("akira", "presence", "tablette", null);
		clock.at(AT_16.plusSeconds(22 * 60));
		sejour.signal("akira", "presence", "tablette", null);

		clock.at(AT_16.plusSeconds(25 * 60));
		assertThatThrownBy(() -> sejour.readChild("akira", "tablette"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("Tu as été déconnecté.");

		ParentSejour ferme = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(ferme.timeLabel()).isEqualTo("16:00");
		assertThat(ferme.durationLabel()).isEqualTo("2 min");

		clock.at(AT_16.plusSeconds(26 * 60));
		assertThatThrownBy(() -> sejour.signal("akira", "reactivate", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("Tu as été déconnecté.");
		assertThat(sejour.readParent(1L, "parent", 7L)).hasSize(1);
		assertThat(seul(sejour.readParent(1L, "parent", 7L)).durationLabel()).isEqualTo("2 min");

		when(sessions.issuedAt("akira")).thenReturn(Optional.of(AT_16.plusSeconds(30 * 60)));
		clock.at(AT_16.plusSeconds(30 * 60));
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(AT_16.plusSeconds(30 * 60 + 30));
		sejour.signal("akira", "presence", "tablette", null);

		List<ParentSejour> apres = sejour.readParent(1L, "parent", 7L);
		assertThat(apres).hasSize(2);
		assertThat(apres.get(0).timeLabel()).isEqualTo("16:30");
		assertThat(apres.get(0).durationLabel()).isEqualTo("30 s");
		assertThat(apres.get(1).timeLabel()).isEqualTo("16:00");
		assertThat(apres.get(1).durationLabel()).isEqualTo("2 min");
	}

	@Test
	void laNuitGagneSurLaPopupEncoreOuverte() {
		clock.at(paris(2026, 11, 22, 21, 35));
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);

		clock.at(paris(2026, 11, 22, 21, 55));
		ChildSejour popup = sejour.readChild("akira", "tablette");
		assertThat(popup.message()).isEqualTo("Tu es encore là ?");
		assertThat(popup.button()).isEqualTo("Je continue");

		clock.at(paris(2026, 11, 22, 22, 0));
		assertThatThrownBy(() -> sejour.readChild("akira", "tablette"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");

		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));
		assertThat(row.timeLabel()).isEqualTo("21:35");
		assertThat(row.durationLabel()).isEqualTo("2 min");
	}

	@Test
	void unSignalA6hOuvreANouveau() {
		clock.at(paris(2026, 11, 22, 21, 59));
		sejour.signal("akira", "screen", "tablette", "accueil");
		sejour.signal("akira", "gesture", "tablette", null);
		clock.at(paris(2026, 11, 22, 22, 0));
		assertThatThrownBy(() -> sejour.readChild("akira", "tablette"))
				.hasMessage("L'app est fermée jusqu'à 6 h.");

		clock.at(paris(2026, 11, 23, 6, 0));
		sejour.signal("akira", "screen", "tablette", "accueil");

		assertThat(sejour.readChild("akira", "tablette").holds()).isTrue();
	}

	@Test
	void troisSejoursLeVingtQuatreNovembreFontUneJourneeDeSeptMinutes() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("profil", paris(2026, 11, 24, 9, 19, 0), paris(2026, 11, 24, 9, 19, 3));
		fermer("accueil", paris(2026, 11, 24, 9, 19, 3), paris(2026, 11, 24, 9, 19, 7));
		fermer("matieres", paris(2026, 11, 24, 9, 19, 7), paris(2026, 11, 24, 9, 19, 10));
		fermer("recompenses", paris(2026, 11, 24, 9, 19, 10), paris(2026, 11, 24, 9, 19, 14));
		fermer("classement", paris(2026, 11, 24, 9, 19, 14), paris(2026, 11, 24, 9, 19, 19));
		fermer("accueil", paris(2026, 11, 24, 9, 22, 0), paris(2026, 11, 24, 9, 23, 4));
		fermer("accueil", paris(2026, 11, 24, 9, 31, 0), paris(2026, 11, 24, 9, 36, 37));

		JourneeMonth mois = sejour.readParentMonth(1L, "parent", 7L, 2026, 11);

		assertThat(mois.monthLabel()).isEqualTo("novembre 2026");
		assertThat(mois.currentMonth()).isTrue();
		assertThat(mois.journees()).hasSize(1);
		Journee jour = mois.journees().get(0);
		assertThat(jour.day()).isEqualTo(24);
		assertThat(jour.today()).isTrue();
		assertThat(jour.dateLabel()).isEqualTo("24 novembre 2026");
		assertThat(jour.durationLabel()).isEqualTo("7 min");
		assertThat(jour.durationSeconds()).isEqualTo(420);
		assertThat(jour.lines()).extracting(
				JourneeLine::space, JourneeLine::label, JourneeLine::durationLabel, JourneeLine::durationSeconds)
				.containsExactly(
						tuple("profil", "Profil", "3 s", 3L),
						tuple("accueil", "Accueil", "6 min 45", 405L),
						tuple("matieres", "Matières", "3 s", 3L),
						tuple("recompenses", "Récompenses", "4 s", 4L),
						tuple("classement", "Classement", "5 s", 5L));
	}

	@Test
	void unProfesseurLitLesMemesJourneesSansPlafond() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 0, 40));
		SejourService jeu = new SejourService(
				sessions, logins, children, store, clock, new MemoryQuestions(), classesDeAkira());

		JourneeMonth parent = sejour.readParentMonth(1L, "parent", 7L, 2026, 11);
		JourneeMonth teacher = jeu.readTeacherMonth(4L, "teacher", 7L, 2026, 11);
		assertThat(teacher.journees()).isEqualTo(parent.journees());
		assertThat(teacher.monthLabel()).isEqualTo(parent.monthLabel());
		assertThat(teacher.plafond()).isNull();
		assertThat(parent.plafond().limitMinutes()).isNull();
		assertThat(parent.plafond().message()).isEmpty();
		assertThatThrownBy(() -> jeu.readTeacherMonth(9L, "teacher", 7L, 2026, 11))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("Cet élève n'est pas dans tes classes.");
	}

	@Test
	void deuxVisitesDeLaMemeQuestionFontUneLigne() {
		clock.at(paris(2026, 11, 24, 16, 0));
		question(42L, "Combien font 2 + 2 ?", "Maths", paris(2026, 11, 24, 10, 0, 0), paris(2026, 11, 24, 10, 1, 0));
		question(42L, "Autre énoncé", "Maths", paris(2026, 11, 24, 11, 0, 0), paris(2026, 11, 24, 11, 2, 0));
		question(43L, "Quelle est la capitale ?", "Histoire", paris(2026, 11, 24, 12, 0, 0), paris(2026, 11, 24, 12, 1, 0));

		Journee jour = sejour.readParentMonth(1L, "parent", 7L, 2026, 11).journees().get(0);

		assertThat(jour.lines()).extracting(JourneeLine::space, JourneeLine::label, JourneeLine::durationSeconds)
				.containsExactly(
						tuple("question", "Combien font 2 + 2 ?", 180L),
						tuple("question", "Quelle est la capitale ?", 60L));
	}

	@Test
	void unMoisFuturEstRefuseEtNEcritRien() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 1, 0));

		assertThatThrownBy(() -> sejour.readParentMonth(1L, "parent", 7L, 2026, 12))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("Ce mois n'est pas encore là.");
		assertThat(sejour.readParentMonth(1L, "parent", 7L, 2026, 11).journees()).hasSize(1);
	}

	@Test
	void laNuitNeComptePasMemeAChevalSurMinuit() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 21, 50, 0), paris(2026, 11, 24, 22, 10, 0));
		fermer("accueil", paris(2026, 11, 22, 23, 50, 0), paris(2026, 11, 23, 0, 10, 0));

		JourneeMonth mois = sejour.readParentMonth(1L, "parent", 7L, 2026, 11);

		assertThat(mois.journees()).extracting(Journee::day, Journee::durationLabel)
				.containsExactly(tuple(24, "10 min"));
	}

	@Test
	void sansMoisLaLecturePrendLeMoisDeLHorloge() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 0, 30));

		JourneeMonth mois = sejour.readParentMonth(1L, "parent", 7L, null, null);

		assertThat(mois.year()).isEqualTo(2026);
		assertThat(mois.month()).isEqualTo(11);
		assertThat(mois.currentMonth()).isTrue();
		assertThat(mois.journees()).hasSize(1);
	}

	@Test
	void sansPlafondLaLectureDitLimiteVide() {
		clock.at(paris(2026, 11, 24, 16, 0));

		var plafond = sejour.readParentMonth(1L, "parent", 7L, null, null).plafond();

		assertThat(plafond.limitMinutes()).isNull();
		assertThat(plafond.exceeded()).isFalse();
		assertThat(plafond.message()).isEmpty();
		assertThat(plafond.editable()).isTrue();
	}

	@Test
	void trenteMinutesEtPile1800SecondesNeSontPasDepassees() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 30, 0));

		Plafond plafond = sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30"));

		assertThat(plafond.limitMinutes()).isEqualTo(30);
		assertThat(plafond.exceeded()).isFalse();
		assertThat(plafond.message()).isEmpty();
	}

	@Test
	void uneSecondeAuDessusDitLaPhraseExacte() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 30, 1));

		Plafond plafond = sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30"));

		assertThat(plafond.exceeded()).isTrue();
		assertThat(plafond.message()).isEqualTo("Plafond du jour dépassé.");
	}

	@Test
	void retirerLePlafondEnleveLeMessage() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 30, 1));
		sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30"));

		Plafond plafond = sejour.saveParentPlafond(1L, "parent", 7L, null);

		assertThat(plafond.limitMinutes()).isNull();
		assertThat(plafond.exceeded()).isFalse();
		assertThat(plafond.message()).isEmpty();
	}

	@Test
	void unNombreRefuseLaisseLePlafondDejaGarde() {
		sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30"));

		assertThatThrownBy(() -> sejour.saveParentPlafond(1L, "parent", 7L, BigDecimal.ZERO))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("Le plafond est entre 1 et 960 minutes.");
		assertThatThrownBy(() -> sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("-1")))
				.hasMessage("Le plafond est entre 1 et 960 minutes.");
		assertThatThrownBy(() -> sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30.5")))
				.hasMessage("Le plafond est entre 1 et 960 minutes.");
		assertThatThrownBy(() -> sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("961")))
				.hasMessage("Le plafond est entre 1 et 960 minutes.");
		assertThat(sejour.readParentMonth(1L, "parent", 7L, null, null).plafond().limitMinutes()).isEqualTo(30);
	}

	@Test
	void unEt960MinutesSontAcceptes() {
		assertThat(sejour.saveParentPlafond(1L, "parent", 7L, BigDecimal.ONE).limitMinutes()).isEqualTo(1);
		assertThat(sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("960")).limitMinutes()).isEqualTo(960);
	}

	@Test
	void unMoisPasseGardeLAlerteDAujourdhui() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 30, 1));
		sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30"));

		Plafond plafond = sejour.readParentMonth(1L, "parent", 7L, 2026, 10).plafond();

		assertThat(sejour.readParentMonth(1L, "parent", 7L, 2026, 10).journees()).isEmpty();
		assertThat(plafond.exceeded()).isTrue();
		assertThat(plafond.message()).isEqualTo("Plafond du jour dépassé.");
	}

	@Test
	void monterLePlafondEnleveLAlerteToutDeSuite() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 30, 1));
		assertThat(sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30")).exceeded()).isTrue();

		Plafond plafond = sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("31"));

		assertThat(plafond.exceeded()).isFalse();
		assertThat(plafond.message()).isEmpty();
	}

	@Test
	void laNuitNeComptePasDansLePlafond() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 21, 50, 0), paris(2026, 11, 24, 22, 10, 0));

		Plafond plafond = sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("10"));

		assertThat(plafond.exceeded()).isFalse();
	}

	@Test
	void unParentEnPartageLitLePlafondSansPouvoirLEcrire() {
		when(children.findOwned(2L, 7L)).thenReturn(Optional.of(child(7L, 2L, true)));
		ChildSharePort shares = mock(ChildSharePort.class);
		when(shares.holds(2L, 7L)).thenReturn(true);
		when(shares.isReferent(2L, 7L)).thenReturn(false);
		SejourService partage = new SejourService(
				sessions, logins, children, store, clock, new MemoryQuestions(), null, shares);
		sejour.saveParentPlafond(1L, "parent", 7L, new BigDecimal("30"));

		Plafond lu = partage.readParentMonth(2L, "parent", 7L, null, null).plafond();
		assertThat(lu.limitMinutes()).isEqualTo(30);
		assertThat(lu.editable()).isFalse();
		assertThatThrownBy(() -> partage.saveParentPlafond(2L, "parent", 7L, new BigDecimal("10")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ParentChildrenService.ONLY_REFERENT);
		assertThat(sejour.readParentMonth(1L, "parent", 7L, null, null).plafond().limitMinutes()).isEqualTo(30);
	}

	@Test
	void unEnfantInconnuRefuseAussiLePlafond() {
		when(children.findOwned(9L, 7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> sejour.saveParentPlafond(9L, "parent", 7L, new BigDecimal("30")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ParentChildrenService.CHILD_UNKNOWN);
	}

	@Test
	void unMauvaisRoleNePosePasLePlafond() {
		assertThatThrownBy(() -> sejour.saveParentPlafond(1L, "teacher", 7L, new BigDecimal("30")))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);
	}

	@Test
	void depasserLePlafondNeFermePasLeSejour() {
		clock.at(paris(2026, 11, 24, 16, 0));
		fermer("accueil", paris(2026, 11, 24, 9, 0, 0), paris(2026, 11, 24, 9, 2, 0));
		sejour.saveParentPlafond(1L, "parent", 7L, BigDecimal.ONE);

		sejour.signal("akira", "gesture", "tablette", null);

		assertThat(sejour.readChild("akira", "tablette").holds()).isTrue();
	}

	@Test
	void unIntervalleStockeQuiTraverseMinuitResteUnSeulSejour() {
		Instant start = paris(2026, 11, 22, 23, 50);
		Instant end = paris(2026, 11, 23, 0, 10);
		OpenSlice opened = store.open(7L, start, "tablette", "accueil");
		store.appendInterval(opened.id(), "accueil", null, null, null, start, end);
		store.close(opened.id(), end, "leave");

		clock.at(paris(2026, 11, 23, 16, 0));
		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));

		assertThat(row.dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(row.timeLabel()).isEqualTo("23:50");
		assertThat(row.durationLabel()).isEqualTo("20 min");
		assertThat(row.lines()).extracting(Line::label, Line::durationLabel)
				.containsExactly(tuple("Accueil", "20 min"));
	}

	@Test
	void laNuitNeComptePasDansLesSecondesDuSuivi() {
		fermer("accueil", paris(2026, 11, 22, 21, 50, 0), paris(2026, 11, 22, 22, 10, 0));

		assertThat(sejour.countableSeconds(7L)).containsExactly(600L);
	}

	@Test
	void unSejourEntierementDansLaNuitNeComptePas() {
		fermer("accueil", paris(2026, 11, 22, 23, 50, 0), paris(2026, 11, 23, 0, 10, 0));

		assertThat(sejour.countableSeconds(7L)).isEmpty();
	}

	private static Instant paris(int year, int month, int day, int hour, int minute) {
		return paris(year, month, day, hour, minute, 0);
	}

	private static Instant paris(int year, int month, int day, int hour, int minute, int second) {
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ReadChildProgressService.PROGRESS_ZONE)
				.toInstant();
	}

	private void fermer(String screen, Instant start, Instant end) {
		OpenSlice opened = store.open(7L, start, "tablette", screen);
		store.appendInterval(opened.id(), screen, null, null, null, start, end);
		store.close(opened.id(), end, "leave");
	}

	private void question(long questionId, String prompt, String subject, Instant start, Instant end) {
		OpenSlice opened = store.open(7L, start, "tablette", null);
		store.appendInterval(opened.id(), null, questionId, prompt, subject, start, end);
		store.close(opened.id(), end, "leave");
	}

	/** Présences toutes les deux minutes, avec un geste pour que la lecture continue. */
	private void confirmerJusqua(SejourService jeu, Instant target) {
		while (clock.instant().isBefore(target)) {
			Instant step = clock.instant().plus(java.time.Duration.ofMinutes(2));
			if (step.isAfter(target)) {
				step = target;
			}
			clock.at(step);
			jeu.signal("akira", "presence", "tablette", null);
			if (clock.instant().isBefore(target)) {
				jeu.signal("akira", "gesture", "tablette", null);
			}
		}
	}

	private static ParentSejour seul(List<ParentSejour> rows) {
		assertThat(rows).hasSize(1);
		return rows.get(0);
	}

	private static TeacherStudentsService classesDeAkira() {
		ClassProfiles profiles = new ClassProfiles();
		profiles.account(4L);
		profiles.account(5L);
		profiles.account(9L);
		profiles.school(4L, new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE1")));
		profiles.school(5L, new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE1")));
		profiles.school(9L, new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE2")));
		ClassRoster roster = new ClassRoster();
		roster.add(new EnrolledChild(
				7L, "Akira", "Iris", 8, SchoolCurricula.FRANCE, "CE1", 1L, "cat", false, true));
		return new TeacherStudentsService(roster, profiles, () -> List.of());
	}

	private static ChildSaved child(long id, long parentUserId, boolean active) {
		return new ChildSaved(id, parentUserId, "Akira", "Iris", "F", 8, "FR", "CE1", null, "cat", false, active);
	}

	private static final class MoveableClock extends Clock {
		private Instant instant;

		private MoveableClock(Instant instant) {
			this.instant = instant;
		}

		private void at(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}

	private static final class ClassProfiles implements TeacherProfilePort {

		private final Map<Long, TeacherAccount> accounts = new HashMap<>();
		private final Map<Long, List<TeacherSchoolLink>> schools = new HashMap<>();

		private void account(long userId) {
			accounts.put(userId, new TeacherAccount("prof@example.com", "Martin"));
		}

		private void school(long userId, TeacherSchoolLink link) {
			schools.put(userId, List.of(link));
		}

		@Override
		public Optional<TeacherAccount> findAccount(long userId) {
			return Optional.ofNullable(accounts.get(userId));
		}

		@Override
		public void updateDisplayName(long userId, String displayName) {
			throw new AssertionError("le nom du professeur ne change pas");
		}

		@Override
		public List<TeacherSchoolLink> findSchools(long userId) {
			return schools.getOrDefault(userId, List.of());
		}

		@Override
		public void replaceSchools(long userId, List<TeacherSchoolLink> next) {
			throw new AssertionError("le profil ne change pas depuis cette lecture");
		}

		@Override
		public List<Long> findUserIdsWithClass(long schoolId, String grade) {
			throw new AssertionError("pas de transfert ici");
		}
	}

	private static final class ClassRoster implements EnrolledChildPort {

		private final List<EnrolledChild> children = new ArrayList<>();

		private void add(EnrolledChild child) {
			children.add(child);
		}

		@Override
		public List<EnrolledChild> findActiveInSchools(Collection<Long> schoolIds) {
			return List.copyOf(children);
		}

		@Override
		public Optional<EnrolledChild> findActive(long childId) {
			return children.stream().filter(child -> child.id() == childId).findFirst();
		}

		@Override
		public Optional<EnrolledPhoto> findPhoto(long childId) {
			return Optional.empty();
		}
	}

	/** Énoncé et matière tels qu'ils sont maintenant. */
	private static final class MemoryQuestions implements SejourQuestionPort {
		private final Map<Long, QuestionCaption> rows = new HashMap<>();

		private void keep(long questionId, String prompt, String subjectName) {
			rows.put(questionId, new QuestionCaption(prompt, subjectName));
		}

		private void forget(long questionId) {
			rows.remove(questionId);
		}

		@Override
		public Optional<QuestionCaption> find(long questionId) {
			return Optional.ofNullable(rows.get(questionId));
		}
	}

	private static final class MemorySejours implements SejourPort {
		private final List<Row> rows = new ArrayList<>();
		private long nextId = 1L;

		@Override
		public Optional<OpenSlice> findOpen(long childId) {
			return rows.stream().filter(row -> row.childId == childId && row.open).map(Row::toOpen).findFirst();
		}

		@Override
		public OpenSlice open(long childId, Instant startedAt, String deviceId, String screen) {
			Row row = new Row();
			row.id = nextId++;
			row.childId = childId;
			row.startedAt = startedAt;
			row.open = true;
			row.deviceId = deviceId;
			row.screen = screen;
			row.questionId = null;
			row.prompt = null;
			row.subjectName = null;
			row.sliceStartedAt = startedAt;
			row.lastConfirmedAt = startedAt;
			row.lastGestureAt = null;
			rows.add(row);
			return row.toOpen();
		}

		@Override
		public void saveOpen(OpenSlice slice) {
			Row row = byId(slice.id());
			row.deviceId = slice.deviceId();
			row.screen = slice.screen();
			row.questionId = slice.questionId();
			row.prompt = slice.prompt();
			row.subjectName = slice.subjectName();
			row.sliceStartedAt = slice.sliceStartedAt();
			row.lastConfirmedAt = slice.lastConfirmedAt();
			row.lastGestureAt = slice.lastGestureAt();
		}

		@Override
		public void appendInterval(
				long sejourId,
				String screen,
				Long questionId,
				String prompt,
				String subjectName,
				Instant startedAt,
				Instant endedAt) {
			byId(sejourId).intervals.add(
					new StoredInterval(screen, questionId, prompt, subjectName, startedAt, endedAt));
		}

		@Override
		public Optional<SejourPort.StayEnd> latestEnd(long childId) {
			Row found = null;
			for (Row row : rows) {
				if (row.childId != childId) {
					continue;
				}
				if (found == null || row.startedAt.isAfter(found.startedAt)
						|| (row.startedAt.equals(found.startedAt) && row.id > found.id)) {
					found = row;
				}
			}
			if (found == null) {
				return Optional.empty();
			}
			return Optional.of(new SejourPort.StayEnd(found.open, found.closedAt, found.endedBy));
		}

		@Override
		public void close(long sejourId, Instant closedAt, String endedBy) {
			Row row = byId(sejourId);
			row.open = false;
			row.closedAt = closedAt;
			row.endedBy = endedBy;
			row.deviceId = null;
			row.screen = null;
			row.questionId = null;
			row.prompt = null;
			row.subjectName = null;
			row.sliceStartedAt = null;
			row.lastConfirmedAt = null;
			row.lastGestureAt = null;
		}

		@Override
		public List<StoredSejour> list(long childId) {
			return rows.stream().filter(row -> row.childId == childId).map(Row::toStored).toList();
		}

		private Row byId(long id) {
			return rows.stream().filter(row -> row.id == id).findFirst().orElseThrow();
		}

		private static final class Row {
			private long id;
			private long childId;
			private Instant startedAt;
			private Instant closedAt;
			private String endedBy;
			private boolean open;
			private String deviceId;
			private String screen;
			private Long questionId;
			private String prompt;
			private String subjectName;
			private Instant sliceStartedAt;
			private Instant lastConfirmedAt;
			private Instant lastGestureAt;
			private final List<StoredInterval> intervals = new ArrayList<>();

			private OpenSlice toOpen() {
				return new OpenSlice(
						id, childId, startedAt, deviceId, screen, questionId, prompt, subjectName,
						sliceStartedAt, lastConfirmedAt, lastGestureAt);
			}

			private StoredSejour toStored() {
				return new StoredSejour(
						id,
						childId,
						startedAt,
						open,
						deviceId,
						screen,
						questionId,
						prompt,
						subjectName,
						sliceStartedAt,
						lastConfirmedAt,
						lastGestureAt,
						List.copyOf(intervals),
						closedAt,
						endedBy);
			}
		}
	}
}
