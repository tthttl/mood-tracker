package com.moodtracker.api;

import com.jayway.jsonpath.JsonPath;
import com.moodtracker.domain.auth.CodeSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the whole stack (controllers, services, H2, in-memory code store) through HTTP.
 * Only the email delivery is replaced, so the test can read the codes a member would receive.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MoodTrackerApiIntegrationTest.CapturingCodeSenderConfig.class)
class MoodTrackerApiIntegrationTest {

	private static final String A = "a@test.com";
	private static final String B = "b@test.com";
	private static final String C = "c@test.com";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CapturingCodeSender codeSender;

	@BeforeEach
	void clearSentCodes() {
		codeSender.sent.clear();
	}

	@Test
	void fullRoundLifecycleThroughTheApi() throws Exception {
		String groupId = createGroup(2, A, B, C);

		mockMvc.perform(get("/api/v1/groups/{id}/rounds/current", groupId))
				.andExpect(status().isNotFound());

		startRound(groupId, A).andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.pendingMemberEmails.length()").value(3));

		String midRound = mockMvc.perform(get("/api/v1/groups/{id}/rounds/current", groupId))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		submit(groupId, A, -2).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.submittedMemberEmails[0]").value(A))
				.andExpect(jsonPath("$.result").doesNotExist());
		submit(groupId, B, 0).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"));

		String closing = submit(groupId, C, 2).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"))
				.andExpect(jsonPath("$.pendingMemberEmails.length()").value(0))
				.andExpect(jsonPath("$.result.min").value(-2))
				.andExpect(jsonPath("$.result.max").value(2))
				.andExpect(jsonPath("$.result.average").value(0.0))
				.andExpect(jsonPath("$.result.submissionCount").value(3))
				.andReturn().getResponse().getContentAsString();

		mockMvc.perform(get("/api/v1/groups/{id}/rounds/current", groupId))
				.andExpect(status().isNotFound());

		String history = mockMvc.perform(get("/api/v1/groups/{id}/rounds", groupId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rounds.length()").value(1))
				.andExpect(jsonPath("$.rounds[0].min").value(-2))
				.andExpect(jsonPath("$.rounds[0].max").value(2))
				.andExpect(jsonPath("$.rounds[0].average").value(0.0))
				.andReturn().getResponse().getContentAsString();

		// individual submissions are never revealed: no response ever carries a per-member value
		assertThat(List.of(midRound, closing, history)).noneMatch(body -> body.contains("\"value\""));

		// a new round can start once the previous one has closed
		startRound(groupId, B).andExpect(status().isCreated());
	}

	@Test
	void aVerificationCodeIsSingleUse() throws Exception {
		String groupId = createGroup(1, A, B);
		String code = requestCode(groupId, A);

		mockMvc.perform(post("/api/v1/groups/{id}/rounds", groupId).contentType(MediaType.APPLICATION_JSON)
				.content(body(A, code))).andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/groups/{id}/rounds/current/submissions", groupId)
				.contentType(MediaType.APPLICATION_JSON).content(body(A, code, 1)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_VERIFICATION_CODE"));
	}

	@Test
	void aWrongCodeIsRejected() throws Exception {
		String groupId = createGroup(1, A, B);
		requestCode(groupId, A);

		mockMvc.perform(post("/api/v1/groups/{id}/rounds", groupId).contentType(MediaType.APPLICATION_JSON)
				.content(body(A, "000000"))).andExpect(status().isUnauthorized());
	}

	@Test
	void aMemberCannotSubmitTwiceInOneRound() throws Exception {
		String groupId = createGroup(1, A, B);
		startRound(groupId, A);
		submit(groupId, A, 1).andExpect(status().isOk());

		submit(groupId, A, -1).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("DUPLICATE_SUBMISSION"));
	}

	@Test
	void onlyOneRoundCanBeOpenAtATime() throws Exception {
		String groupId = createGroup(1, A, B);
		startRound(groupId, A).andExpect(status().isCreated());

		startRound(groupId, B).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("ROUND_ALREADY_OPEN"));
	}

	@Test
	void aValueOutsideTheGroupsRangeIsRejected() throws Exception {
		String groupId = createGroup(1, A, B);
		startRound(groupId, A);

		submit(groupId, A, 2).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_MOOD_VALUE"));
	}

	@Test
	void noCodeIsSentToAnEmailThatIsNotInTheGroup() throws Exception {
		String groupId = createGroup(1, A, B);

		mockMvc.perform(post("/api/v1/groups/{id}/verification-codes", groupId)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "stranger@test.com"}"""))
				.andExpect(status().isNoContent());

		assertThat(codeSender.sent).isEmpty();
	}

	@Test
	void aCodeForOneGroupDoesNotMakeSomeoneAMemberOfAnother() throws Exception {
		String groupOfA = createGroup(1, A);
		String groupOfB = createGroup(1, B);
		String codeOfA = requestCode(groupOfA, A);

		mockMvc.perform(post("/api/v1/groups/{id}/members", groupOfB).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"requesterEmail": "%s", "code": "%s", "newMemberEmail": "%s"}""".formatted(A, codeOfA, C)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/groups/{id}", groupOfB))
				.andExpect(jsonPath("$.memberEmails.length()").value(1));
	}

	@Test
	void aMemberCanAddAnotherMember() throws Exception {
		String groupId = createGroup(1, A);
		String code = requestCode(groupId, A);

		mockMvc.perform(post("/api/v1/groups/{id}/members", groupId).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"requesterEmail": "%s", "code": "%s", "newMemberEmail": "%s"}""".formatted(A, code, B)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberEmails.length()").value(2));
	}

	@Test
	void unknownGroupsReturn404() throws Exception {
		mockMvc.perform(get("/api/v1/groups/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("GROUP_NOT_FOUND"));
	}

	private String createGroup(int moodRange, String... emails) throws Exception {
		String memberEmails = String.join("\",\"", emails);
		String response = mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Team", "moodRange": %d, "memberEmails": ["%s"]}""".formatted(moodRange, memberEmails)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private String requestCode(String groupId, String email) throws Exception {
		mockMvc.perform(post("/api/v1/groups/{id}/verification-codes", groupId)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "%s"}""".formatted(email)))
				.andExpect(status().isNoContent());
		return codeSender.sent.get(email);
	}

	private ResultActions startRound(String groupId, String email) throws Exception {
		String code = requestCode(groupId, email);
		return mockMvc.perform(post("/api/v1/groups/{id}/rounds", groupId)
				.contentType(MediaType.APPLICATION_JSON).content(body(email, code)));
	}

	private ResultActions submit(String groupId, String email, int value) throws Exception {
		String code = requestCode(groupId, email);
		return mockMvc.perform(post("/api/v1/groups/{id}/rounds/current/submissions", groupId)
				.contentType(MediaType.APPLICATION_JSON).content(body(email, code, value)));
	}

	private static String body(String email, String code) {
		return """
				{"email": "%s", "code": "%s"}""".formatted(email, code);
	}

	private static String body(String email, String code, int value) {
		return """
				{"email": "%s", "code": "%s", "value": %d}""".formatted(email, code, value);
	}

	static class CapturingCodeSender implements CodeSender {

		final Map<String, String> sent = new ConcurrentHashMap<>();

		@Override
		public void send(String email, String code) {
			sent.put(email, code);
		}

	}

	@TestConfiguration
	static class CapturingCodeSenderConfig {

		@Bean
		@Primary
		CapturingCodeSender capturingCodeSender() {
			return new CapturingCodeSender();
		}

	}

}
