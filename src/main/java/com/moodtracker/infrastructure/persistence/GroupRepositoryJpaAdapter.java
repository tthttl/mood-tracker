package com.moodtracker.infrastructure.persistence;

import com.moodtracker.domain.Group;
import com.moodtracker.domain.repository.GroupRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class GroupRepositoryJpaAdapter implements GroupRepository {

	private final SpringDataGroupJpaRepository jpaRepository;

	GroupRepositoryJpaAdapter(SpringDataGroupJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Group save(Group group) {
		var saved = jpaRepository.save(GroupEntityMapper.toEntity(group));
		return GroupEntityMapper.toDomain(saved);
	}

	@Override
	public Optional<Group> findById(String id) {
		return jpaRepository.findById(id).map(GroupEntityMapper::toDomain);
	}

	@Override
	public List<Group> findAll() {
		return jpaRepository.findAll().stream().map(GroupEntityMapper::toDomain).toList();
	}

}
