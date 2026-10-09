package com.moodtracker.application;

import com.moodtracker.domain.Group;
import com.moodtracker.domain.Member;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.event.RoundClosed;
import com.moodtracker.domain.event.RoundStarted;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.exception.NoOpenRoundException;
import com.moodtracker.domain.repository.GroupRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoundService {

	private final GroupRepository groupRepository;
	private final ApplicationEventPublisher events;

	public RoundService(GroupRepository groupRepository, ApplicationEventPublisher events) {
		this.groupRepository = groupRepository;
		this.events = events;
	}

	@Transactional
	public Round startRound(String groupId, String startedByEmail) {
		Group group = getGroupOrThrow(groupId);
		group.startRound(startedByEmail);
		Group saved = groupRepository.save(group);
		Round round = saved.currentRound().orElseThrow();
		List<String> everyoneElse = saved.memberEmails().stream()
				.filter(email -> !email.equals(startedByEmail))
				.sorted()
				.toList();
		events.publishEvent(new RoundStarted(saved.id(), saved.name().value(), round.id(), startedByEmail, everyoneElse));
		return round;
	}

	@Transactional
	public Round submitMood(String groupId, String email, int value) {
		Group group = getGroupOrThrow(groupId);
		Round round = group.currentRound().orElseThrow(() -> new NoOpenRoundException(groupId));
		round.submit(email, value);
		groupRepository.save(group);
		if (round.status() == Round.Status.CLOSED) {
			events.publishEvent(new RoundClosed(group.id(), group.name().value(), round.result(),
					participantsWhoWantToBeNotified(group, round)));
		}
		return round;
	}

	@Transactional(readOnly = true)
	public Optional<Round> currentRound(String groupId) {
		return getGroupOrThrow(groupId).currentRound();
	}

	@Transactional(readOnly = true)
	public List<RoundResult> history(String groupId) {
		return getGroupOrThrow(groupId).closedRounds().stream()
				.map(round -> round.result())
				.toList();
	}

	private static List<String> participantsWhoWantToBeNotified(Group group, Round round) {
		return group.members().stream()
				.filter(Member::notifyOnRoundComplete)
				.map(member -> member.email().value())
				.filter(round.eligibleMemberEmails()::contains)
				.sorted()
				.toList();
	}

	private Group getGroupOrThrow(String groupId) {
		return groupRepository.findById(groupId)
				.orElseThrow(() -> new GroupNotFoundException(groupId));
	}

}
