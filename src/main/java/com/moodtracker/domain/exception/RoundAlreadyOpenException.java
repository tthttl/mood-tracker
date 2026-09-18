package com.moodtracker.domain.exception;

public class RoundAlreadyOpenException extends MoodTrackerException {

	public RoundAlreadyOpenException(String groupId) {
		super("Group " + groupId + " already has an open round");
	}

}
