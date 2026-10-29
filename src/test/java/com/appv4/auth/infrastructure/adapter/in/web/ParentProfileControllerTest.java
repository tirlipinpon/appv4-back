package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.service.AccountContact;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParentProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Autowired
	private TeacherProfilePort profiles;

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.reset(profiles);
		when(profiles.findAccount(9L))
				.thenReturn(Optional.of(new TeacherAccount("parent@example.com", "Ancien")));
	}

	@Test
	void parentReadsTheSharedAccountAndTheAvatarList() throws Exception {
		mockMvc.perform(get("/api/int/parent/profile").header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("parent@example.com"))
				.andExpect(jsonPath("$.displayName").value("Ancien"))
				.andExpect(jsonPath("$.avatarChoices[0].code").value("owl"))
				.andExpect(jsonPath("$.avatarChoices[0].label").value("Hibou"));
	}

	@Test
	void parentSavesContactAndDoesNotReplaceSchools() throws Exception {
		mockMvc.perform(put("/api/int/parent/profile")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Parent Contact","contactEmail":"contact.parent@example.com",
								 "phone":"+32 470 00 00 00","avatarCode":"owl","email":"pirate@example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("parent@example.com"))
				.andExpect(jsonPath("$.displayName").value("Parent Contact"))
				.andExpect(jsonPath("$.avatarCode").value("owl"));

		verify(profiles).updateContact(
				9L, "Parent Contact", "contact.parent@example.com", "+32 470 00 00 00", "owl");
		verify(profiles, never()).replaceSchools(
				org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void invalidContactIsRefused() throws Exception {
		mockMvc.perform(put("/api/int/parent/profile")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Nom","contactEmail":"pas-un-mail",
								 "phone":"0470000001","avatarCode":"owl"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(AccountContact.EMAIL_INVALID));

		verify(profiles, never()).updateContact(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void teacherAndAnonymousCannotOpenTheParentProfile() throws Exception {
		mockMvc.perform(get("/api/int/parent/profile").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));

		mockMvc.perform(get("/api/int/parent/profile"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(put("/api/int/parent/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Nom\"}"))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "parent@example.com", activeRole, List.of(activeRole), false, 0).token();
		return "Bearer " + raw;
	}
}
