package com.moodtracker.domain.exception;

public class DuplicateSubmissionException extends MoodTrackerException {

	public DuplicateSubmissionException(String roundId, String email) {
		super(email + " has already submitted a mood for round " + roundId);
	}

}
