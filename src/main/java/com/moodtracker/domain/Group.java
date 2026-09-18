package com.moodtracker.domain;

import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundAlreadyOpenException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Group {

	private final String id;
	private final Name name;
	private final MoodRange moodRange;
	private final Members members;
	private final List<Round> rounds;

	public Group(Name name, MoodRange moodRange, List<String> memberEmails) {
		this(null, name, moodRange, Members.of(memberEmails), List.of());
	}

	private Group(String id, Name name, MoodRange moodRange, Members members, List<Round> rounds) {
		this.id = id;
		this.name = name;
		this.moodRange = moodRange;
		this.members = members;
		this.rounds = new ArrayList<>(rounds);
	}

	public static Group reconstitute(String id, Name name, MoodRange moodRange, List<Member> members,
			List<Round> rounds) {
		return new Group(id, name, moodRange, Members.from(members), rounds);
	}

	public Round startRound(String startedByEmail) {
		if (!members.contains(startedByEmail)) {
			throw new NotAGroupMemberException(startedByEmail);
		}
		if (currentRound().isPresent()) {
			throw new RoundAlreadyOpenException(id);
		}
		Round round = new Round(id, moodRange, members.emails(), startedByEmail);
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
		members.add(email);
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
		return members.emails();
	}

	public Collection<Member> members() {
		return members.all();
	}

}
