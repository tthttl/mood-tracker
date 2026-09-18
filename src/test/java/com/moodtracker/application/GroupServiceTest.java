package com.moodtracker.application;

import com.moodtracker.domain.Email;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.Member;
import com.moodtracker.domain.MoodRange;
import com.moodtracker.domain.Name;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

	@Mock
	private GroupRepository groupRepository;

	private GroupService groupService;

	@BeforeEach
	void setUp() {
		groupService = new GroupService(groupRepository);
	}

	@Test
	void createGroupBuildsAndSavesANewGroup() {
		when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Group created = groupService.createGroup("Team A", 1, List.of("a@test.com", "b@test.com"));

		assertThat(created.name()).isEqualTo(new Name("Team A"));
		assertThat(created.moodRange()).isEqualTo(new MoodRange(1));
		assertThat(created.memberEmails()).containsExactlyInAnyOrder("a@test.com", "b@test.com");

		ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
		verify(groupRepository).save(captor.capture());
		assertThat(captor.getValue()).isSameAs(created).isEqualTo(created);
	}

	@Test
	void addMemberLoadsMutatesAndSavesTheGroup() {
		Group existing = Group.of("group-1", new Name("Team A"), new MoodRange(1),
				List.of(new Member(new Email("a@test.com"))), List.of());
		when(groupRepository.findById("group-1")).thenReturn(Optional.of(existing));
		when(groupRepository.save(existing)).thenReturn(existing);

		Group updated = groupService.addMember("group-1", "b@test.com");

		assertThat(updated.memberEmails()).containsExactlyInAnyOrder("a@test.com", "b@test.com");
		verify(groupRepository).save(existing);
	}

	@Test
	void addMemberThrowsWhenGroupDoesNotExist() {
		when(groupRepository.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> groupService.addMember("missing", "a@test.com"))
				.isInstanceOf(GroupNotFoundException.class);
	}

	@Test
	void getGroupThrowsWhenGroupDoesNotExist() {
		when(groupRepository.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> groupService.getGroup("missing"))
				.isInstanceOf(GroupNotFoundException.class);
	}

}
