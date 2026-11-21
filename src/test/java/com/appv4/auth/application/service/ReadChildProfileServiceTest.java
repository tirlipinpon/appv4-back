package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildPhotoBytes;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildProfile;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.ChildProfileView;

@ExtendWith(MockitoExtension.class)
class ReadChildProfileServiceTest {

	@Mock
	private ChildLoginPort childLoginPort;

	@Mock
	private ChildSessionTokenPort childSessionTokenPort;

	private ReadChildProfileService service;

	@BeforeEach
	void setUp() {
		service = new ReadChildProfileService(childLoginPort, childSessionTokenPort);
	}

	@Test
	void profileReturnsActiveFiche() {
		when(childSessionTokenPort.read("token"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(7L)));
		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "Léa", "fox", false, 8)));

		ChildProfileView view = service.profile("token");

		assertThat(view.childId()).isEqualTo(7L);
		assertThat(view.firstName()).isEqualTo("Léa");
		assertThat(view.avatarCode()).isEqualTo("fox");
		assertThat(view.hasPhoto()).isFalse();
	}

	@Test
	void profileRefusesAdultOrExpiredToken() {
		when(childSessionTokenPort.read("bad")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.profile("bad"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildProfileService.INVALID_SESSION_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.UNAUTHORIZED);
	}

	@Test
	void profileRefusesStoppedFiche() {
		when(childSessionTokenPort.read("token"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(7L)));
		when(childLoginPort.findActiveProfileById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.profile("token"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildProfileService.INVALID_SESSION_MESSAGE);
	}

	@Test
	void photoReturnsBytesWhenPresent() {
		when(childSessionTokenPort.read("token"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(7L)));
		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "Léa", null, true, 8)));
		when(childLoginPort.findActivePhotoById(7L))
				.thenReturn(Optional.of(new ChildPhotoBytes(new byte[] {1, 2}, "image/png")));

		ChildPhotoBytes photo = service.photo("token");

		assertThat(photo.contentType()).isEqualTo("image/png");
		assertThat(photo.bytes()).containsExactly(1, 2);
	}

	@Test
	void photoMissingIsNotFound() {
		when(childSessionTokenPort.read("token"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(7L)));
		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "Léa", "fox", false, 8)));
		when(childLoginPort.findActivePhotoById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.photo("token"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ReadChildProfileService.PHOTO_MISSING_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.NOT_FOUND);
	}
}
