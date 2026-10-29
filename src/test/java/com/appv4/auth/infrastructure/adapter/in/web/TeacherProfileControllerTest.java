package com.appv4.auth.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.application.service.TeacherProfileService;
import com.appv4.auth.application.service.TeacherStudentsService;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Autowired
	private TeacherProfilePort profiles;

	@BeforeEach
	void setUp() {
		reset(profiles);
		when(profiles.findAccount(9L)).thenReturn(Optional.of(new TeacherAccount("prof@example.com", "Ancien nom")));
		when(profiles.findSchools(9L)).thenReturn(List.of());
	}

	@Test
	void teacherReadsEmailNameAndAnEmptySchoolCatalog() throws Exception {
		mockMvc.perform(get("/api/int/teacher/profile").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("prof@example.com"))
				.andExpect(jsonPath("$.displayName").value("Ancien nom"))
				.andExpect(jsonPath("$.schools").isEmpty())
				.andExpect(jsonPath("$.availableSchools").isEmpty())
				.andExpect(jsonPath("$.curricula[0].code").value("FRANCE"))
				.andExpect(jsonPath("$.curricula[0].grades", hasItem("CE1")))
				.andExpect(jsonPath("$.curricula[0].grades", not(hasItem("1re primaire"))))
				.andExpect(jsonPath("$.curricula[1].code").value("BELGIUM_FR"))
				.andExpect(jsonPath("$.curricula[1].grades", hasItem("1re primaire")))
				.andExpect(jsonPath("$.curricula[1].grades", not(hasItem("CP"))));
	}

	@Test
	void teacherSavesTheNameWithoutCreatingASchool() throws Exception {
		mockMvc.perform(put("/api/int/teacher/profile")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"  Professeur Martin  \",\"schools\":[]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Professeur Martin"))
				.andExpect(jsonPath("$.email").value("prof@example.com"))
				.andExpect(jsonPath("$.schools").isEmpty());

		verify(profiles).updateContact(9L, "Professeur Martin", null, null, null);
		verify(profiles).replaceSchools(eq(9L), eq(List.of()));
	}

	@Test
	void blankOrTooLongNameIsRejectedAndNotStored() throws Exception {
		mockMvc.perform(put("/api/int/teacher/profile")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"   \",\"schools\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherProfileService.NAME_REQUIRED));

		mockMvc.perform(put("/api/int/teacher/profile")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"" + "x".repeat(256) + "\",\"schools\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherProfileService.NAME_TOO_LONG));

		verify(profiles, never()).updateDisplayName(anyLong(), anyString());
		verify(profiles, never()).replaceSchools(anyLong(), anyList());
	}

	@Test
	void schoolIsRefusedWhenTheCatalogIsEmpty() throws Exception {
		mockMvc.perform(put("/api/int/teacher/profile")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Professeur Martin","schools":[
								  {"schoolId":1,"curriculum":"FRANCE","grades":["CE1"]}
								]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherProfileService.SCHOOL_UNKNOWN));

		verify(profiles, never()).updateDisplayName(anyLong(), anyString());
	}

	@Test
	void parentPendingAndAnonymousCannotUpdate() throws Exception {
		mockMvc.perform(put("/api/int/teacher/profile")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Parent\",\"schools\":[]}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));

		mockMvc.perform(get("/api/int/teacher/profile").header(HttpHeaders.AUTHORIZATION, bearer(null)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/int/teacher/profile"))
				.andExpect(status().isUnauthorized());

		verify(profiles, never()).updateDisplayName(anyLong(), anyString());
	}

	@Test
	void removalPreviewTellsWhoIsConcernedAndRefusesTheWrongRole() throws Exception {
		mockMvc.perform(get("/api/int/teacher/profile/removal-preview")
						.param("schoolId", "1")
						.param("grade", "CE1")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(TeacherProfileService.SCHOOL_NOT_ON_PROFILE));

		when(profiles.findSchools(9L)).thenReturn(List.of(new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));

		mockMvc.perform(get("/api/int/teacher/profile/removal-preview")
						.param("schoolId", "1")
						.param("grade", "CE1")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schoolId").value(1))
				.andExpect(jsonPath("$.grade").value("CE1"))
				.andExpect(jsonPath("$.studentCount").value(0))
				.andExpect(jsonPath("$.subjectCount").value(0))
				.andExpect(jsonPath("$.studentCountLabel").value("0 élève"))
				.andExpect(jsonPath("$.subjectCountLabel").value("0 matière"))
				.andExpect(jsonPath("$.emptyStudentsLabel").value(TeacherStudentsService.EMPTY_GRADE))
				.andExpect(jsonPath("$.emptySubjectsLabel").value(TeacherProfileService.EMPTY_GRADE_SUBJECTS));

		mockMvc.perform(get("/api/int/teacher/profile/removal-preview")
						.param("schoolId", "1")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));

		mockMvc.perform(get("/api/int/teacher/profile/removal-preview").param("schoolId", "1"))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "prof@example.com", activeRole, List.of("teacher"), false, 0).token();
		return "Bearer " + raw;
	}
}
