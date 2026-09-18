package com.moodtracker.domain;

import com.moodtracker.domain.exception.DuplicateMemberException;
import com.moodtracker.domain.exception.InvalidGroupConfigurationException;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundAlreadyOpenException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Group {

	private final String id;
	private final Name name;
	private final MoodRange moodRange;
	private final Map<String, Member> members = new LinkedHashMap<>();
	private final List<Round> rounds = new ArrayList<>();

	public Group(Name name, MoodRange moodRange, List<String> memberEmails) {
		this(null, name, moodRange);
		if (memberEmails == null || memberEmails.isEmpty()) {
			throw new InvalidGroupConfigurationException("A group needs at least one member");
		}
		for (String email : memberEmails) {
			addMember(email);
		}
	}

	private Group(String id, Name name, MoodRange moodRange) {
		this.id = id;
		this.name = name;
		this.moodRange = moodRange;
	}

	/**
	 * Rebuilds a group with its original members (preserving per-member
	 * notification
	 * preference) and round history, bypassing constructor validation since the
	 * data was
	 * already validated when it was first written. For persistence reconstruction
	 * only.
	 */
	public static Group reconstitute(String id, Name name, MoodRange moodRange, List<Member> members,
			List<Round> rounds) {
		Group group = new Group(id, name, moodRange);
		for (Member member : members) {
			group.members.put(member.email().value(), member);
		}
		group.rounds.addAll(rounds);
		return group;
	}

	public Round startRound(String startedByEmail) {
		if (!members.containsKey(startedByEmail)) {
			throw new NotAGroupMemberException(startedByEmail);
		}
		if (currentRound().isPresent()) {
			throw new RoundAlreadyOpenException(id);
		}
		Round round = new Round(id, moodRange, members.keySet(), startedByEmail);
		rounds.add(round);
		return round;
	}

	public Optional<Round> currentRound() {
		if (rounds.isEmpty()) {
			return Optional.empty();
		}
		Round last = rounds.get(rounds.size() - 1);
		return last.status() == Round.Status.OPEN ? Optional.of(last) : Optional.empty();
	}

	public List<Round> closedRounds() {
		return rounds.stream().filter(r -> r.status() == Round.Status.CLOSED).toList();
	}

	public List<Round> rounds() {
		return Collections.unmodifiableList(rounds);
	}

	public void addMember(String email) {
		if (members.putIfAbsent(email, new Member(new Email(email))) != null) {
			throw new DuplicateMemberException(email);
		}
	}

	public String id() {
		return id;
	}

	public Name name() {
		return name;
	}

	public MoodRange moodRange() {
		return moodRange;
	}

	public Set<String> memberEmails() {
		return Collections.unmodifiableSet(members.keySet());
	}

	public Collection<Member> members() {
		return Collections.unmodifiableCollection(members.values());
	}

}
