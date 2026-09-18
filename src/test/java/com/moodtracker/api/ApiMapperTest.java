package com.moodtracker.api;

import com.moodtracker.api.generated.model.GroupResponse;
import com.moodtracker.api.generated.model.RoundStatusResponse;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.Round;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiMapperTest {

	@Test
	void mapsAGroupWithSortedMemberEmails() {
		Group group = Group.create(new Name("Team A"), new MoodRange(2), List.of("b@test.com", "a@test.com"));

		GroupResponse response = ApiMapper.toGroupResponse(group);

		assertThat(response.getName()).isEqualTo("Team A");
		assertThat(response.getMoodRange()).isEqualTo(2);
		assertThat(response.getMemberEmails()).containsExactly("a@test.com", "b@test.com");
	}

	@Test
	void openRoundShowsWhoSubmittedAndWhoIsPendingButNoResult() {
		Group group = Group.create(new Name("Team A"), new MoodRange(1),
				List.of("a@test.com", "b@test.com", "c@test.com"));
		Round round = group.startRound("a@test.com");
		round.submit("b@test.com", 1);

		RoundStatusResponse response = ApiMapper.toRoundStatusResponse(round);

		assertThat(response.getStatus()).isEqualTo(RoundStatusResponse.StatusEnum.OPEN);
		assertThat(response.getStartedBy()).isEqualTo("a@test.com");
		assertThat(response.getSubmittedMemberEmails()).containsExactly("b@test.com");
		assertThat(response.getPendingMemberEmails()).containsExactly("a@test.com", "c@test.com");
		assertThat(response.getClosedAt()).isNull();
		assertThat(response.getResult()).isNull();
	}

	@Test
	void closedRoundIncludesTheAggregateResult() {
		Group group = Group.create(new Name("Team A"), new MoodRange(1), List.of("a@test.com", "b@test.com"));
		Round round = group.startRound("a@test.com");
		round.submit("a@test.com", -1);
		round.submit("b@test.com", 1);

		RoundStatusResponse response = ApiMapper.toRoundStatusResponse(round);

		assertThat(response.getStatus()).isEqualTo(RoundStatusResponse.StatusEnum.CLOSED);
		assertThat(response.getPendingMemberEmails()).isEmpty();
		assertThat(response.getClosedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
		assertThat(response.getResult().getMin()).isEqualTo(-1);
		assertThat(response.getResult().getMax()).isEqualTo(1);
		assertThat(response.getResult().getAverage()).isEqualTo(0.0);
		assertThat(response.getResult().getSubmissionCount()).isEqualTo(2);
	}

}
