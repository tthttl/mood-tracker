package com.moodtracker.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class SubmissionEmbeddable {

	@Column(nullable = false)
	private String email;

	@Column(name = "mood_value", nullable = false)
	private int value;

	@Column(nullable = false)
	private Instant submittedAt;

	protected SubmissionEmbeddable() {
	}

	public SubmissionEmbeddable(String email, int value, Instant submittedAt) {
		this.email = email;
		this.value = value;
		this.submittedAt = submittedAt;
	}

	public String getEmail() {
		return email;
	}

	public int getValue() {
		return value;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

}
