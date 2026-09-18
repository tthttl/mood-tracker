package com.moodtracker.api;

import com.moodtracker.api.generated.model.GroupResponse;
import com.moodtracker.api.generated.model.RoundHistoryResponse;
import com.moodtracker.api.generated.model.RoundResultResponse;
import com.moodtracker.api.generated.model.RoundStatusResponse;
import com.moodtracker.domain.Group;
import com.moodtracker.domain.Round;
import com.moodtracker.domain.RoundResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

final class ApiMapper {

	private ApiMapper() {
	}

	static GroupResponse toGroupResponse(Group group) {
		return new GroupResponse(group.id(), group.name().value(), group.moodRange().value(),
				group.memberEmails().stream().sorted().toList());
	}

	static RoundStatusResponse toRoundStatusResponse(Round round) {
		List<String> submitted = round.submittedMemberEmails().stream().sorted().toList();
		List<String> pending = round.eligibleMemberEmails().stream()
				.filter(email -> !round.submittedMemberEmails().contains(email))
				.sorted()
				.toList();
		RoundStatusResponse response = new RoundStatusResponse(round.id(),
				RoundStatusResponse.StatusEnum.valueOf(round.status().name()), round.startedBy(),
				toOffsetDateTime(round.startedAt()), submitted, pending);
		if (round.status() == Round.Status.CLOSED) {
			response.closedAt(toOffsetDateTime(round.closedAt()));
			response.result(toRoundResultResponse(round.result()));
		}
		return response;
	}

	static RoundResultResponse toRoundResultResponse(RoundResult result) {
		return new RoundResultResponse(result.roundId(), toOffsetDateTime(result.closedAt()), result.min(),
				result.max(), result.average(), result.submissionCount());
	}

	static RoundHistoryResponse toRoundHistoryResponse(List<RoundResult> results) {
		return new RoundHistoryResponse(results.stream().map(ApiMapper::toRoundResultResponse).toList());
	}

	private static OffsetDateTime toOffsetDateTime(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

}
