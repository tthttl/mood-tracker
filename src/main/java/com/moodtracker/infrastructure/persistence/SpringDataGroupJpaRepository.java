package com.moodtracker.infrastructure.persistence;

import com.moodtracker.infrastructure.persistence.entity.GroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataGroupJpaRepository extends JpaRepository<GroupJpaEntity, String> {
}
