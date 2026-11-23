package com.appv4.auth.application.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;

@ExtendWith(MockitoExtension.class)
class LocalAdminSeedTest {

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ActiveRolePort activeRolePort;

	private LocalAdminSeed seed;

	@BeforeEach
	void setUp() {
		seed = new LocalAdminSeed(authUserAccountPort, passwordEncoder, activeRolePort);
	}

	@Test
	void blankPasswordDoesNothing() {
		seed.ensure("tony-ster@hotmail.com", "  ");

		verify(authUserAccountPort, never()).createWithPassword(anyString(), anyString(), anyString());
		verify(activeRolePort, never()).grantAdmin(org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void createsTheAccountAndGrantsAdmin() {
		when(passwordEncoder.encode("secret")).thenReturn("hash");
		when(authUserAccountPort.findByEmail("tony-ster@hotmail.com")).thenReturn(Optional.empty());
		when(authUserAccountPort.createWithPassword("tony-ster@hotmail.com", LocalAdminSeed.DISPLAY_NAME, "hash"))
				.thenReturn(new AuthUserAccount(3L, "tony-ster@hotmail.com", "Admin", true));

		seed.ensure(" Tony-Ster@hotmail.com ", "secret");

		verify(authUserAccountPort).updatePasswordHash(3L, "hash");
		verify(activeRolePort).grantAdmin(3L);
	}

	@Test
	void existingAccountKeepsTheSameIdAndReceivesAdmin() {
		when(passwordEncoder.encode("secret")).thenReturn("hash");
		when(authUserAccountPort.findByEmail("tony-ster@hotmail.com"))
				.thenReturn(Optional.of(new AuthUserAccount(3L, "tony-ster@hotmail.com", "Admin", true)));

		seed.ensure("tony-ster@hotmail.com", "secret");

		verify(authUserAccountPort).updatePasswordHash(3L, "hash");
		verify(authUserAccountPort, never()).createWithPassword(anyString(), anyString(), anyString());
		verify(activeRolePort).grantAdmin(eq(3L));
	}
}
