package com.moodtracker.domain;

import java.util.Objects;

public final class Member {

	private final Email email;
	private final boolean notifyOnRoundComplete;

	public Member(Email email) {
		this(email, true);
	}

	public Member(Email email, boolean notifyOnRoundComplete) {
		this.email = email;
		this.notifyOnRoundComplete = notifyOnRoundComplete;
	}

	public Email email() {
		return email;
	}

	public boolean notifyOnRoundComplete() {
		return notifyOnRoundComplete;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Member member))
			return false;
		return email.equals(member.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

}
