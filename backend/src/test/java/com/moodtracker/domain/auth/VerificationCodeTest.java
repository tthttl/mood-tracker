package com.moodtracker.domain.auth;

import com.moodtracker.domain.auth.exception.InvalidVerificationCodeException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationCodeTest {

	@Test
	void verifiesSuccessfullyWithTheCorrectCode() {
		VerificationCode code = VerificationCode.issue("a@test.com", "123456", future());

		assertThatCode(() -> code.verify("123456")).doesNotThrowAnyException();
	}

	@Test
	void rejectsTheWrongCode() {
		VerificationCode code = VerificationCode.issue("a@test.com", "123456", future());

		assertThatThrownBy(() -> code.verify("000000"))
				.isInstanceOf(InvalidVerificationCodeException.class);
	}

	@Test
	void rejectsAnExpiredCode() {
		VerificationCode code = VerificationCode.issue("a@test.com", "123456", past());

		assertThatThrownBy(() -> code.verify("123456"))
				.isInstanceOf(InvalidVerificationCodeException.class);
	}

	@Test
	void rejectsReplayingAnAlreadyVerifiedCode() {
		VerificationCode code = VerificationCode.issue("a@test.com", "123456", future());
		code.verify("123456");

		assertThatThrownBy(() -> code.verify("123456"))
				.isInstanceOf(InvalidVerificationCodeException.class);
	}

	private static Instant future() {
		return Instant.now().plus(10, ChronoUnit.MINUTES);
	}

	private static Instant past() {
		return Instant.now().minus(1, ChronoUnit.MINUTES);
	}

}
