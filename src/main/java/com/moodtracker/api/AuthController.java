package com.moodtracker.api;

import com.moodtracker.api.generated.AuthApi;
import com.moodtracker.api.generated.model.RequestVerificationCodeRequest;
import com.moodtracker.application.AuthService;
import com.moodtracker.application.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuthController implements AuthApi {

	private final GroupService groupService;
	private final AuthService authService;

	AuthController(GroupService groupService, AuthService authService) {
		this.groupService = groupService;
		this.authService = authService;
	}

	@Override
	public ResponseEntity<Void> requestVerificationCode(String groupId, RequestVerificationCodeRequest request) {
		boolean isMember = groupService.getGroup(groupId).memberEmails().contains(request.getEmail());
		if (isMember) {
			authService.requestCode(request.getEmail());
		}
		return ResponseEntity.noContent().build();
	}

}
