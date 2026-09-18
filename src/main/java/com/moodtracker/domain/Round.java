package com.moodtracker.domain;

import com.moodtracker.domain.exception.DuplicateSubmissionException;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundClosedException;
import com.moodtracker.domain.exception.RoundStillOpenException;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Round {

	public enum Status {
		OPEN, CLOSED
	}

	private final String id;
	private final String groupId;
	private final MoodRange moodRange;
	private final Set<String> eligibleMemberEmails;
	private final String startedBy;
	private final Instant startedAt;
	private final Map<String, Submission> submissions;

	private Instant closedAt;

	private Round(String id, String groupId, MoodRange moodRange, Set<String> eligibleMemberEmails,
			String startedBy, Instant startedAt, Instant closedAt, Map<String, Submission> submissions) {
		this.id = id;
		this.groupId = groupId;
		this.moodRange = moodRange;
		this.eligibleMemberEmails = Set.copyOf(eligibleMemberEmails);
		this.startedBy = startedBy;
		this.startedAt = startedAt;
		this.closedAt = closedAt;
		this.submissions = submissions;
	}

	public static Round create(String groupId, MoodRange moodRange, Set<String> eligibleMemberEmails,
			String startedBy) {
		return new Round(null, groupId, moodRange, eligibleMemberEmails, startedBy, Instant.now(), null,
				new LinkedHashMap<>());
	}

	public static Round of(String id, String groupId, MoodRange moodRange, Set<String> eligibleMemberEmails,
			String startedBy, Instant startedAt, Instant closedAt, List<Submission> submissions) {
		return new Round(id, groupId, moodRange, eligibleMemberEmails, startedBy, startedAt, closedAt,
				convertSubmissionListToMap(submissions));
	}

	private static Map<String, Submission> convertSubmissionListToMap(List<Submission> submissions) {
		return submissions.stream()
				.collect(Collectors.toMap(Submission::email, Function.identity(), (e1, e2) -> e2, LinkedHashMap::new));
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

	public Set<String> eligibleMemberEmails() {
		return eligibleMemberEmails;
	}

	public MoodRange moodRange() {
		return moodRange;
	}

	public List<Submission> getSubmissions() {
		return List.copyOf(submissions.values());
	}

	public RoundResult result() {
		if (status() == Status.OPEN) {
			throw new RoundStillOpenException(id);
		}
		var values = submissions.values().stream().map(Submission::value).toList();
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
