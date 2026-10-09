package com.moodtracker.domain.event;

import java.util.List;

public record RoundStarted(String groupId, String groupName, String roundId, String startedBy,
		List<String> recipients) {
}
