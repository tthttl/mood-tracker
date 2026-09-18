package com.moodtracker.domain;

import com.moodtracker.domain.exception.DuplicateMemberException;
import com.moodtracker.domain.exception.InvalidGroupConfigurationException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Members {

	private final Map<String, Member> byEmail;

	private Members(Map<String, Member> byEmail) {
		this.byEmail = byEmail;
	}

	public static Members of(List<String> emails) {
		if (emails == null || emails.isEmpty()) {
			throw new InvalidGroupConfigurationException("A group needs at least one member");
		}
		Members members = new Members(new LinkedHashMap<>());
		for (String email : emails) {
			members.add(email);
		}
		return members;
	}

	public static Members from(Collection<Member> members) {
		if (members == null || members.isEmpty()) {
			throw new InvalidGroupConfigurationException("A group needs at least one member");
		}
		Map<String, Member> byEmail = new LinkedHashMap<>();
		for (Member member : members) {
			if (byEmail.putIfAbsent(member.email().value(), member) != null) {
				throw new DuplicateMemberException(member.email().value());
			}
		}
		return new Members(byEmail);
	}

	public void add(String email) {
		if (byEmail.putIfAbsent(email, new Member(new Email(email))) != null) {
			throw new DuplicateMemberException(email);
		}
	}

	public boolean contains(String email) {
		return byEmail.containsKey(email);
	}

	public Set<String> emails() {
		return Collections.unmodifiableSet(byEmail.keySet());
	}

	public Collection<Member> all() {
		return Collections.unmodifiableCollection(byEmail.values());
	}

}
