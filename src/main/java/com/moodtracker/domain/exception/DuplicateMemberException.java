package com.moodtracker.domain.exception;

public class DuplicateMemberException extends MoodTrackerException {

	public DuplicateMemberException(String email) {
		super(email + " is already a member of this group");
	}

}
