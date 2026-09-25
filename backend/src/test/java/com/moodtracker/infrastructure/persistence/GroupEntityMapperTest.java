package com.moodtracker.infrastructure.persistence;

import com.moodtracker.domain.Group;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.RoundResult;
import com.moodtracker.infrastructure.persistence.entity.GroupJpaEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupEntityMapperTest {

	@Test
	void roundTripsAGroupWithAClosedRound() {
		Group group = Group.create(new Name("Team A"), new MoodRange(1), List.of("a@test.com", "b@test.com"));
		group.addMember("c@test.com");
		Round round = group.startRound("a@test.com");
		round.submit("a@test.com", -1);
		round.submit("b@test.com", 0);
		round.submit("c@test.com", 1);

		GroupJpaEntity entity = GroupEntityMapper.toEntity(group);
		Group reconstructed = GroupEntityMapper.toDomain(entity);

		// group/round ids are assigned by the database on save, not by this pure
		// mapper,
		// so a group that was never actually persisted round-trips with a null id.
		assertThat(reconstructed.id()).isNull();
		assertThat(reconstructed.name()).isEqualTo(new Name("Team A"));
		assertThat(reconstructed.moodRange()).isEqualTo(new MoodRange(1));
		assertThat(reconstructed.memberEmails()).containsExactlyInAnyOrder("a@test.com", "b@test.com", "c@test.com");
		assertThat(reconstructed.currentRound()).isEmpty();
		assertThat(reconstructed.closedRounds()).hasSize(1);

		Round reconstructedRound = reconstructed.closedRounds().get(0);
		assertThat(reconstructedRound.id()).isNull();
		assertThat(reconstructedRound.status()).isEqualTo(Round.Status.CLOSED);
		assertThat(reconstructedRound.submittedMemberEmails())
				.containsExactlyInAnyOrder("a@test.com", "b@test.com", "c@test.com");

		RoundResult result = reconstructedRound.result();
		assertThat(result.min()).isEqualTo(-1);
		assertThat(result.max()).isEqualTo(1);
		assertThat(result.average()).isEqualTo(0.0);
		assertThat(result.submissionCount()).isEqualTo(3);
	}

	@Test
	void roundTripsAGroupWithAnOpenRoundAndPartialSubmissions() {
		Group group = Group.create(new Name("Team B"), new MoodRange(2),
				List.of("a@test.com", "b@test.com", "c@test.com"));
		Round round = group.startRound("a@test.com");
		round.submit("a@test.com", 2);

		Group reconstructed = GroupEntityMapper.toDomain(GroupEntityMapper.toEntity(group));

		assertThat(reconstructed.closedRounds()).isEmpty();
		assertThat(reconstructed.currentRound()).isPresent();
		Round reconstructedRound = reconstructed.currentRound().orElseThrow();
		assertThat(reconstructedRound.status()).isEqualTo(Round.Status.OPEN);
		assertThat(reconstructedRound.submittedMemberEmails()).containsExactly("a@test.com");
		assertThat(reconstructedRound.eligibleMemberEmails())
				.containsExactlyInAnyOrder("a@test.com", "b@test.com", "c@test.com");

		reconstructedRound.submit("b@test.com", -2);
		reconstructedRound.submit("c@test.com", 0);
		assertThat(reconstructedRound.status()).isEqualTo(Round.Status.CLOSED);
	}

	@Test
	void preservesPerMemberNotificationPreference() {
		Group group = Group.create(new Name("Team C"), new MoodRange(1), List.of("a@test.com"));

		Group reconstructed = GroupEntityMapper.toDomain(GroupEntityMapper.toEntity(group));

		assertThat(reconstructed.members())
				.filteredOn(member -> member.email().value().equals("a@test.com"))
				.singleElement()
				.satisfies(member -> assertThat(member.notifyOnRoundComplete()).isTrue());
	}

}
