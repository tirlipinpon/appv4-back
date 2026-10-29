package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.application.service.TeacherSubjectService;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherSubjectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Autowired
	private TeacherProfilePort profiles;

	@Autowired
	private TeacherSubjectPort subjects;

	@BeforeEach
	void setUp() {
		reset(profiles, subjects);
		when(profiles.findAccount(9L)).thenReturn(Optional.of(new TeacherAccount("prof@example.com", "Martin")));
		when(profiles.findSchools(9L)).thenReturn(List.of());
		when(subjects.findByUser(9L)).thenReturn(List.of());
		when(subjects.findVisibleInClass(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of());
		when(subjects.hasAccess(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
				.thenReturn(true);
		when(subjects.otherAccessHolders(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
				.thenReturn(List.of());
		when(subjects.otherAccessNames(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
				.thenReturn(List.of());
	}

	@Test
	void teacherWithoutASchoolSeesAnEmptyList() throws Exception {
		mockMvc.perform(get("/api/int/teacher/subjects").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schools").isEmpty());
	}

	@Test
	void listGroupsGradesAndSortsNames() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE2", "CE1"))));
		when(subjects.findByUser(9L)).thenReturn(List.of(
				new StoredSubject(2L, 9L, 1L, "CE1", "Mathématiques", true),
				new StoredSubject(1L, 9L, 1L, "CE1", "Français", true)));

		mockMvc.perform(get("/api/int/teacher/subjects").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schools[0].schoolId").value(1))
				.andExpect(jsonPath("$.schools[0].grades[0].grade").value("CE1"))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].name").value("Français"))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].active").value(true))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].followedByMe").value(true))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].owner.displayName").value("Martin"))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].owner.userId").value(9))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[1].name").value("Mathématiques"))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[1].followedByMe").value(true))
				.andExpect(jsonPath("$.schools[0].grades[1].grade").value("CE2"))
				.andExpect(jsonPath("$.schools[0].grades[1].subjects").isEmpty());
	}

	@Test
	void listShowsASubjectAlreadyFollowedByAColleague() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.findByUser(9L)).thenReturn(List.of());
		when(subjects.findVisibleInClass(1L, "CE1")).thenReturn(List.of(
				new StoredSubject(4L, 10L, 1L, "CE1", "Mathématiques", true)));
		when(subjects.hasAccess(4L, 9L)).thenReturn(false);
		when(subjects.otherAccessHolders(4L, 9L)).thenReturn(List.of(
				new TeacherSubjectPort.AccessHolder(10L, "Camille")));

		mockMvc.perform(get("/api/int/teacher/subjects").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].name").value("Mathématiques"))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].followedByMe").value(false))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].ownedByMe").value(false))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].sharedWith[0].displayName").value("Camille"));
	}

	@Test
	void listNamesThePeopleAlreadyAskedToShare() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.findByUser(9L)).thenReturn(List.of());
		when(subjects.findVisibleInClass(1L, "CE1")).thenReturn(List.of(
				new StoredSubject(4L, 10L, 1L, "CE1", "Mathématiques", true)));
		when(subjects.hasAccess(4L, 9L)).thenReturn(false);
		when(subjects.otherAccessHolders(4L, 9L)).thenReturn(List.of(
				new TeacherSubjectPort.AccessHolder(10L, "Camille")));
		when(subjects.findPendingAsksFrom(4L, 9L)).thenReturn(List.of(
				new TeacherSubjectPort.StoredTransferRequest(8L, 4L, 9L, 10L, "ask", "pending")));
		when(profiles.findAccount(10L)).thenReturn(Optional.of(new TeacherAccount("camille@example.com", "Camille")));

		mockMvc.perform(get("/api/int/teacher/subjects").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].shareAskedTo[0].displayName").value("Camille"))
				.andExpect(jsonPath("$.schools[0].grades[0].subjects[0].shareAskedTo[0].userId").value(10));
	}

	@Test
	void createTrimsTheName() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.nameTaken(1L, "CE1", "mathématiques", null)).thenReturn(false);
		when(subjects.insert(9L, 1L, "CE1", "Mathématiques", "mathématiques"))
				.thenReturn(new StoredSubject(5L, 9L, 1L, "CE1", "Mathématiques", true));

		mockMvc.perform(post("/api/int/teacher/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"schoolId":1,"grade":"CE1","name":"  Mathématiques  "}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.name").value("Mathématiques"))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void blankTooLongOrTakenNameIsRefused() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.nameTaken(1L, "CE1", "lecture", null)).thenReturn(true);

		mockMvc.perform(post("/api/int/teacher/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.NAME_REQUIRED));

		mockMvc.perform(post("/api/int/teacher/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"" + "x".repeat(101) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.NAME_TOO_LONG));

		mockMvc.perform(post("/api/int/teacher/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"lecture\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.NAME_TAKEN));

		verify(subjects, never()).insert(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void removedNameOffersRestoreThenBringsTheSameSubjectBack() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.findRemovedByName(1L, "CE1", "mathématiques"))
				.thenReturn(Optional.of(new StoredSubject(5L, 9L, 1L, "CE1", "Mathématiques", false)));
		when(subjects.nameTaken(1L, "CE1", "mathématiques", 5L)).thenReturn(false);

		mockMvc.perform(post("/api/int/teacher/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"Mathématiques\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.REMOVED_RESTORABLE));

		when(subjects.restore(9L, 5L))
				.thenReturn(Optional.of(new StoredSubject(5L, 9L, 1L, "CE1", "Mathématiques", true)));

		mockMvc.perform(post("/api/int/teacher/subjects/restore")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"Mathématiques\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.name").value("Mathématiques"))
				.andExpect(jsonPath("$.active").value(true));

		verify(subjects, never()).insert(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void takenNameOfAnotherTeacherOffersAShareRequest() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.nameTaken(1L, "CE1", "mathématiques", null)).thenReturn(true);
		when(subjects.findVisibleByName(1L, "CE1", "mathématiques"))
				.thenReturn(Optional.of(new StoredSubject(5L, 4L, 1L, "CE1", "Mathématiques", true)));
		when(subjects.hasAccess(5L, 9L)).thenReturn(false);
		when(subjects.hasAccess(5L, 4L)).thenReturn(true);
		when(profiles.findAccount(4L)).thenReturn(Optional.of(new TeacherAccount("anne@example.com", "Anne")));

		mockMvc.perform(post("/api/int/teacher/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"Mathématiques\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.nameCanAsk("Anne")));

		mockMvc.perform(post("/api/int/teacher/subjects/share-requests")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"Mathématiques\"}"))
				.andExpect(status().isNoContent());

		verify(subjects).insertTransferRequest(5L, 9L, 4L, TeacherSubjectService.MODE_ASK);
	}

	@Test
	void classMissingFromTheProfileIsRefused() throws Exception {
		mockMvc.perform(post("/api/int/teacher/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"schoolId\":1,\"grade\":\"CE1\",\"name\":\"Français\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.CLASS_NOT_ON_PROFILE));

		verify(subjects, never()).insert(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void renameAndDeleteStayOnTheOwnedSubject() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.findOwned(9L, 4L)).thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Français", true)));
		when(subjects.nameTaken(1L, "CE1", "lecture", 4L)).thenReturn(false);
		when(subjects.rename(9L, 4L, "Lecture", "lecture"))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Lecture", true)));
		when(subjects.hide(9L, 4L)).thenReturn(true);

		mockMvc.perform(put("/api/int/teacher/subjects/4")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Lecture\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Lecture"));

		mockMvc.perform(delete("/api/int/teacher/subjects/4")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isNoContent());
	}

	@Test
	void sharedColleagueCannotRename() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 3L, 1L, "CE1", "Français", true)));

		mockMvc.perform(put("/api/int/teacher/subjects/4")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Lecture\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.RENAME_ONLY_CREATOR));

		verify(subjects, never()).rename(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void unknownSubjectIsNotFound() throws Exception {
		when(subjects.findOwned(9L, 4L)).thenReturn(Optional.empty());
		when(subjects.hide(9L, 4L)).thenReturn(false);

		mockMvc.perform(put("/api/int/teacher/subjects/4")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Lecture\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.SUBJECT_MISSING));

		mockMvc.perform(delete("/api/int/teacher/subjects/4")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deactivateLeavesTheSubjectForTheTeacher() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Français", true)));
		when(subjects.accessUserIds(4L)).thenReturn(List.of(9L));
		when(subjects.deactivateAccesses(
						org.mockito.ArgumentMatchers.eq(9L),
						org.mockito.ArgumentMatchers.eq(4L),
						org.mockito.ArgumentMatchers.anyList()))
				.thenReturn(true);

		mockMvc.perform(post("/api/int/teacher/subjects/4/deactivate")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"alsoUserIds\":[]}"))
				.andExpect(status().isNoContent());

		verify(subjects).deactivateAccesses(
				org.mockito.ArgumentMatchers.eq(9L),
				org.mockito.ArgumentMatchers.eq(4L),
				org.mockito.ArgumentMatchers.anyList());
		verify(subjects, never()).hide(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void parentAndAnonymousCannotOpenSubjects() throws Exception {
		mockMvc.perform(get("/api/int/teacher/subjects").header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));

		mockMvc.perform(get("/api/int/teacher/subjects"))
				.andExpect(status().isUnauthorized());

		verify(subjects, never()).findByUser(9L);
	}

	@Test
	void transferRequiresColleagueAndMode() throws Exception {
		when(profiles.findSchools(9L)).thenReturn(List.of(
				new TeacherSchoolLink(1L, "FRANCE", List.of("CE1"))));
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));

		mockMvc.perform(post("/api/int/teacher/subjects/4/transfer")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"colleagueUserId\":null,\"mode\":\"share\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.COLLEAGUE_REQUIRED));

		mockMvc.perform(post("/api/int/teacher/subjects/4/transfer")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"colleagueUserId\":10,\"mode\":\"clone\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.MODE_REQUIRED));
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "prof@example.com", activeRole, List.of("teacher"), false, 0).token();
		return "Bearer " + raw;
	}
}
