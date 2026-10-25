package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildSubjectControllerTest {

	private static final ChildSaved LEA = new ChildSaved(
			3L, 9L, "Léa", "Martin", 7, "FRANCE", "CE1", 1L, "fox", false, true);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Autowired
	private ChildPort children;

	@Autowired
	private ChildSubjectPort subjects;

	@BeforeEach
	void setUp() {
		reset(children, subjects);
		when(children.findByParent(9L)).thenReturn(List.of(LEA));
		when(children.findOwned(9L, 3L)).thenReturn(Optional.of(LEA));
		when(subjects.visibleSubjects()).thenReturn(List.of(
				new CatalogSubject(10L, "Français", "Prof A", 1L, "CE1"),
				new CatalogSubject(12L, "Mathématiques", "Prof A", 1L, "CE2")));
		when(subjects.linksOf(3L)).thenReturn(List.of(new StoredLink(10L, false)));
	}

	@Test
	void parentSeesTheChildWithClassSubjectsAlreadyChosen() throws Exception {
		mockMvc.perform(get("/api/int/parent/subjects").header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.children[0].firstName").value("Léa"))
				.andExpect(jsonPath("$.children[0].chosen[0].name").value("Français"))
				.andExpect(jsonPath("$.children[0].chosen[0].scolaire").value(true))
				.andExpect(jsonPath("$.children[0].proposed[0].name").value("Mathématiques"))
				.andExpect(jsonPath("$.children[0].proposed[0].scolaire").value(false));
	}

	@Test
	void addAnExtraCourseAndRemoveAClassSubject() throws Exception {
		mockMvc.perform(post("/api/int/parent/subjects")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"childId\":3,\"subjectId\":12}"))
				.andExpect(status().isOk());
		verify(subjects).save(3L, 12L, false);

		mockMvc.perform(delete("/api/int/parent/subjects/3/10")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isOk());
		verify(subjects).save(3L, 10L, true);
	}

	@Test
	void teacherCannotOpenThePage() throws Exception {
		mockMvc.perform(get("/api/int/parent/subjects").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "parent@example.com", activeRole, List.of(activeRole), false, 0).token();
		return "Bearer " + raw;
	}
}
