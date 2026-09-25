package com.moodtracker.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MemberEmbeddable {

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private boolean notifyOnRoundComplete;

	protected MemberEmbeddable() {
	}

	public MemberEmbeddable(String email, boolean notifyOnRoundComplete) {
		this.email = email;
		this.notifyOnRoundComplete = notifyOnRoundComplete;
	}

	public String getEmail() {
		return email;
	}

	public boolean isNotifyOnRoundComplete() {
		return notifyOnRoundComplete;
	}

}
