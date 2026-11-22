package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.RecoverIdentifierUseCase.IdentifierRecovery;
import com.appv4.auth.application.port.out.RecoveryAccountPort;
import com.appv4.auth.application.port.out.RecoveryAccountPort.RecoveryCompte;

class RecoverIdentifierServiceTest {

	@Test
	void oneActiveCompteReturnsTheLoginMaskAndWritesNothing() {
		List<RecoveryCompte> comptes = new ArrayList<>();
		comptes.add(new RecoveryCompte(
				7L,
				"marie@ecole.be",
				"elodie martin",
				"+32 470 12 34 56",
				"Marie@ecole.be",
				true));
		RecoveryAccountPort port = () -> comptes;
		RecoverIdentifierService service = new RecoverIdentifierService(port);

		IdentifierRecovery result = service.recover("Elodi Martin", "0470 12 34 56", "marue@ecole.be");

		assertThat(result.maskedEmail()).isEqualTo("ma***@ecole.be");
		assertThat(result.message()).isNull();
		assertThat(comptes).containsExactly(new RecoveryCompte(
				7L,
				"marie@ecole.be",
				"elodie martin",
				"+32 470 12 34 56",
				"Marie@ecole.be",
				true));
	}

	@Test
	void sameWordsInAnotherOrderStillReturnTheMask() {
		IdentifierRecovery result = service(compte(
				"jean@ecole.be", "elodie martin", "+32 470 12 34 56", "marie@ecole.be", true))
				.recover("Martin Elodie", "0470 12 34 56", "marie@ecole.be");

		assertThat(result.maskedEmail()).isEqualTo("je**@ecole.be");
		assertThat(result.message()).isNull();
	}

	@Test
	void hyphenMatchesTheSameName() {
		IdentifierRecovery result = service(compte(
				"jean@ecole.be", "jean pierre", "0470123456", "marie@ecole.be", true))
				.recover("Jean-Pierre", "0470123456", "marie@ecole.be");

		assertThat(result.maskedEmail()).isEqualTo("je**@ecole.be");
	}

	@Test
	void frenchPrefixMatchesTheNationalNumber() {
		IdentifierRecovery result = service(compte(
				"a@ecole.be", "Elodie Martin", "+33 6 12 34 56 78", "marie@ecole.be", true))
				.recover("Élodie Martin", "06 12 34 56 78", "marie@ecole.be");

		assertThat(result.maskedEmail()).isEqualTo("*@ecole.be");
	}

	@Test
	void twoCharacterLocalPartIsOnlyStars() {
		IdentifierRecovery result = service(compte(
				"ab@ecole.be", "Elodie Martin", "0470123456", "marie@ecole.be", true))
				.recover("Elodie Martin", "0470123456", "marie@ecole.be");

		assertThat(result.maskedEmail()).isEqualTo("**@ecole.be");
	}

	@Test
	void missingWordDoesNotMatch() {
		IdentifierRecovery result = service(elodie(true))
				.recover("Marie", "0470 12 34 56", "marie@ecole.be");

		assertThat(result.maskedEmail()).isNull();
		assertThat(result.message()).isEqualTo(RecoverIdentifierService.MISS);
	}

	@Test
	void twoWrongDigitsDoNotMatch() {
		IdentifierRecovery result = service(elodie(true))
				.recover("Elodie Martin", "0470 12 34 99", "marie@ecole.be");

		assertThat(result.message()).isEqualTo(RecoverIdentifierService.MISS);
		assertThat(result.maskedEmail()).isNull();
	}

	@Test
	void anotherDomainDoesNotMatch() {
		IdentifierRecovery result = service(elodie(true))
				.recover("Elodie Martin", "0470 12 34 56", "marie@gmail.com");

		assertThat(result.message()).isEqualTo(RecoverIdentifierService.MISS);
	}

	@Test
	void compteWithoutPhoneDoesNotMatch() {
		IdentifierRecovery result = service(compte(
				"marie@ecole.be", "elodie martin", null, "marie@ecole.be", true))
				.recover("Elodie Martin", "0470 12 34 56", "marie@ecole.be");

		assertThat(result.message()).isEqualTo(RecoverIdentifierService.MISS);
	}

	@Test
	void oneInactiveCompteSaysSoWithoutAMask() {
		IdentifierRecovery result = service(elodie(false))
				.recover("Elodie Martin", "0470 12 34 56", "marie@ecole.be");

		assertThat(result.message()).isEqualTo(RecoverIdentifierService.INACTIVE);
		assertThat(result.maskedEmail()).isNull();
	}

	@Test
	void activeAndInactiveTogetherUseTheMissSentence() {
		RecoverIdentifierService service = new RecoverIdentifierService(
				() -> List.of(elodie(true), elodie(false)));

		IdentifierRecovery result = service.recover("Elodie Martin", "0470 12 34 56", "marie@ecole.be");

		assertThat(result.message()).isEqualTo(RecoverIdentifierService.MISS);
		assertThat(result.maskedEmail()).isNull();
	}

	@Test
	void noCompteUsesTheMissSentence() {
		IdentifierRecovery result = new RecoverIdentifierService(List::of)
				.recover("Elodie Martin", "0470 12 34 56", "marie@ecole.be");

		assertThat(result.message()).isEqualTo(RecoverIdentifierService.MISS);
	}

	@Test
	void blankNameIsRefusedBeforeTheSearch() {
		List<RecoveryCompte> comptes = new ArrayList<>();
		comptes.add(elodie(true));

		assertThatThrownBy(() -> new RecoverIdentifierService(() -> comptes)
				.recover("  ", "0470123456", "marie@ecole.be"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.NAME_REQUIRED);
		assertThat(comptes).containsExactly(elodie(true));
	}

	@Test
	void rejectedPhoneIsRefusedBeforeTheSearch() {
		assertThatThrownBy(() -> service(elodie(true)).recover("Elodie", "12", "marie@ecole.be"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.PHONE_INVALID);
	}

	private static RecoverIdentifierService service(RecoveryCompte compte) {
		return new RecoverIdentifierService(() -> List.of(compte));
	}

	private static RecoveryCompte elodie(boolean active) {
		return compte("marie@ecole.be", "elodie martin", "+32 470 12 34 56", "Marie@ecole.be", active);
	}

	private static RecoveryCompte compte(
			String loginEmail,
			String displayName,
			String phone,
			String contactEmail,
			boolean active) {
		return new RecoveryCompte(7L, loginEmail, displayName, phone, contactEmail, active);
	}
}
