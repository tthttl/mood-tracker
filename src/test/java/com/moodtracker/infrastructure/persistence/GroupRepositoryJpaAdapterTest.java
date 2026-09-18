package com.moodtracker.infrastructure.persistence;

import com.moodtracker.domain.Group;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.Round;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GroupRepositoryJpaAdapterTest {

	@Autowired
	private SpringDataGroupJpaRepository jpaRepository;

	private GroupRepositoryJpaAdapter adapter() {
		return new GroupRepositoryJpaAdapter(jpaRepository);
	}

	@Test
	void savesAndReloadsAGroupWithAClosedRoundThroughRealH2() {
		Group group = Group.create(new Name("Team A"), new MoodRange(1),
				List.of("a@test.com", "b@test.com"));
		Round round = group.startRound("a@test.com");
		round.submit("a@test.com", -1);
		round.submit("b@test.com", 1);

		Group persistedGroup = adapter().save(group);

		Optional<Group> reloaded = adapter().findById(persistedGroup.id());

		assertThat(reloaded).isPresent();
		Group loaded = reloaded.orElseThrow();
		assertThat(loaded.name()).isEqualTo(new Name("Team A"));
		assertThat(loaded.memberEmails()).containsExactlyInAnyOrder("a@test.com", "b@test.com");
		assertThat(loaded.closedRounds()).hasSize(1);
		assertThat(loaded.closedRounds().get(0).result().average()).isEqualTo(0.0);
	}

	@Test
	void findByIdReturnsEmptyForUnknownGroup() {
		assertThat(adapter().findById("missing")).isEmpty();
	}

	@Test
	void findAllReturnsEveryPersistedGroup() {
		Group persistedGroupA = adapter().save(Group.create(new Name("A"), new MoodRange(1), List.of("a@test.com")));
		Group persistedGroupB = adapter().save(Group.create(new Name("B"), new MoodRange(1), List.of("b@test.com")));

		assertThat(adapter().findAll()).extracting(group -> group.id()).containsExactlyInAnyOrder(persistedGroupA.id(),
				persistedGroupB.id());
	}

}
