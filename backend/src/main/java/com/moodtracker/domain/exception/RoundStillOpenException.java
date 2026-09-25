package com.moodtracker.domain.exception;

public class RoundStillOpenException extends MoodTrackerException {

	public RoundStillOpenException(String roundId) {
		super("Round " + roundId + " has not concluded yet");
	}

}
