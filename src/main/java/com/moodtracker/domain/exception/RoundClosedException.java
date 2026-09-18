package com.moodtracker.domain.exception;

public class RoundClosedException extends MoodTrackerException {

	public RoundClosedException(String roundId) {
		super("Round " + roundId + " is already closed");
	}

}
