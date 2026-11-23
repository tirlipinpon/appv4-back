package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.SejourUseCase.ChildSejour;
import com.appv4.auth.application.port.in.SejourUseCase.Line;
import com.appv4.auth.application.port.in.SejourUseCase.ParentSejour;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.SejourPort;
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
	private SejourService sejour;

	@BeforeEach
	void akira() {
		when(sessions.read("akira")).thenReturn(Optional.of(new ChildSession(7L)));
		when(logins.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Akira", "hash")));
		when(children.findOwned(1L, 7L)).thenReturn(Optional.of(child(7L, 1L, true)));
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
	void unSignalA22h00NOuvrePasDeSejour() {
		clock.at(paris(2026, 11, 22, 22, 0));

		assertThatThrownBy(() -> sejour.signal("akira", "gesture", "tablette", null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage("L'app est fermée jusqu'à 6 h.");
		assertThat(sejour.readParent(1L, "parent", 7L)).isEmpty();
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
	void unIntervalleStockeQuiTraverseMinuitResteUnSeulSejour() {
		Instant start = paris(2026, 11, 22, 23, 50);
		Instant end = paris(2026, 11, 23, 0, 10);
		OpenSlice opened = store.open(7L, start, "tablette", "accueil");
		store.appendInterval(opened.id(), "accueil", null, null, null, start, end);
		store.close(opened.id(), end);

		clock.at(paris(2026, 11, 23, 16, 0));
		ParentSejour row = seul(sejour.readParent(1L, "parent", 7L));

		assertThat(row.dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(row.timeLabel()).isEqualTo("23:50");
		assertThat(row.durationLabel()).isEqualTo("20 min");
		assertThat(row.lines()).extracting(Line::label, Line::durationLabel)
				.containsExactly(tuple("Accueil", "20 min"));
	}

	private static Instant paris(int year, int month, int day, int hour, int minute) {
		return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ReadChildProgressService.PROGRESS_ZONE)
				.toInstant();
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
		public void close(long sejourId, Instant closedAt) {
			Row row = byId(sejourId);
			row.open = false;
			row.closedAt = closedAt;
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
						List.copyOf(intervals));
			}
		}
	}
}
