package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.service.ParentChildrenService;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParentChildrenControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Autowired
	private ChildPort children;

	@BeforeEach
	void setUp() {
		reset(children);
		when(children.findByParent(anyLong())).thenReturn(List.of());
	}

	@Test
	void parentListsAnEmptyPage() throws Exception {
		mockMvc.perform(get("/api/int/parent/children").header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.children").isEmpty())
				.andExpect(jsonPath("$.schools").isEmpty());
	}

	@Test
	void parentAddsAChildAndThePinIsNotInTheResponse() throws Exception {
		when(children.insert(any())).thenReturn(
				new ChildSaved(3L, 9L, "Léa", "Martin", 7, "FRANCE", "CE1", null, "fox", false, true));

		String body = "{\"firstName\":\"Léa\",\"lastName\":\"Martin\",\"age\":\"7\","
				+ "\"country\":\"France\",\"grade\":\"CE1\",\"avatarCode\":\"fox\","
				+ "\"pin\":\"4821\",\"pinConfirmation\":\"4821\"}";
		mockMvc.perform(post("/api/int/parent/children")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Léa"))
				.andExpect(jsonPath("$.grade").value("CE1"))
				.andExpect(jsonPath("$.avatarCode").value("fox"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.pin").doesNotExist())
				.andExpect(jsonPath("$.pinHash").doesNotExist());
	}

	@Test
	void mismatchedPinDoesNotCreateAChild() throws Exception {
		String body = "{\"firstName\":\"Léa\",\"lastName\":\"Martin\",\"age\":\"7\","
				+ "\"country\":\"France\",\"grade\":\"CE1\",\"avatarCode\":\"fox\","
				+ "\"pin\":\"4821\",\"pinConfirmation\":\"4822\"}";
		mockMvc.perform(post("/api/int/parent/children")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(ParentChildrenService.PIN_MISMATCH));

		verify(children, never()).insert(any());
	}

	@Test
	void teacherAndAnonymousCannotOpenTheList() throws Exception {
		mockMvc.perform(get("/api/int/parent/children").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));
		mockMvc.perform(get("/api/int/parent/children"))
				.andExpect(status().isUnauthorized());
		verify(children, never()).insert(any());
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "parent@example.com", activeRole, List.of(activeRole), false, 0).token();
		return "Bearer " + raw;
	}
}
