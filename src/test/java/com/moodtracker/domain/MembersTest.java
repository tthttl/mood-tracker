package com.moodtracker.domain;

import com.moodtracker.domain.exception.DuplicateMemberException;
import com.moodtracker.domain.exception.InvalidGroupConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembersTest {

	@Test
	void ofRejectsNullOrEmptyEmailList() {
		assertThatThrownBy(() -> Members.of(null))
				.isInstanceOf(InvalidGroupConfigurationException.class)
				.hasMessage("A group needs at least one member");
		assertThatThrownBy(() -> Members.of(List.of()))
				.isInstanceOf(InvalidGroupConfigurationException.class)
				.hasMessage("A group needs at least one member");
	}

	@Test
	void ofRejectsDuplicateEmails() {
		assertThatThrownBy(() -> Members.of(List.of("a@test.com", "a@test.com")))
				.isInstanceOf(DuplicateMemberException.class);
	}

	@Test
	void ofBuildsMembersWithDefaultNotificationPreference() {
		Members members = Members.of(List.of("a@test.com", "b@test.com"));

		assertThat(members.emails()).containsExactlyInAnyOrder("a@test.com", "b@test.com");
		assertThat(members.all()).allSatisfy(member -> assertThat(member.notifyOnRoundComplete()).isTrue());
	}

	@Test
	void fromRejectsNullOrEmptyCollection() {
		assertThatThrownBy(() -> Members.from(null))
				.isInstanceOf(InvalidGroupConfigurationException.class);
		assertThatThrownBy(() -> Members.from(List.of()))
				.isInstanceOf(InvalidGroupConfigurationException.class);
	}

	@Test
	void fromRejectsDuplicateEmails() {
		assertThatThrownBy(() -> Members.from(List.of(
				new Member(new Email("a@test.com")),
				new Member(new Email("a@test.com"), false))))
				.isInstanceOf(DuplicateMemberException.class);
	}

	@Test
	void fromPreservesEachMembersNotificationPreference() {
		Members members = Members.from(List.of(
				new Member(new Email("a@test.com"), false),
				new Member(new Email("b@test.com"), true)));

		assertThat(members.all())
				.filteredOn(member -> member.email().value().equals("a@test.com"))
				.singleElement()
				.satisfies(member -> assertThat(member.notifyOnRoundComplete()).isFalse());
	}

	@Test
	void addRejectsDuplicateEmail() {
		Members members = Members.of(List.of("a@test.com"));

		assertThatThrownBy(() -> members.add("a@test.com"))
				.isInstanceOf(DuplicateMemberException.class);
	}

	@Test
	void addRejectsBlankEmail() {
		Members members = Members.of(List.of("a@test.com"));

		assertThatThrownBy(() -> members.add(" "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void addAddsANewMember() {
		Members members = Members.of(List.of("a@test.com"));

		members.add("b@test.com");

		assertThat(members.emails()).containsExactlyInAnyOrder("a@test.com", "b@test.com");
		assertThat(members.contains("b@test.com")).isTrue();
	}

}
