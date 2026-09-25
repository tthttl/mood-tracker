package com.moodtracker.domain.auth;

import com.moodtracker.domain.auth.exception.InvalidVerificationCodeException;

import java.time.Instant;

public final class VerificationCode {

	private final String email;
	private final String code;
	private final Instant expiresAt;
	private boolean used;

	private VerificationCode(String email, String code, Instant expiresAt) {
		this.email = email;
		this.code = code;
		this.expiresAt = expiresAt;
	}

	public static VerificationCode issue(String email, String code, Instant expiresAt) {
		return new VerificationCode(email, code, expiresAt);
	}

	public void verify(String suppliedCode) {
		if (used || Instant.now().isAfter(expiresAt) || !code.equals(suppliedCode)) {
			throw new InvalidVerificationCodeException();
		}
		used = true;
	}

	public String email() {
		return email;
	}

}
