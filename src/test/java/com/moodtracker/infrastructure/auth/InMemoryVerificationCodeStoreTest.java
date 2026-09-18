package com.moodtracker.infrastructure.auth;

import com.moodtracker.domain.auth.VerificationCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVerificationCodeStoreTest {

	private final InMemoryVerificationCodeStore store = new InMemoryVerificationCodeStore();

	@Test
	void findReturnsEmptyForUnknownEmail() {
		assertThat(store.find("missing@test.com")).isEmpty();
	}

	@Test
	void savedCodeCanBeFoundByEmail() {
		VerificationCode code = VerificationCode.issue("a@test.com", "123456", future());

		store.save(code);

		assertThat(store.find("a@test.com")).contains(code);
	}

	@Test
	void savingAgainForTheSameEmailSupersedesThePreviousCode() {
		VerificationCode first = VerificationCode.issue("a@test.com", "111111", future());
		VerificationCode second = VerificationCode.issue("a@test.com", "222222", future());

		store.save(first);
		store.save(second);

		assertThat(store.find("a@test.com")).contains(second);
	}

	private static Instant future() {
		return Instant.now().plus(10, ChronoUnit.MINUTES);
	}

}
