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
import java.util.UUID;

public final class Group {

	private final String id;
	private Name name;
	private final MoodRange moodRange;
	private final Map<String, Member> members = new LinkedHashMap<>();
	private final List<Round> rounds = new ArrayList<>();

	public Group(String id, Name name, MoodRange moodRange, List<String> memberEmails) {
		if (memberEmails == null || memberEmails.isEmpty()) {
			throw new InvalidGroupConfigurationException("A group needs at least one member");
		}
		this.id = id;
		this.name = name;
		this.moodRange = moodRange;
		for (String email : memberEmails) {
			addMember(email);
		}
	}

	public Round startRound(String startedByEmail) {
		if (!members.containsKey(startedByEmail)) {
			throw new NotAGroupMemberException(startedByEmail);
		}
		if (currentRound().isPresent()) {
			throw new RoundAlreadyOpenException(id);
		}
		Round round = new Round(UUID.randomUUID().toString(), id, moodRange, members.keySet(), startedByEmail);
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
