package com.moodtracker.domain;

import com.moodtracker.domain.exception.DuplicateSubmissionException;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundClosedException;
import com.moodtracker.domain.exception.RoundStillOpenException;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Round {

	public enum Status {
		OPEN, CLOSED
	}

	private record Submission(String email, int value, Instant submittedAt) {
	}

	private final String id;
	private final String groupId;
	private final MoodRange moodRange;
	private final Set<String> eligibleMemberEmails;
	private final String startedBy;
	private final Instant startedAt;
	private final Map<String, Submission> submissions = new LinkedHashMap<>();
	private Instant closedAt;

	Round(String id, String groupId, MoodRange moodRange, Set<String> eligibleMemberEmails,
			String startedBy) {
		this.id = id;
		this.groupId = groupId;
		this.moodRange = moodRange;
		this.eligibleMemberEmails = Set.copyOf(eligibleMemberEmails);
		this.startedBy = startedBy;
		this.startedAt = Instant.now();
	}

	public void submit(String email, int value) {
		if (status() == Status.CLOSED) {
			throw new RoundClosedException(id);
		}
		if (!eligibleMemberEmails.contains(email)) {
			throw new NotAGroupMemberException(email);
		}
		if (submissions.containsKey(email)) {
			throw new DuplicateSubmissionException(id, email);
		}
		moodRange.validate(value);
		submissions.put(email, new Submission(email, value, Instant.now()));
		if (submissions.size() == eligibleMemberEmails.size()) {
			closedAt = Instant.now();
		}
	}

	public Status status() {
		return closedAt == null ? Status.OPEN : Status.CLOSED;
	}

	public Set<String> submittedMemberEmails() {
		return Collections.unmodifiableSet(submissions.keySet());
	}

	public RoundResult result() {
		if (status() == Status.OPEN) {
			throw new RoundStillOpenException(id);
		}
		var values = submissions.values().stream().map(t -> t.value()).toList();
		return RoundResult.from(id, values, closedAt);
	}

	public String id() {
		return id;
	}

	public String groupId() {
		return groupId;
	}

	public String startedBy() {
		return startedBy;
	}

	public Instant startedAt() {
		return startedAt;
	}

	public Instant closedAt() {
		return closedAt;
	}

}
