package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;

class ConnectedAccountServiceTest {

	@Test
	void returnsTheNameOfTheConnectedAccount() {
		AuthUserAccountPort accounts = mock(AuthUserAccountPort.class);
		when(accounts.findById(4L)).thenReturn(Optional.of(new AuthUserAccount(4L, "anne@example.com", "Anne", true)));
		ConnectedAccountService service = new ConnectedAccountService(accounts);

		assertThat(service.displayName(4L)).isEqualTo("Anne");
	}

	@Test
	void missingAccountIsNotFound() {
		AuthUserAccountPort accounts = mock(AuthUserAccountPort.class);
		when(accounts.findById(4L)).thenReturn(Optional.empty());
		ConnectedAccountService service = new ConnectedAccountService(accounts);

		assertThatThrownBy(() -> service.displayName(4L))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);
	}
}
