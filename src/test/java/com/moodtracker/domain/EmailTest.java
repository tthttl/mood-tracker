package com.moodtracker.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

	@Test
	void createsValidEmail() {
		Email email = new Email("user@test.com");

		assertThat(email.value()).isEqualTo("user@test.com");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t", "\n" })
	void rejectsNullOrBlankEmail(String invalidEmail) {
		assertThatThrownBy(() -> new Email(invalidEmail))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email cannot be blank");
	}

}
