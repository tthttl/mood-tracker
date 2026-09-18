package com.moodtracker.api;

import com.moodtracker.api.generated.GroupsApi;
import com.moodtracker.api.generated.model.AddMemberRequest;
import com.moodtracker.api.generated.model.CreateGroupRequest;
import com.moodtracker.api.generated.model.GroupResponse;
import com.moodtracker.application.AuthService;
import com.moodtracker.application.GroupService;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
class GroupsController implements GroupsApi {

	private static final int DEFAULT_MOOD_RANGE = 1;

	private final GroupService groupService;
	private final AuthService authService;

	GroupsController(GroupService groupService, AuthService authService) {
		this.groupService = groupService;
		this.authService = authService;
	}

	@Override
	public ResponseEntity<GroupResponse> createGroup(CreateGroupRequest request) {
		int moodRange = request.getMoodRange() != null ? request.getMoodRange() : DEFAULT_MOOD_RANGE;
		Group group = groupService.createGroup(request.getName(), moodRange, request.getMemberEmails());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{groupId}").buildAndExpand(group.id()).toUri();
		return ResponseEntity.created(location).body(ApiMapper.toGroupResponse(group));
	}

	@Override
	public ResponseEntity<GroupResponse> getGroup(String groupId) {
		return ResponseEntity.ok(ApiMapper.toGroupResponse(groupService.getGroup(groupId)));
	}

	@Override
	public ResponseEntity<GroupResponse> addMember(String groupId, AddMemberRequest request) {
		Group group = groupService.getGroup(groupId);
		authService.verifyCode(request.getRequesterEmail(), request.getCode());

		if (!group.memberEmails().contains(request.getRequesterEmail())) {
			throw new NotAGroupMemberException(request.getRequesterEmail());
		}

		Group updated = groupService.addMember(groupId, request.getNewMemberEmail());
		return ResponseEntity.ok(ApiMapper.toGroupResponse(updated));
	}

}
