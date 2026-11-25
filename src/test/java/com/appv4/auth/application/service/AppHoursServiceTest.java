package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.AppHoursUseCase.AppHoursView;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AppHoursPort;

class AppHoursServiceTest {

	private static final long ADMIN = 1L;
	private static final long PARENT = 2L;

	private final ActiveRolePort roles = mock(ActiveRolePort.class);
	private final MemoryHours hours = new MemoryHours();
	private AppHoursService service;

	@BeforeEach
	void admin() {
		when(roles.hasAdminRole(ADMIN)).thenReturn(true);
		when(roles.hasAdminRole(PARENT)).thenReturn(false);
		service = new AppHoursService(roles, hours);
	}

	@Test
	void lAdminLitLeDefaut() {
		AppHoursView view = service.read(ADMIN);

		assertThat(view.opensAt()).isEqualTo("06:00");
		assertThat(view.closesAt()).isEqualTo("22:00");
		assertThat(view.closedMessage()).isEqualTo("L'app est fermée jusqu'à 6 h.");
	}

	@Test
	void lAdminEnregistreUneFenetreEtLaPhraseSuitLOuverture() {
		AppHoursView view = service.update(ADMIN, "8:30", "18:00");

		assertThat(view.opensAt()).isEqualTo("08:30");
		assertThat(view.closesAt()).isEqualTo("18:00");
		assertThat(view.closedMessage()).isEqualTo("L'app est fermée jusqu'à 8 h 30.");
		assertThat(service.read(ADMIN).opensAt()).isEqualTo("08:30");
	}

	@Test
	void unParentNeLitPasEtNeChangePas() {
		assertThatThrownBy(() -> service.read(PARENT))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AppHoursService.FORBIDDEN)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.FORBIDDEN);
		assertThatThrownBy(() -> service.update(PARENT, "08:00", "18:00"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AppHoursService.FORBIDDEN);

		assertThat(hours.current().opensAt()).isEqualTo(LocalTime.of(6, 0));
		assertThat(hours.current().closesAt()).isEqualTo(LocalTime.of(22, 0));
	}

	@Test
	void uneHeureIllisibleNeRemplacePasLaFenetre() {
		assertThatThrownBy(() -> service.update(ADMIN, "soir", "18:00"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AppHoursService.OPEN_INVALID)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.BAD_REQUEST);
		assertThatThrownBy(() -> service.update(ADMIN, "08:00", ""))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AppHoursService.CLOSE_INVALID);

		assertThat(hours.current()).isEqualTo(AppHours.defaults());
	}

	@Test
	void leDebutApresLaFinEstRefuse() {
		assertThatThrownBy(() -> service.update(ADMIN, "18:00", "08:00"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AppHoursService.WINDOW);
		assertThatThrownBy(() -> service.update(ADMIN, "08:00", "08:00"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AppHoursService.WINDOW);

		assertThat(hours.current()).isEqualTo(AppHours.defaults());
	}

	private static final class MemoryHours implements AppHoursPort {
		private AppHours hours = AppHours.defaults();

		@Override
		public AppHours current() {
			return hours;
		}

		@Override
		public void save(LocalTime opensAt, LocalTime closesAt) {
			hours = new AppHours(opensAt, closesAt);
		}
	}
}
