package com.moodtracker.domain.exception;

public class NoOpenRoundException extends MoodTrackerException {

	public NoOpenRoundException(String groupId) {
		super("Group " + groupId + " has no open round");
	}

}
