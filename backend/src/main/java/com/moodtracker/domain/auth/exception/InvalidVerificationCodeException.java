package com.moodtracker.domain.auth.exception;

import com.moodtracker.domain.exception.MoodTrackerException;

public class InvalidVerificationCodeException extends MoodTrackerException {

	public InvalidVerificationCodeException() {
		super("Invalid or expired verification code");
	}

}
