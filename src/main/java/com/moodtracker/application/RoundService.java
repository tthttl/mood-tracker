package com.moodtracker.application;

import com.moodtracker.domain.Group;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.exception.NoOpenRoundException;
import com.moodtracker.domain.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoundService {

	private final GroupRepository groupRepository;

	public RoundService(GroupRepository groupRepository) {
		this.groupRepository = groupRepository;
	}

	@Transactional
	public Round startRound(String groupId, String startedByEmail) {
		Group group = getGroupOrThrow(groupId);
		group.startRound(startedByEmail);
		Group saved = groupRepository.save(group);
		return saved.currentRound().orElseThrow();
	}

	@Transactional
	public Round submitMood(String groupId, String email, int value) {
		Group group = getGroupOrThrow(groupId);
		Round round = group.currentRound().orElseThrow(() -> new NoOpenRoundException(groupId));
		round.submit(email, value);
		groupRepository.save(group);
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

	private Group getGroupOrThrow(String groupId) {
		return groupRepository.findById(groupId)
				.orElseThrow(() -> new GroupNotFoundException(groupId));
	}

}
