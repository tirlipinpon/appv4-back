package com.appv4.auth.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.appv4.auth.application.port.out.LocalLoginProfilesPort;

class LocalLoginProfilesControllerTest {

	@Test
	void listsProfilesWithoutSecretsAndWithoutCaching() throws Exception {
		var port = mock(LocalLoginProfilesPort.class);
		when(port.list()).thenReturn(List.of(new LocalLoginProfilesPort.Profile(1, "demo@example.test", "Demo", "parent", true)));
		MockMvcBuilders.standaloneSetup(new LocalLoginProfilesController(port)).build()
				.perform(get("/api/int/auth/dev-profiles"))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$[0].email").value("demo@example.test"))
				.andExpect(jsonPath("$[0].password").doesNotExist())
				.andExpect(jsonPath("$[0].passwordHash").doesNotExist());
	}

	@Test
	void onlyLocalProfileRegistersTheControllerEvenWithMixedProfiles() {
		for (String[] profiles : List.of(new String[]{"local"}, new String[]{"production"}, new String[]{"prod"},
				new String[]{"test"}, new String[]{"local", "production"}, new String[]{"local", "prod"}, new String[]{})) {
			try (var context = new AnnotationConfigApplicationContext()) {
				context.getEnvironment().setActiveProfiles(profiles);
				context.registerBean(LocalLoginProfilesPort.class, () -> mock(LocalLoginProfilesPort.class));
				context.register(LocalLoginProfilesController.class);
				context.refresh();
				assertThat(context.getBeansOfType(LocalLoginProfilesController.class).size())
						.isEqualTo(profiles.length == 1 && profiles[0].equals("local") ? 1 : 0);
			}
		}
	}
}
