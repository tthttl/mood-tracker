package com.moodtracker.domain;

import com.moodtracker.domain.exception.DuplicateSubmissionException;
import com.moodtracker.domain.exception.InvalidMoodValueException;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundClosedException;
import com.moodtracker.domain.exception.RoundStillOpenException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundTest {

	private static Group groupOf(int moodRange, String... emails) {
		return new Group("group-1", new Name("Team A"), new MoodRange(moodRange), List.of(emails));
	}

	@Test
	void acceptsSubmissionsAtBoundaryValues() {
		Round round = groupOf(1, "a@test.com", "b@test.com").startRound("a@test.com");

		round.submit("a@test.com", -1);
		round.submit("b@test.com", 1);

		assertThat(round.submittedMemberEmails()).containsExactlyInAnyOrder("a@test.com", "b@test.com");
	}

	@Test
	void rejectsValuesOutsideMoodRange() {
		Round round = groupOf(1, "a@test.com", "b@test.com").startRound("a@test.com");

		assertThatThrownBy(() -> round.submit("a@test.com", 2))
				.isInstanceOf(InvalidMoodValueException.class);
		assertThatThrownBy(() -> round.submit("a@test.com", -2))
				.isInstanceOf(InvalidMoodValueException.class);
	}

	@Test
	void rejectsSubmissionFromNonMember() {
		Round round = groupOf(1, "a@test.com", "b@test.com").startRound("a@test.com");

		assertThatThrownBy(() -> round.submit("stranger@test.com", 0))
				.isInstanceOf(NotAGroupMemberException.class);
	}

	@Test
	void rejectsDuplicateSubmissionFromSameMember() {
		Round round = groupOf(1, "a@test.com", "b@test.com").startRound("a@test.com");
		round.submit("a@test.com", 0);

		assertThatThrownBy(() -> round.submit("a@test.com", 1))
				.isInstanceOf(DuplicateSubmissionException.class);
	}

	@Test
	void autoClosesOnceLastEligibleMemberSubmits() {
		Round round = groupOf(1, "a@test.com", "b@test.com").startRound("a@test.com");
		round.submit("a@test.com", 0);
		assertThat(round.status()).isEqualTo(Round.Status.OPEN);

		round.submit("b@test.com", 1);

		assertThat(round.status()).isEqualTo(Round.Status.CLOSED);
		assertThatThrownBy(() -> round.submit("a@test.com", 0))
				.isInstanceOf(RoundClosedException.class);
	}

	@Test
	void singleMemberGroupClosesAfterFirstSubmission() {
		Round round = groupOf(1, "solo@test.com").startRound("solo@test.com");

		round.submit("solo@test.com", -1);

		assertThat(round.status()).isEqualTo(Round.Status.CLOSED);
		assertThat(round.result().min()).isEqualTo(-1);
		assertThat(round.result().max()).isEqualTo(-1);
	}

	@Test
	void resultThrowsWhileRoundStillOpen() {
		Round round = groupOf(1, "a@test.com", "b@test.com").startRound("a@test.com");
		round.submit("a@test.com", 0);

		assertThatThrownBy(round::result).isInstanceOf(RoundStillOpenException.class);
	}

	@Test
	void resultAggregatesMinMaxAverageAfterClosing() {
		Round round = groupOf(1, "a@test.com", "b@test.com", "c@test.com").startRound("a@test.com");
		round.submit("a@test.com", -1);
		round.submit("b@test.com", 0);
		round.submit("c@test.com", 1);

		RoundResult result = round.result();

		assertThat(result.min()).isEqualTo(-1);
		assertThat(result.max()).isEqualTo(1);
		assertThat(result.average()).isEqualTo(0.0);
		assertThat(result.submissionCount()).isEqualTo(3);
	}

}
