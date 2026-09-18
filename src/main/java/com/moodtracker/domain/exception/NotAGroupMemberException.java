package com.moodtracker.domain.exception;

public class NotAGroupMemberException extends MoodTrackerException {

	public NotAGroupMemberException(String email) {
		super(email + " is not a member of this group");
	}

}
