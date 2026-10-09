package com.moodtracker.application;

import com.moodtracker.domain.Email;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.Member;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.event.RoundClosed;
import com.moodtracker.domain.event.RoundStarted;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.exception.NoOpenRoundException;
import com.moodtracker.domain.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

	@Mock
	private GroupRepository groupRepository;

	@Mock
	private ApplicationEventPublisher events;

	private RoundService roundService;
	private Group group;

	@BeforeEach
	void setUp() {
		roundService = new RoundService(groupRepository, events);
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

	@Test
	void startRoundNotifiesEveryoneExceptTheStarter() {
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
		when(groupRepository.save(group)).thenReturn(group);

		roundService.startRound("group-1", "a@test.com");

		ArgumentCaptor<RoundStarted> captor = ArgumentCaptor.forClass(RoundStarted.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().groupId()).isEqualTo("group-1");
		assertThat(captor.getValue().groupName()).isEqualTo("Team A");
		assertThat(captor.getValue().startedBy()).isEqualTo("a@test.com");
		assertThat(captor.getValue().recipients()).containsExactly("b@test.com");
	}

	@Test
	void aFailedStartPublishesNothing() {
		when(groupRepository.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roundService.startRound("missing", "a@test.com"))
				.isInstanceOf(GroupNotFoundException.class);

		verifyNoInteractions(events);
	}

	@Test
	void aSubmissionThatLeavesTheRoundOpenPublishesNothing() {
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
		group.startRound("a@test.com");

		roundService.submitMood("group-1", "a@test.com", 1);

		verify(events, never()).publishEvent(any(RoundClosed.class));
	}

	@Test
	void theSubmissionThatClosesTheRoundNotifiesParticipantsWhoOptedIn() {
		Group mixed = Group.of("group-1", new Name("Team A"), new MoodRange(1),
				List.of(new Member(new Email("a@test.com")), new Member(new Email("b@test.com"), false),
						new Member(new Email("c@test.com"))),
				List.of());
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(mixed));
		mixed.startRound("a@test.com");
		roundService.submitMood("group-1", "a@test.com", -1);
		roundService.submitMood("group-1", "b@test.com", 0);

		roundService.submitMood("group-1", "c@test.com", 1);

		ArgumentCaptor<RoundClosed> captor = ArgumentCaptor.forClass(RoundClosed.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().groupName()).isEqualTo("Team A");
		assertThat(captor.getValue().result().average()).isEqualTo(0.0);
		assertThat(captor.getValue().recipients()).containsExactly("a@test.com", "c@test.com");
	}

	@Test
	void membersAddedAfterTheRoundStartedAreNotNotifiedOfItsResult() {
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
		group.startRound("a@test.com");
		group.addMember("late@test.com");
		roundService.submitMood("group-1", "a@test.com", 1);

		roundService.submitMood("group-1", "b@test.com", 1);

		ArgumentCaptor<RoundClosed> captor = ArgumentCaptor.forClass(RoundClosed.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().recipients()).containsExactly("a@test.com", "b@test.com");
	}

}
