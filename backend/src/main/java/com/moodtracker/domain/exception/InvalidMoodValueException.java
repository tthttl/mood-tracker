package com.moodtracker.domain.exception;

public class InvalidMoodValueException extends MoodTrackerException {

	public InvalidMoodValueException(int value, int moodRange) {
		super("Mood value " + value + " is outside the allowed range [-" + moodRange + ", " + moodRange + "]");
	}

}
