package com.moodtracker.application;

import com.moodtracker.domain.Email;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.Member;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.exception.NoOpenRoundException;
import com.moodtracker.domain.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

	@Mock
	private GroupRepository groupRepository;

	private RoundService roundService;
	private Group group;

	@BeforeEach
	void setUp() {
		roundService = new RoundService(groupRepository);
		group = Group.of("group-1", new Name("Team A"), new MoodRange(1),
				List.of(new Member(new Email("a@test.com")), new Member(new Email("b@test.com"))), List.of());
	}

	@Test
	void startRoundStartsAndSavesTheGroupsRound() {
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
		when(groupRepository.save(group)).thenReturn(group);

		Round round = roundService.startRound("group-1", "a@test.com");

		assertThat(round.status()).isEqualTo(Round.Status.OPEN);
		assertThat(group.currentRound()).contains(round);
		verify(groupRepository).save(group);
	}

	@Test
	void startRoundThrowsWhenGroupDoesNotExist() {
		when(groupRepository.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roundService.startRound("missing", "a@test.com"))
				.isInstanceOf(GroupNotFoundException.class);
	}

	@Test
	void submitMoodRecordsTheSubmissionOnTheOpenRound() {
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
		group.startRound("a@test.com");

		Round round = roundService.submitMood("group-1", "a@test.com", 1);

		assertThat(round.submittedMemberEmails()).containsExactly("a@test.com");
		verify(groupRepository).save(group);
	}

	@Test
	void submitMoodThrowsWhenNoRoundIsOpen() {
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));

		assertThatThrownBy(() -> roundService.submitMood("group-1", "a@test.com", 1))
				.isInstanceOf(NoOpenRoundException.class);
	}

	@Test
	void historyReturnsResultsForClosedRoundsOnly() {
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
		Round round = group.startRound("a@test.com");
		round.submit("a@test.com", -1);
		round.submit("b@test.com", 1);

		List<RoundResult> history = roundService.history("group-1");

		assertThat(history).hasSize(1);
		assertThat(history.get(0).average()).isEqualTo(0.0);
	}

}
