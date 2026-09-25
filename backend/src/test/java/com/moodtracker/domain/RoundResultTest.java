package com.moodtracker.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundResultTest {

	@Test
	void computesMinMaxAndAverageFromSubmittedValues() {
		RoundResult result = RoundResult.from("round-1", List.of(-1, 0, 1), Instant.now());

		assertThat(result.min()).isEqualTo(-1);
		assertThat(result.max()).isEqualTo(1);
		assertThat(result.average()).isEqualTo(0.0);
		assertThat(result.submissionCount()).isEqualTo(3);
	}

	@Test
	void singleSubmissionYieldsEqualMinMaxAndAverage() {
		RoundResult result = RoundResult.from("round-1", List.of(1), Instant.now());

		assertThat(result.min()).isEqualTo(1);
		assertThat(result.max()).isEqualTo(1);
		assertThat(result.average()).isEqualTo(1.0);
		assertThat(result.submissionCount()).isEqualTo(1);
	}

}
