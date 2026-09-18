package com.moodtracker.api;

import com.moodtracker.application.AuthService;
import com.moodtracker.application.GroupService;
import com.moodtracker.domain.Email;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.Member;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.exception.GroupNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GroupService groupService;

	@MockitoBean
	private AuthService authService;

	private static Group group() {
		return Group.of("g-1", new Name("Team A"), new MoodRange(1),
				List.of(new Member(new Email("a@test.com"))), List.of());
	}

	@Test
	void sendsACodeToAMemberOfTheGroup() throws Exception {
		when(groupService.getGroup("g-1")).thenReturn(group());

		mockMvc.perform(post("/api/v1/groups/g-1/verification-codes")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "a@test.com"}"""))
				.andExpect(status().isNoContent());

		verify(authService).requestCode("a@test.com");
	}

	@Test
	void answers204ButSendsNothingToANonMember() throws Exception {
		when(groupService.getGroup("g-1")).thenReturn(group());

		mockMvc.perform(post("/api/v1/groups/g-1/verification-codes")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "stranger@test.com"}"""))
				.andExpect(status().isNoContent());

		verifyNoInteractions(authService);
	}

	@Test
	void unknownGroupReturns404() throws Exception {
		when(groupService.getGroup("missing")).thenThrow(new GroupNotFoundException("missing"));

		mockMvc.perform(post("/api/v1/groups/missing/verification-codes")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "a@test.com"}"""))
				.andExpect(status().isNotFound());

		verifyNoInteractions(authService);
	}

	@Test
	void rejectsAnInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/v1/groups/g-1/verification-codes")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "nope"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());

		verifyNoInteractions(groupService, authService);
	}

}
