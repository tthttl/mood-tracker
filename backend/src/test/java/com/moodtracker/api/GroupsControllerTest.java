package com.moodtracker.api;

import com.moodtracker.application.AuthService;
import com.moodtracker.application.GroupService;
import com.moodtracker.domain.Email;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.Member;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.auth.exception.InvalidVerificationCodeException;
import com.moodtracker.domain.exception.DuplicateMemberException;
import com.moodtracker.domain.exception.GroupNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupsController.class)
class GroupsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GroupService groupService;

	@MockitoBean
	private AuthService authService;

	private static Group group() {
		return Group.of("g-1", new Name("Team A"), new MoodRange(2),
				List.of(new Member(new Email("a@test.com")), new Member(new Email("b@test.com"))), List.of());
	}

	@Test
	void createGroupReturns201WithLocationAndBody() throws Exception {
		when(groupService.createGroup("Team A", 2, List.of("a@test.com", "b@test.com"))).thenReturn(group());

		mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Team A", "moodRange": 2, "memberEmails": ["a@test.com", "b@test.com"]}"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/v1/groups/g-1")))
				.andExpect(jsonPath("$.id").value("g-1"))
				.andExpect(jsonPath("$.name").value("Team A"))
				.andExpect(jsonPath("$.moodRange").value(2))
				.andExpect(jsonPath("$.memberEmails[0]").value("a@test.com"));
	}

	@Test
	void createGroupDefaultsMoodRangeToOne() throws Exception {
		when(groupService.createGroup(anyString(), anyInt(), anyList())).thenReturn(group());

		mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Team A", "memberEmails": ["a@test.com"]}"""))
				.andExpect(status().isCreated());

		verify(groupService).createGroup("Team A", 1, List.of("a@test.com"));
	}

	@Test
	void createGroupRejectsInvalidBodyWithFieldErrors() throws Exception {
		mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "  ", "moodRange": 0, "memberEmails": []}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'moodRange')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'memberEmails')]").exists());

		verifyNoInteractions(groupService);
	}

	@Test
	void createGroupRejectsAnInvalidMemberEmail() throws Exception {
		mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Team A", "memberEmails": ["not-an-email"]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

		verifyNoInteractions(groupService);
	}

	@Test
	void createGroupRejectsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
	}

	@Test
	void createGroupWithDuplicateMembersReturns409() throws Exception {
		when(groupService.createGroup(anyString(), anyInt(), anyList()))
				.thenThrow(new DuplicateMemberException("a@test.com"));

		mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Team A", "memberEmails": ["a@test.com", "a@test.com"]}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("DUPLICATE_MEMBER"));
	}

	@Test
	void getGroupReturnsTheGroup() throws Exception {
		when(groupService.getGroup("g-1")).thenReturn(group());

		mockMvc.perform(get("/api/v1/groups/g-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("g-1"))
				.andExpect(jsonPath("$.memberEmails.length()").value(2));
	}

	@Test
	void getUnknownGroupReturns404() throws Exception {
		when(groupService.getGroup("missing")).thenThrow(new GroupNotFoundException("missing"));

		mockMvc.perform(get("/api/v1/groups/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("GROUP_NOT_FOUND"));
	}

	@Test
	void addMemberVerifiesTheRequestersCodeThenAddsTheMember() throws Exception {
		when(groupService.getGroup("g-1")).thenReturn(group());
		when(groupService.addMember("g-1", "c@test.com")).thenReturn(group());

		mockMvc.perform(post("/api/v1/groups/g-1/members").contentType(MediaType.APPLICATION_JSON).content("""
				{"requesterEmail": "a@test.com", "code": "123456", "newMemberEmail": "c@test.com"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("g-1"));

		InOrder inOrder = inOrder(authService, groupService);
		inOrder.verify(authService).verifyCode("a@test.com", "123456");
		inOrder.verify(groupService).addMember("g-1", "c@test.com");
	}

	@Test
	void addMemberWithABadCodeReturns401AndAddsNobody() throws Exception {
		when(groupService.getGroup("g-1")).thenReturn(group());
		doThrow(new InvalidVerificationCodeException()).when(authService).verifyCode(any(), any());

		mockMvc.perform(post("/api/v1/groups/g-1/members").contentType(MediaType.APPLICATION_JSON).content("""
				{"requesterEmail": "a@test.com", "code": "123456", "newMemberEmail": "c@test.com"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_VERIFICATION_CODE"));

		verify(groupService, never()).addMember(any(), any());
	}

	@Test
	void addMemberByAnEmailThatIsNotInTheGroupReturns403() throws Exception {
		when(groupService.getGroup("g-1")).thenReturn(group());

		mockMvc.perform(post("/api/v1/groups/g-1/members").contentType(MediaType.APPLICATION_JSON).content("""
				{"requesterEmail": "stranger@test.com", "code": "123456", "newMemberEmail": "c@test.com"}"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("NOT_A_GROUP_MEMBER"));

		verify(groupService, never()).addMember(any(), any());
	}

	@Test
	void addMemberRejectsACodeThatIsNotSixDigits() throws Exception {
		mockMvc.perform(post("/api/v1/groups/g-1/members").contentType(MediaType.APPLICATION_JSON).content("""
				{"requesterEmail": "a@test.com", "code": "12", "newMemberEmail": "c@test.com"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'code')]").exists());

		verifyNoInteractions(authService);
	}

}
