package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
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
import org.springframework.test.web.servlet.MvcResult;

import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledChild;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledPhoto;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.application.service.TeacherStudentsService;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherStudentsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Autowired
	private TeacherProfilePort profiles;

	@Autowired
	private EnrolledChildPort roster;

	@BeforeEach
	void setUp() {
		reset(profiles, roster);
		when(profiles.findAccount(9L)).thenReturn(Optional.of(new TeacherAccount("prof@example.com", "Martin")));
		when(profiles.findSchools(9L)).thenReturn(List.of());
	}

	@Test
	void teacherWithoutASchoolSeesTheEmptySentence() throws Exception {
		mockMvc.perform(get("/api/int/teacher/students").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schools").isEmpty())
				.andExpect(jsonPath("$.noClassMessage").value(TeacherStudentsService.NO_CLASS));

		verify(roster, never()).findActiveInSchools(any());
	}

	@Test
	void listGroupsTheMatchingChildrenAndHidesThePin() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE2", "CE1"))));
		when(roster.findActiveInSchools(anyCollection())).thenReturn(List.of(
				child(2, "Tom", "Martin", "CE1", 1L),
				child(1, "Léa", "Martin", "CE1", 1L),
				child(3, "Noah", "Martin", "CM1", 1L)));

		MvcResult result = mockMvc.perform(get("/api/int/teacher/students")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schools[0].schoolId").value(1))
				.andExpect(jsonPath("$.schools[0].grades[0].grade").value("CE1"))
				.andExpect(jsonPath("$.schools[0].grades[0].countLabel").value("2 élèves"))
				.andExpect(jsonPath("$.schools[0].grades[0].students[0].firstName").value("Léa"))
				.andExpect(jsonPath("$.schools[0].grades[0].students[0].lastName").value("Martin"))
				.andExpect(jsonPath("$.schools[0].grades[0].students[0].age").value(7))
				.andExpect(jsonPath("$.schools[0].grades[0].students[0].hasPhoto").value(false))
				.andExpect(jsonPath("$.schools[0].grades[0].students[1].firstName").value("Tom"))
				.andExpect(jsonPath("$.schools[0].grades[1].grade").value("CE2"))
				.andExpect(jsonPath("$.schools[0].grades[1].countLabel").value("0 élève"))
				.andExpect(jsonPath("$.schools[0].grades[1].emptyMessage")
						.value(TeacherStudentsService.EMPTY_GRADE))
				.andExpect(jsonPath("$.schools[0].grades[1].students").isEmpty())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(body)
				.doesNotContain("Noah")
				.doesNotContain("pin")
				.doesNotContain("password")
				.doesNotContain("prof@example.com")
				.doesNotContain("parentUserId");
	}

	@Test
	void photoOfAStudentInClassIsTheImageOnly() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(roster.findActive(1L)).thenReturn(Optional.of(child(1, "Léa", "Martin", "CE1", 1L)));
		when(roster.findPhoto(1L)).thenReturn(Optional.of(new EnrolledPhoto(new byte[] {9, 8}, "image/png")));

		mockMvc.perform(get("/api/int/teacher/students/1/photo")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(content().bytes(new byte[] {9, 8}));
	}

	@Test
	void photoOutsideTheClassIsNotFound() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(roster.findActive(4L)).thenReturn(Optional.of(child(4, "Emma", "Durand", "CE1", 2L)));

		mockMvc.perform(get("/api/int/teacher/students/4/photo")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(TeacherStudentsService.NOT_IN_CLASS));

		verify(roster, never()).findPhoto(4L);
	}

	@Test
	void parentAndAnonymousCannotListStudents() throws Exception {
		mockMvc.perform(get("/api/int/teacher/students").header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));

		mockMvc.perform(get("/api/int/teacher/students"))
				.andExpect(status().isUnauthorized());

		verify(roster, never()).findActiveInSchools(any());
	}

	private static EnrolledChild child(long id, String firstName, String lastName, String grade, long schoolId) {
		return new EnrolledChild(id, firstName, lastName, 7, "FRANCE", grade, schoolId, "owl", false, true);
	}

	@SuppressWarnings("unchecked")
	private static Collection<Long> anyCollection() {
		return any(Collection.class);
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "prof@example.com", activeRole, List.of("teacher"), false, 0).token();
		return "Bearer " + raw;
	}
}
