package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.RecoveryPromptUseCase.RecoveryPrompt;
import com.appv4.auth.application.port.out.RecoveryPromptPort;
import com.appv4.auth.application.port.out.RecoveryPromptPort.RecoveryPromptState;

class RecoveryPromptServiceTest {

	@Test
	void missingPhoneAsksOnceWithTheSentence() {
		Memory memory = new Memory(new RecoveryPromptState("Anne", null, "anne@example.com", false));
		RecoveryPromptService service = new RecoveryPromptService(memory);

		RecoveryPrompt prompt = service.read(7L);

		assertThat(prompt.ask()).isTrue();
		assertThat(prompt.recoveryPrompt()).isEqualTo(RecoveryPromptService.PROMPT);
	}

	@Test
	void bothFieldsPresentDoesNotAsk() {
		Memory memory = new Memory(new RecoveryPromptState("Anne", "0470000000", "anne@example.com", false));

		RecoveryPrompt prompt = new RecoveryPromptService(memory).read(7L);

		assertThat(prompt.ask()).isFalse();
		assertThat(prompt.recoveryPrompt()).isNull();
	}

	@Test
	void skipKeepsTheContactAndDoesNotAskAgain() {
		Memory memory = new Memory(new RecoveryPromptState("Anne", null, null, false));
		RecoveryPromptService service = new RecoveryPromptService(memory);

		service.answer(7L, true, null, null, null);

		assertThat(memory.state.displayName()).isEqualTo("Anne");
		assertThat(memory.state.phone()).isNull();
		assertThat(memory.state.contactEmail()).isNull();
		assertThat(service.read(7L).ask()).isFalse();
		assertThat(service.read(7L).recoveryPrompt()).isNull();
	}

	@Test
	void rejectedPhoneDoesNotCountAsAsked() {
		Memory memory = new Memory(new RecoveryPromptState("Anne", null, null, false));
		RecoveryPromptService service = new RecoveryPromptService(memory);

		assertThatThrownBy(() -> service.answer(7L, false, "Anne", "12", "anne@example.com"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.PHONE_INVALID);
		assertThat(service.read(7L).ask()).isTrue();
	}

	@Test
	void saveStoresTheContactAndDoesNotAskAgain() {
		Memory memory = new Memory(new RecoveryPromptState("Anne", null, null, false));
		RecoveryPromptService service = new RecoveryPromptService(memory);

		service.answer(7L, false, " Anne ", "0470123456", "anne@example.com");

		assertThat(memory.state.displayName()).isEqualTo("Anne");
		assertThat(memory.state.phone()).isEqualTo("0470123456");
		assertThat(memory.state.contactEmail()).isEqualTo("anne@example.com");
		assertThat(service.read(7L).ask()).isFalse();
	}

	@Test
	void clearingThePhoneStillCountsAsAsked() {
		Memory memory = new Memory(new RecoveryPromptState("Anne", "0470000000", "anne@example.com", false));
		RecoveryPromptService service = new RecoveryPromptService(memory);

		service.answer(7L, false, "Anne", "", "anne@example.com");

		assertThat(memory.state.phone()).isNull();
		assertThat(memory.state.contactEmail()).isEqualTo("anne@example.com");
		assertThat(service.read(7L).ask()).isFalse();
	}

	private static final class Memory implements RecoveryPromptPort {
		private RecoveryPromptState state;

		private Memory(RecoveryPromptState state) {
			this.state = state;
		}

		@Override
		public Optional<RecoveryPromptState> find(long userId) {
			return Optional.of(state);
		}

		@Override
		public void markAsked(long userId) {
			state = new RecoveryPromptState(state.displayName(), state.phone(), state.contactEmail(), true);
		}

		@Override
		public void saveAndAsk(long userId, String displayName, String phone, String contactEmail) {
			state = new RecoveryPromptState(displayName, phone, contactEmail, true);
		}
	}
}
