package com.moodtracker.application;

import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.event.RoundClosed;
import com.moodtracker.domain.event.RoundStarted;
import com.moodtracker.domain.notification.Message;

import java.util.Locale;

final class NotificationMessages {

	private NotificationMessages() {
	}

	static Message roundStarted(RoundStarted event, String frontendBaseUrl) {
		String body = """
				%s started a mood round in "%s".

				Submit your mood: %s
				""".formatted(event.startedBy(), event.groupName(), groupLink(frontendBaseUrl, event.groupId()));
		return new Message(subject(event.groupName(), "A new mood round has started"), body);
	}

	static Message roundClosed(RoundClosed event, String frontendBaseUrl) {
		RoundResult result = event.result();
		String body = """
				Everyone in "%s" has submitted, the round is complete.

				Lowest: %d
				Highest: %d
				Average: %s
				Submissions: %d

				See the history: %s
				""".formatted(event.groupName(), result.min(), result.max(),
				String.format(Locale.ROOT, "%.2f", result.average()), result.submissionCount(),
				groupLink(frontendBaseUrl, event.groupId()));
		return new Message(subject(event.groupName(), "Mood round complete"), body);
	}

	private static String groupLink(String frontendBaseUrl, String groupId) {
		return frontendBaseUrl.replaceAll("/+$", "") + "/groups/" + groupId;
	}

	// The group name is user input: a CR/LF in a subject would let it inject mail headers.
	private static String subject(String groupName, String text) {
		String safeName = groupName.replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
		return "[" + safeName + "] " + text;
	}

}
