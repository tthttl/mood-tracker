package com.moodtracker.domain.event;

import com.moodtracker.domain.RoundResult;

import java.util.List;

public record RoundClosed(String groupId, String groupName, RoundResult result, List<String> recipients) {
}
