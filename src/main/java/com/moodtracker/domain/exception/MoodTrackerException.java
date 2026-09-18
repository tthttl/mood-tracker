package com.moodtracker.domain.exception;

public abstract class MoodTrackerException extends RuntimeException {

	protected MoodTrackerException(String message) {
		super(message);
	}

}
