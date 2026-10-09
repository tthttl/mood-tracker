package com.moodtracker.application;

import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.event.RoundClosed;
import com.moodtracker.domain.event.RoundStarted;
import com.moodtracker.domain.notification.Message;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessagesTest {

	private static RoundStarted started(String groupName) {
		return new RoundStarted("g-1", groupName, "r-1", "ann@x.com", List.of("bob@x.com"));
	}

	@Test
	void roundStartedNamesTheStarterAndLinksToTheGroup() {
		Message message = NotificationMessages.roundStarted(started("Team Rocket"), "http://localhost:4200");

		assertThat(message.subject()).isEqualTo("[Team Rocket] A new mood round has started");
		assertThat(message.body()).contains("ann@x.com started a mood round in \"Team Rocket\"")
				.contains("http://localhost:4200/groups/g-1");
	}

	@Test
	void aTrailingSlashInTheBaseUrlDoesNotProduceADoubleSlash() {
		Message message = NotificationMessages.roundStarted(started("Team"), "https://mood.example.com//");

		assertThat(message.body()).contains("https://mood.example.com/groups/g-1");
	}

	@Test
	void roundClosedShowsTheAggregateOnly() {
		RoundClosed event = new RoundClosed("g-1", "Team", new RoundResult("r-1", -1, 2, 0.5, 2, Instant.now()),
				List.of("ann@x.com", "bob@x.com"));

		Message message = NotificationMessages.roundClosed(event, "http://localhost:4200");

		assertThat(message.subject()).isEqualTo("[Team] Mood round complete");
		assertThat(message.body()).contains("Lowest: -1").contains("Highest: 2").contains("Average: 0.50")
				.contains("Submissions: 2").contains("http://localhost:4200/groups/g-1");
	}

	@Test
	void controlCharactersInTheGroupNameCannotInjectMailHeaders() {
		Message message = NotificationMessages.roundStarted(started("Team\r\nBcc: attacker@evil.com"), "http://x");

		assertThat(message.subject()).doesNotContain("\r").doesNotContain("\n");
		assertThat(message.subject()).isEqualTo("[Team Bcc: attacker@evil.com] A new mood round has started");
	}

}
