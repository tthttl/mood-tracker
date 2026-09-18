package com.moodtracker.api;

import com.moodtracker.api.generated.RoundsApi;
import com.moodtracker.api.generated.model.RoundHistoryResponse;
import com.moodtracker.api.generated.model.RoundStatusResponse;
import com.moodtracker.api.generated.model.StartRoundRequest;
import com.moodtracker.api.generated.model.SubmitMoodRequest;
import com.moodtracker.application.AuthService;
import com.moodtracker.application.RoundService;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.exception.NoOpenRoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
class RoundsController implements RoundsApi {

	private final RoundService roundService;
	private final AuthService authService;

	RoundsController(RoundService roundService, AuthService authService) {
		this.roundService = roundService;
		this.authService = authService;
	}

	@Override
	public ResponseEntity<RoundStatusResponse> startRound(String groupId, StartRoundRequest request) {
		authService.verifyCode(request.getEmail(), request.getCode());
		Round round = roundService.startRound(groupId, request.getEmail());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/current").build().toUri();
		return ResponseEntity.created(location).body(ApiMapper.toRoundStatusResponse(round));
	}

	@Override
	public ResponseEntity<RoundStatusResponse> getCurrentRound(String groupId) {
		Round round = roundService.currentRound(groupId)
				.orElseThrow(() -> new NoOpenRoundException(groupId));
		return ResponseEntity.ok(ApiMapper.toRoundStatusResponse(round));
	}

	@Override
	public ResponseEntity<RoundStatusResponse> submitMood(String groupId, SubmitMoodRequest request) {
		authService.verifyCode(request.getEmail(), request.getCode());
		Round round = roundService.submitMood(groupId, request.getEmail(), request.getValue());
		return ResponseEntity.ok(ApiMapper.toRoundStatusResponse(round));
	}

	@Override
	public ResponseEntity<RoundHistoryResponse> getRoundHistory(String groupId) {
		return ResponseEntity.ok(ApiMapper.toRoundHistoryResponse(roundService.history(groupId)));
	}

}
