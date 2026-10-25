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
import com.appv4.auth.application.port.out.ChildPort.ChildLinkView;
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

	@Test
	void parentCreatesABrotherAndBothCardsShareThePhrase() throws Exception {
		ChildSaved lea = new ChildSaved(3L, 9L, "Léa", "Martin", 7, "FRANCE", "CE1", null, "fox", false, true);
		ChildSaved noah = new ChildSaved(4L, 9L, "Noah", "Martin", 8, "FRANCE", "CM1", null, "fox", false, true);
		when(children.findOwned(9L, 3L)).thenReturn(Optional.of(lea));
		when(children.insert(any())).thenReturn(noah);
		when(children.findLinks(9L)).thenReturn(List.of(new ChildLinkView(4L, "Noah", 3L, "Léa", "FRERE")));

		String body = "{\"firstName\":\"Noah\",\"lastName\":\"Martin\",\"age\":\"8\","
				+ "\"country\":\"France\",\"grade\":\"CM1\",\"avatarCode\":\"fox\","
				+ "\"pin\":\"1357\",\"pinConfirmation\":\"1357\",\"fromChildId\":3,\"link\":\"FRERE\"}";
		mockMvc.perform(post("/api/int/parent/children")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Noah"))
				.andExpect(jsonPath("$.linkPhrases[0]").value("Noah est le frère de Léa"))
				.andExpect(jsonPath("$.links[0].otherChildId").value(3))
				.andExpect(jsonPath("$.links[0].label").value("Frère"))
				.andExpect(jsonPath("$.pin").doesNotExist())
				.andExpect(jsonPath("$.pinHash").doesNotExist());
		verify(children).insertLink(9L, 4L, 3L, "FRERE");

		when(children.findByParent(9L)).thenReturn(List.of(lea, noah));
		mockMvc.perform(get("/api/int/parent/children").header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.children[0].linkPhrases[0]").value("Noah est le frère de Léa"))
				.andExpect(jsonPath("$.children[1].linkPhrases[0]").value("Noah est le frère de Léa"))
				.andExpect(jsonPath("$.children[0].links[0].otherChildId").value(4))
				.andExpect(jsonPath("$.children[0].links[0].label").value("Frère"))
				.andExpect(jsonPath("$.children[1].links[0].otherChildId").value(3))
				.andExpect(jsonPath("$.children[1].links[0].label").value("Frère"));
	}

	@Test
	void noneLinkAndUnknownSourceCreateNoRelationship() throws Exception {
		when(children.findOwned(9L, 3L)).thenReturn(Optional.of(
				new ChildSaved(3L, 9L, "Léa", "Martin", 7, "FRANCE", "CE1", null, "fox", false, true)));
		when(children.insert(any())).thenReturn(
				new ChildSaved(5L, 9L, "Tom", "Martin", 7, "FRANCE", "CE1", null, "fox", false, true));

		String none = "{\"firstName\":\"Tom\",\"lastName\":\"Martin\",\"age\":\"7\","
				+ "\"country\":\"France\",\"grade\":\"CE1\",\"avatarCode\":\"fox\","
				+ "\"pin\":\"2468\",\"pinConfirmation\":\"2468\",\"fromChildId\":3,\"link\":\"AUCUN\"}";
		mockMvc.perform(post("/api/int/parent/children")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(none))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.linkPhrases").isEmpty())
				.andExpect(jsonPath("$.links").isEmpty());
		verify(children, never()).insertLink(anyLong(), anyLong(), anyLong(), any());

		String foreign = "{\"firstName\":\"Nina\",\"lastName\":\"Martin\",\"age\":\"7\","
				+ "\"country\":\"France\",\"grade\":\"CE1\",\"avatarCode\":\"fox\","
				+ "\"pin\":\"8642\",\"pinConfirmation\":\"8642\",\"fromChildId\":99,\"link\":\"SOEUR\"}";
		mockMvc.perform(post("/api/int/parent/children")
						.header(HttpHeaders.AUTHORIZATION, bearer("parent"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(foreign))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(ParentChildrenService.CHILD_UNKNOWN));
		verify(children).insert(any());
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "parent@example.com", activeRole, List.of(activeRole), false, 0).token();
		return "Bearer " + raw;
	}
}
