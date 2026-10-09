package com.moodtracker.api;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole notification path over HTTP with the SMTP provider switched on, delivering to an
 * in-process SMTP server: verification codes arrive by mail, and starting/closing a round
 * notifies the right members.
 */
@SpringBootTest(properties = { "moodtracker.mail.provider=smtp", "moodtracker.mail.from=noreply@mood.test",
		"moodtracker.frontend-base-url=http://mood.test" })
@AutoConfigureMockMvc
class NotificationIntegrationTest {

	private static final String ANN = "ann@test.com";
	private static final String BOB = "bob@test.com";
	private static final String CY = "cy@test.com";

	// Started once per class: Spring needs the SMTP port when it builds the context, before any test runs.
	@RegisterExtension
	static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
			.withPerMethodLifecycle(false);

	@DynamicPropertySource
	static void smtpServer(DynamicPropertyRegistry registry) {
		registry.add("spring.mail.host", () -> "localhost");
		registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
	}

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void emptyMailboxes() throws Exception {
		GREEN_MAIL.purgeEmailFromAllMailboxes();
	}

	@Test
	void codesArriveByMailAndRoundEventsNotifyTheRightMembers() throws Exception {
		String groupId = createGroup();

		// starting a round: the other members are notified, the starter is not
		String startCode = codeFor(groupId, ANN);
		GREEN_MAIL.purgeEmailFromAllMailboxes();
		mockMvc.perform(post("/api/v1/groups/{id}/rounds", groupId).contentType(MediaType.APPLICATION_JSON)
				.content(body(ANN, startCode))).andExpect(status().isCreated());

		List<MimeMessage> started = awaitMails(2);
		assertThat(recipientsOf(started)).containsExactlyInAnyOrder(BOB, CY);
		assertThat(started).allSatisfy(mail -> {
			assertThat(mail.getSubject()).isEqualTo("[Team] A new mood round has started");
			assertThat(GreenMailUtil.getBody(mail)).contains("http://mood.test/groups/" + groupId);
		});

		submit(groupId, ANN, -1).andExpect(status().isOk());
		submit(groupId, BOB, 0).andExpect(status().isOk());
		String lastCode = codeFor(groupId, CY);
		GREEN_MAIL.purgeEmailFromAllMailboxes();

		// the last submission closes the round: all participants get the aggregate
		mockMvc.perform(post("/api/v1/groups/{id}/rounds/current/submissions", groupId)
				.contentType(MediaType.APPLICATION_JSON).content(body(CY, lastCode, 1)))
				.andExpect(status().isOk());

		List<MimeMessage> closed = awaitMails(3);
		assertThat(recipientsOf(closed)).containsExactlyInAnyOrder(ANN, BOB, CY);
		assertThat(closed).allSatisfy(mail -> {
			assertThat(mail.getSubject()).isEqualTo("[Team] Mood round complete");
			assertThat(GreenMailUtil.getBody(mail)).contains("Lowest: -1").contains("Highest: 1")
					.contains("Average: 0.00").contains("Submissions: 3");
		});
	}

	@Test
	void aRoundThatIsStillOpenSendsNoResultMail() throws Exception {
		String groupId = createGroup();
		String code = codeFor(groupId, ANN);
		mockMvc.perform(post("/api/v1/groups/{id}/rounds", groupId).contentType(MediaType.APPLICATION_JSON)
				.content(body(ANN, code))).andExpect(status().isCreated());
		awaitMails(3); // code mail + 2 start notifications
		GREEN_MAIL.purgeEmailFromAllMailboxes();

		submit(groupId, ANN, 1).andExpect(status().isOk());

		// only the verification code mail for that submission, no "round complete" mail
		assertThat(Arrays.stream(GREEN_MAIL.getReceivedMessages()).map(this::subjectOf))
				.noneMatch(subject -> subject.contains("Mood round complete"));
	}

	private List<MimeMessage> awaitMails(int count) {
		assertThat(GREEN_MAIL.waitForIncomingEmail(5000, count)).as("mails within 5s").isTrue();
		return Arrays.asList(GREEN_MAIL.getReceivedMessages());
	}

	private List<String> recipientsOf(List<MimeMessage> mails) {
		return mails.stream().flatMap(mail -> {
			try {
				return Arrays.stream(mail.getAllRecipients()).map(Address::toString);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}).toList();
	}

	private String subjectOf(MimeMessage mail) {
		try {
			return mail.getSubject();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String createGroup() throws Exception {
		String response = mockMvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Team", "moodRange": 1, "memberEmails": ["%s", "%s", "%s"]}""".formatted(ANN, BOB, CY)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	/** Requests a code and reads it from the mail that arrives. */
	private String codeFor(String groupId, String email) throws Exception {
		GREEN_MAIL.purgeEmailFromAllMailboxes();
		mockMvc.perform(post("/api/v1/groups/{id}/verification-codes", groupId)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email": "%s"}""".formatted(email))).andExpect(status().isNoContent());
		MimeMessage mail = awaitMails(1).get(0);
		assertThat(mail.getAllRecipients()[0].toString()).isEqualTo(email);
		Matcher code = Pattern.compile("\\b(\\d{6})\\b").matcher(GreenMailUtil.getBody(mail));
		assertThat(code.find()).as("6-digit code in mail body").isTrue();
		return code.group(1);
	}

	private ResultActions submit(String groupId, String email, int value) throws Exception {
		String code = codeFor(groupId, email);
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

}
