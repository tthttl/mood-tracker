package com.moodtracker.domain.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTemplateTest {

	private static final String TEXT = "Subject: Hello {name}\n\nDear {name},\nyour code is {code}.\n";

	@Test
	void rendersSubjectAndBodyWithTheGivenValues() {
		Message message = MessageTemplate.parse(TEXT).render(Map.of("name", "Ann", "code", "123456"));

		assertThat(message.subject()).isEqualTo("Hello Ann");
		assertThat(message.body()).isEqualTo("Dear Ann,\nyour code is 123456.\n");
	}

	@Test
	void listsEveryPlaceholderOnce() {
		assertThat(MessageTemplate.parse(TEXT).placeholders()).containsExactlyInAnyOrder("name", "code");
	}

	@Test
	void bodyLineBreaksAndWindowsLineEndingsInTheFileAreHandled() {
		MessageTemplate template = MessageTemplate.parse("Subject: S\r\n\r\nline1\r\nline2\r\n");

		assertThat(template.render(Map.of()).body()).isEqualTo("line1\r\nline2\r\n");
	}

	@Test
	void aValueThatLooksLikeAPlaceholderIsNotSubstitutedAgain() {
		Message message = MessageTemplate.parse(TEXT).render(Map.of("name", "{code}", "code", "123456"));

		assertThat(message.subject()).isEqualTo("Hello {code}");
		assertThat(message.body()).contains("Dear {code},");
	}

	@Test
	void regexSpecialCharactersInValuesAreKeptLiterally() {
		Message message = MessageTemplate.parse(TEXT).render(Map.of("name", "$1 \\ (a|b)", "code", "1"));

		assertThat(message.subject()).isEqualTo("Hello $1 \\ (a|b)");
	}

	@Test
	void lineBreaksInSubjectValuesCannotInjectMailHeaders() {
		Message message = MessageTemplate.parse(TEXT)
				.render(Map.of("name", "Ann\r\nBcc: attacker@evil.com", "code", "1"));

		assertThat(message.subject()).isEqualTo("Hello Ann Bcc: attacker@evil.com");
	}

	@Test
	void bodyValuesKeepTheirLineBreaks() {
		Message message = MessageTemplate.parse(TEXT).render(Map.of("name", "Ann\nBob", "code", "1"));

		assertThat(message.body()).startsWith("Dear Ann\nBob,");
	}

	@Test
	void aMissingValueFailsInsteadOfSendingALiteralPlaceholder() {
		assertThatThrownBy(() -> MessageTemplate.parse(TEXT).render(Map.of("name", "Ann")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("code");
	}

	@Test
	void anUnknownValueFailsSoTyposInTheCallingCodeAreCaught() {
		assertThatThrownBy(() -> MessageTemplate.parse(TEXT).render(Map.of("name", "Ann", "code", "1", "extra", "x")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("extra");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"no subject line\n\nbody",
			"Subject: only subject",
			"Subject: S\nbody without empty line\n",
			"Subject:    \n\nbody",
	})
	void malformedTemplatesAreRejected(String text) {
		assertThatThrownBy(() -> MessageTemplate.parse(text)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void aMissingResourceIsReportedWithItsPath() {
		assertThatThrownBy(() -> MessageTemplate.fromResource("does-not-exist"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("/mail/does-not-exist.txt");
	}

	@ParameterizedTest
	@ValueSource(strings = { "round-started", "round-closed", "verification-code" })
	void everyShippedTemplateLoadsAndRendersWithoutLeftoverBraces(String name) {
		MessageTemplate template = MessageTemplate.fromResource(name);
		Map<String, String> values = new HashMap<>();
		template.placeholders().forEach(placeholder -> values.put(placeholder, "VALUE"));

		Message message = template.render(values);

		assertThat(message.subject() + message.body()).doesNotContain("{").doesNotContain("}");
	}

	@Test
	void shippedTemplatesUseExactlyThePlaceholdersTheCodeFillsIn() {
		assertThat(MessageTemplate.fromResource("round-started").placeholders())
				.isEqualTo(Set.of("starter", "groupName", "link"));
		assertThat(MessageTemplate.fromResource("round-closed").placeholders())
				.isEqualTo(Set.of("groupName", "min", "max", "average", "submissions", "link"));
		assertThat(MessageTemplate.fromResource("verification-code").placeholders()).isEqualTo(Set.of("code"));
	}

}
