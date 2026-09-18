package com.moodtracker.application;

import com.moodtracker.domain.Group;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

	private final GroupRepository groupRepository;

	public GroupService(GroupRepository groupRepository) {
		this.groupRepository = groupRepository;
	}

	@Transactional
	public Group createGroup(String name, int moodRange, List<String> memberEmails) {
		Group group = new Group(new Name(name), new MoodRange(moodRange), memberEmails);
		return groupRepository.save(group);
	}

	@Transactional
	public Group addMember(String groupId, String email) {
		Group group = getGroupOrThrow(groupId);
		group.addMember(email);
		return groupRepository.save(group);
	}

	@Transactional(readOnly = true)
	public Group getGroup(String groupId) {
		return getGroupOrThrow(groupId);
	}

	@Transactional(readOnly = true)
	public List<Group> listGroups() {
		return groupRepository.findAll();
	}

	private Group getGroupOrThrow(String groupId) {
		return groupRepository.findById(groupId)
				.orElseThrow(() -> new GroupNotFoundException(groupId));
	}

}
