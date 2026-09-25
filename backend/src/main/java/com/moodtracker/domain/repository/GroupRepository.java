package com.moodtracker.domain.repository;

import com.moodtracker.domain.Group;

import java.util.List;
import java.util.Optional;

public interface GroupRepository {

	Group save(Group group);

	Optional<Group> findById(String id);

	List<Group> findAll();

}
