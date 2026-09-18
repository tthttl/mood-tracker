package com.moodtracker.domain;

import com.moodtracker.domain.exception.InvalidGroupConfigurationException;
import com.moodtracker.domain.exception.InvalidMoodValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoodRangeTest {

	@Test
	void createsValidMoodRange() {
		MoodRange range = new MoodRange(5);

		assertThat(range.value()).isEqualTo(5);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1, -10 })
	void rejectsNonPositiveMoodRange(int invalidValue) {
		assertThatThrownBy(() -> new MoodRange(invalidValue))
				.isInstanceOf(InvalidGroupConfigurationException.class)
				.hasMessage("Mood range must be > 0");
	}

	@ParameterizedTest
	@ValueSource(ints = { -3, -2, 0, 2, 3 })
	void validatesValuesWithinRange(int validScore) {
		MoodRange range = new MoodRange(3);

		assertThatCode(() -> range.validate(validScore))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(ints = { -4, 4, 10, -10 })
	void rejectsValuesOutsideRange(int invalidScore) {
		MoodRange range = new MoodRange(3);

		assertThatThrownBy(() -> range.validate(invalidScore))
				.isInstanceOf(InvalidMoodValueException.class);
	}

}
