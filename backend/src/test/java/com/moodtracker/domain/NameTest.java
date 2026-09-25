package com.moodtracker.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NameTest {

	@Test
	void createsValidName() {
		Name name = new Name("Team A");

		assertThat(name.value()).isEqualTo("Team A");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t", "\n" })
	void rejectsNullOrBlankName(String invalidName) {
		assertThatThrownBy(() -> new Name(invalidName))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Name cannot be blank");
	}

}
