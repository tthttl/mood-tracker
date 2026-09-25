package com.moodtracker.domain.exception;

public class GroupNotFoundException extends MoodTrackerException {

	public GroupNotFoundException(String groupId) {
		super("No group found with id " + groupId);
	}

}
