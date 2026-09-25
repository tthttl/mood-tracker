package com.moodtracker.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

	@Test
	void createsMemberWithNotificationsOnByDefault() {
		Email email = new Email("user@test.com");
		Member member = new Member(email);

		assertThat(member.email()).isEqualTo(email);
		assertThat(member.notifyOnRoundComplete()).isTrue();
	}

	@Test
	void createsMemberWithCustomNotificationSetting() {
		Email email = new Email("user@test.com");
		Member member = new Member(email, false);

		assertThat(member.email()).isEqualTo(email);
		assertThat(member.notifyOnRoundComplete()).isFalse();
	}

	@Test
	void membersWithSameEmailAreEqual() {
		Member member1 = new Member(new Email("user@test.com"), true);
		Member member2 = new Member(new Email("user@test.com"), false);

		assertThat(member1).isEqualTo(member2);
		assertThat(member1.hashCode()).isEqualTo(member2.hashCode());
	}

	@Test
	void membersWithDifferentEmailsAreNotEqual() {
		Member member1 = new Member(new Email("user1@test.com"));
		Member member2 = new Member(new Email("user2@test.com"));

		assertThat(member1).isNotEqualTo(member2);
	}

}
