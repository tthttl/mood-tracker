package com.moodtracker.api;

import com.moodtracker.application.AuthService;
import com.moodtracker.application.RoundService;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.Submission;
import com.moodtracker.domain.auth.exception.InvalidVerificationCodeException;
import com.moodtracker.domain.exception.DuplicateSubmissionException;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.exception.InvalidMoodValueException;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundAlreadyOpenException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoundsController.class)
class RoundsControllerTest {

	private static final Instant STARTED = Instant.parse("2026-09-01T10:00:00Z");
	private static final Instant CLOSED = Instant.parse("2026-09-01T10:05:00Z");
	private static final String START_BODY = """
			{"email": "a@test.com", "code": "123456"}""";
	private static final String SUBMIT_BODY = """
			{"email": "a@test.com", "code": "123456", "value": 1}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RoundService roundService;

	@MockitoBean
	private AuthService authService;

	private static Round openRound() {
		return Round.of("r-1", "g-1", new MoodRange(1), Set.of("a@test.com", "b@test.com"), "a@test.com",
				STARTED, null, List.of(new Submission("b@test.com", 1, STARTED)));
	}

	private static Round closedRound() {
		return Round.of("r-1", "g-1", new MoodRange(1), Set.of("a@test.com", "b@test.com"), "a@test.com",
				STARTED, CLOSED,
				List.of(new Submission("a@test.com", -1, STARTED), new Submission("b@test.com", 1, CLOSED)));
	}

	@Test
	void startRoundVerifiesTheCodeThenStartsTheRound() throws Exception {
		when(roundService.startRound("g-1", "a@test.com")).thenReturn(openRound());

		mockMvc.perform(post("/api/v1/groups/g-1/rounds").contentType(MediaType.APPLICATION_JSON)
				.content(START_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/v1/groups/g-1/rounds/current")))
				.andExpect(jsonPath("$.roundId").value("r-1"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.startedBy").value("a@test.com"));

		InOrder inOrder = inOrder(authService, roundService);
		inOrder.verify(authService).verifyCode("a@test.com", "123456");
		inOrder.verify(roundService).startRound("g-1", "a@test.com");
	}

	@Test
	void startRoundWithABadCodeReturns401AndNeverTouchesTheRoundService() throws Exception {
		doThrow(new InvalidVerificationCodeException()).when(authService).verifyCode(any(), any());

		mockMvc.perform(post("/api/v1/groups/g-1/rounds").contentType(MediaType.APPLICATION_JSON)
				.content(START_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_VERIFICATION_CODE"));

		verifyNoInteractions(roundService);
	}

	@Test
	void startRoundByANonMemberReturns403() throws Exception {
		when(roundService.startRound(any(), any())).thenThrow(new NotAGroupMemberException("a@test.com"));

		mockMvc.perform(post("/api/v1/groups/g-1/rounds").contentType(MediaType.APPLICATION_JSON)
				.content(START_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("NOT_A_GROUP_MEMBER"));
	}

	@Test
	void startRoundWhileOneIsOpenReturns409() throws Exception {
		when(roundService.startRound(any(), any())).thenThrow(new RoundAlreadyOpenException("g-1"));

		mockMvc.perform(post("/api/v1/groups/g-1/rounds").contentType(MediaType.APPLICATION_JSON)
				.content(START_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("ROUND_ALREADY_OPEN"));
	}

	@Test
	void getCurrentRoundShowsWhoSubmittedAndWhoIsPending() throws Exception {
		when(roundService.currentRound("g-1")).thenReturn(Optional.of(openRound()));

		mockMvc.perform(get("/api/v1/groups/g-1/rounds/current"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.submittedMemberEmails[0]").value("b@test.com"))
				.andExpect(jsonPath("$.pendingMemberEmails[0]").value("a@test.com"))
				.andExpect(content().string(not(containsString("\"result\""))))
				.andExpect(content().string(not(containsString("\"closedAt\""))));
	}

	@Test
	void getCurrentRoundReturns404WhenNoRoundIsOpen() throws Exception {
		when(roundService.currentRound("g-1")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/groups/g-1/rounds/current"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NO_OPEN_ROUND"));
	}

	@Test
	void submitMoodVerifiesTheCodeThenSubmits() throws Exception {
		when(roundService.submitMood("g-1", "a@test.com", 1)).thenReturn(openRound());

		mockMvc.perform(post("/api/v1/groups/g-1/rounds/current/submissions")
				.contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.result").doesNotExist());

		InOrder inOrder = inOrder(authService, roundService);
		inOrder.verify(authService).verifyCode("a@test.com", "123456");
		inOrder.verify(roundService).submitMood("g-1", "a@test.com", 1);
	}

	@Test
	void theSubmissionThatClosesTheRoundReturnsTheResult() throws Exception {
		when(roundService.submitMood("g-1", "a@test.com", 1)).thenReturn(closedRound());

		mockMvc.perform(post("/api/v1/groups/g-1/rounds/current/submissions")
				.contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"))
				.andExpect(jsonPath("$.closedAt").value("2026-09-01T10:05:00Z"))
				.andExpect(jsonPath("$.result.min").value(-1))
				.andExpect(jsonPath("$.result.max").value(1))
				.andExpect(jsonPath("$.result.average").value(0.0))
				.andExpect(jsonPath("$.result.submissionCount").value(2))
				// only aggregates: no individual value anywhere in the payload
				.andExpect(content().string(not(containsString("\"value\""))));
	}

	@Test
	void submitMoodWithABadCodeReturns401AndNeverTouchesTheRoundService() throws Exception {
		doThrow(new InvalidVerificationCodeException()).when(authService).verifyCode(any(), any());

		mockMvc.perform(post("/api/v1/groups/g-1/rounds/current/submissions")
				.contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(roundService);
	}

	@Test
	void submitMoodTwiceReturns409() throws Exception {
		when(roundService.submitMood(any(), any(), anyInt()))
				.thenThrow(new DuplicateSubmissionException("r-1", "a@test.com"));

		mockMvc.perform(post("/api/v1/groups/g-1/rounds/current/submissions")
				.contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("DUPLICATE_SUBMISSION"));
	}

	@Test
	void submitMoodOutsideTheGroupsRangeReturns400() throws Exception {
		when(roundService.submitMood(any(), any(), anyInt())).thenThrow(new InvalidMoodValueException(5, 1));

		mockMvc.perform(post("/api/v1/groups/g-1/rounds/current/submissions")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "a@test.com", "code": "123456", "value": 5}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_MOOD_VALUE"));
	}

	@Test
	void submitMoodRejectsAMissingValue() throws Exception {
		mockMvc.perform(post("/api/v1/groups/g-1/rounds/current/submissions")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "a@test.com", "code": "123456"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'value')]").exists());

		verifyNoInteractions(authService, roundService);
	}

	@Test
	void historyListsTheAggregatesOfClosedRounds() throws Exception {
		when(roundService.history("g-1")).thenReturn(List.of(new RoundResult("r-1", -1, 1, 0.0, 2, CLOSED)));

		mockMvc.perform(get("/api/v1/groups/g-1/rounds"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rounds.length()").value(1))
				.andExpect(jsonPath("$.rounds[0].roundId").value("r-1"))
				.andExpect(jsonPath("$.rounds[0].average").value(0.0));
	}

	@Test
	void historyOfAnUnknownGroupReturns404() throws Exception {
		when(roundService.history("missing")).thenThrow(new GroupNotFoundException("missing"));

		mockMvc.perform(get("/api/v1/groups/missing/rounds"))
				.andExpect(status().isNotFound());
	}

	@Test
	void unexpectedFailuresReturnAGeneric500WithoutLeakingDetails() throws Exception {
		when(roundService.history("g-1")).thenThrow(new IllegalStateException("db password is hunter2"));

		mockMvc.perform(get("/api/v1/groups/g-1/rounds"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
				.andExpect(content().string(not(containsString("hunter2"))));
	}

	@Test
	void unsupportedMethodsKeepTheirStatusButUseTheErrorBodyShape() throws Exception {
		mockMvc.perform(delete("/api/v1/groups/g-1/rounds"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
	}

}
