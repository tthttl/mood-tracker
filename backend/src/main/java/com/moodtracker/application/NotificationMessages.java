package com.moodtracker.application;

import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.event.RoundClosed;
import com.moodtracker.domain.event.RoundStarted;
import com.moodtracker.domain.notification.Message;
import com.moodtracker.domain.notification.MessageTemplate;

import java.util.Locale;
import java.util.Map;

final class NotificationMessages {
	private static final MessageTemplate ROUND_STARTED = MessageTemplate.fromResource("round-started");
	private static final MessageTemplate ROUND_CLOSED = MessageTemplate.fromResource("round-closed");

	private NotificationMessages() {
	}

	static Message roundStarted(RoundStarted event, String frontendBaseUrl) {
		return ROUND_STARTED.render(Map.of(
				"starter", event.startedBy(),
				"groupName", event.groupName(),
				"link", groupLink(frontendBaseUrl, event.groupId())));
	}

	static Message roundClosed(RoundClosed event, String frontendBaseUrl) {
		RoundResult result = event.result();
		return ROUND_CLOSED.render(Map.of(
				"groupName", event.groupName(),
				"min", String.valueOf(result.min()),
				"max", String.valueOf(result.max()),
				"average", String.format(Locale.ROOT, "%.2f", result.average()),
				"submissions", String.valueOf(result.submissionCount()),
				"link", groupLink(frontendBaseUrl, event.groupId())));
	}

	private static String groupLink(String frontendBaseUrl, String groupId) {
		return frontendBaseUrl.replaceAll("/+$", "") + "/groups/" + groupId;
	}

}
