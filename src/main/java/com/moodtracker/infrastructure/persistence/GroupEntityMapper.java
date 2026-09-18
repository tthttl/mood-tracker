package com.moodtracker.infrastructure.persistence;

import com.moodtracker.domain.Email;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.Member;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.Round;
import com.moodtracker.infrastructure.persistence.entity.GroupJpaEntity;
import com.moodtracker.infrastructure.persistence.entity.MemberEmbeddable;
import com.moodtracker.infrastructure.persistence.entity.RoundJpaEntity;
import com.moodtracker.infrastructure.persistence.entity.SubmissionEmbeddable;

import java.util.ArrayList;
import java.util.List;

final class GroupEntityMapper {

	private GroupEntityMapper() {
	}

	static GroupJpaEntity toEntity(Group group) {
		List<MemberEmbeddable> members = group.members().stream()
				.map(member -> new MemberEmbeddable(member.email().value(), member.notifyOnRoundComplete()))
				.toList();
		List<RoundJpaEntity> rounds = group.rounds().stream()
				.map(GroupEntityMapper::toEntity)
				.toList();
		return new GroupJpaEntity(group.id(), group.name().value(), group.moodRange().value(),
				new ArrayList<>(members), new ArrayList<>(rounds));
	}

	private static RoundJpaEntity toEntity(Round round) {
		List<SubmissionEmbeddable> submissions = round.submissionsForPersistence().stream()
				.map(s -> new SubmissionEmbeddable(s.email(), s.value(), s.submittedAt()))
				.toList();
		return new RoundJpaEntity(round.id(), round.moodRange().value(), round.eligibleMemberEmails(),
				round.startedBy(), round.startedAt(), round.closedAt(), new ArrayList<>(submissions));
	}

	static Group toDomain(GroupJpaEntity entity) {
		List<Member> members = entity.getMembers().stream()
				.map(m -> new Member(new Email(m.getEmail()), m.isNotifyOnRoundComplete()))
				.toList();
		List<Round> rounds = entity.getRounds().stream()
				.map(round -> toDomain(round, entity.getId()))
				.toList();
		return Group.reconstitute(entity.getId(), new Name(entity.getName()), new MoodRange(entity.getMoodRange()),
				members, rounds);
	}

	private static Round toDomain(RoundJpaEntity entity, String groupId) {
		List<Round.SubmissionRecord> submissions = entity.getSubmissions().stream()
				.map(s -> new Round.SubmissionRecord(s.getEmail(), s.getValue(), s.getSubmittedAt()))
				.toList();
		return Round.reconstitute(entity.getId(), groupId, new MoodRange(entity.getMoodRange()),
				entity.getEligibleMemberEmails(), entity.getStartedBy(), entity.getStartedAt(),
				entity.getClosedAt(), submissions);
	}

}
