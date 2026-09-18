package com.moodtracker.domain;

import com.moodtracker.domain.exception.DuplicateMemberException;
import com.moodtracker.domain.exception.InvalidGroupConfigurationException;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundAlreadyOpenException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupTest {

	private static final Name NAME = new Name("Team A");
	private static final MoodRange MOOD_RANGE = new MoodRange(1);
	private static final List<String> TWO_MEMBERS = List.of("a@test.com", "b@test.com");

	private static Group createGroup(List<String> memberEmails) {
		return new Group("g1", NAME, MOOD_RANGE, memberEmails);
	}

	private static Group defaultGroup() {
		return createGroup(TWO_MEMBERS);
	}

	@Test
	void rejectsNoMembers() {
		assertThatThrownBy(() -> createGroup(List.of()))
				.isInstanceOf(InvalidGroupConfigurationException.class)
				.hasMessage("A group needs at least one member");
	}

	@Test
	void rejectsDuplicateMemberEmailsAtConstruction() {
		assertThatThrownBy(() -> createGroup(List.of("a@test.com", "a@test.com")))
				.isInstanceOf(DuplicateMemberException.class);
	}

	@Test
	void startRoundByNonMemberThrows() {
		Group group = defaultGroup();

		assertThatThrownBy(() -> group.startRound("stranger@test.com"))
				.isInstanceOf(NotAGroupMemberException.class);
	}

	@Test
	void startRoundByMemberSucceeds() {
		Group group = defaultGroup();

		Round round = group.startRound("a@test.com");

		assertThat(round.status()).isEqualTo(Round.Status.OPEN);
		assertThat(group.currentRound()).contains(round);
	}

	@Test
	void startRoundWhileAlreadyOpenThrows() {
		Group group = defaultGroup();
		group.startRound("a@test.com");

		assertThatThrownBy(() -> group.startRound("b@test.com"))
				.isInstanceOf(RoundAlreadyOpenException.class);
	}

	@Test
	void newRoundCanStartAfterPreviousOneAutoCloses() {
		Group group = defaultGroup();
		Round first = group.startRound("a@test.com");
		first.submit("a@test.com", 0);
		first.submit("b@test.com", 0);

		Round second = group.startRound("b@test.com");

		assertThat(second).isNotSameAs(first);
		assertThat(group.currentRound()).contains(second);
		assertThat(group.closedRounds()).containsExactly(first);
	}

	@Test
	void addMemberRejectsBlankEmail() {
		Group group = defaultGroup();

		assertThatThrownBy(() -> group.addMember(" "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void addMemberRejectsDuplicateEmail() {
		Group group = defaultGroup();

		assertThatThrownBy(() -> group.addMember("a@test.com"))
				.isInstanceOf(DuplicateMemberException.class);
	}

	@Test
	void addMemberAddsNewEligibleMemberWithNotificationsOnByDefault() {
		Group group = defaultGroup();

		group.addMember("c@test.com");

		assertThat(group.memberEmails()).contains("c@test.com");
		assertThat(group.members())
				.filteredOn(member -> member.email().value().equals("c@test.com"))
				.singleElement()
				.satisfies(member -> assertThat(member.notifyOnRoundComplete()).isTrue());
	}

	@Test
	void closedRoundsExcludeTheOpenRoundAndCurrentRoundIsEmptyOnceAllClosed() {
		Group group = defaultGroup();
		Round round = group.startRound("a@test.com");

		assertThat(group.closedRounds()).isEmpty();

		round.submit("a@test.com", 0);
		round.submit("b@test.com", 0);

		assertThat(group.currentRound()).isEmpty();
		assertThat(group.closedRounds()).containsExactly(round);
	}

}
