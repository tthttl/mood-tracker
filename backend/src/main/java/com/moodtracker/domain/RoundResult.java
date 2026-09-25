package com.moodtracker.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.IntSummaryStatistics;
import java.util.NoSuchElementException;

public record RoundResult(String roundId, int min, int max, double average,
		int submissionCount, Instant closedAt) {

	static RoundResult from(String roundId, Collection<Integer> values, Instant closedAt) {

		if (values.isEmpty()) {
			throw new NoSuchElementException("Values list cannot be empty");
		}

		IntSummaryStatistics stats = values.stream()
				.mapToInt(i -> i)
				.summaryStatistics();

		return new RoundResult(roundId, stats.getMin(), stats.getMax(), stats.getAverage(), values.size(), closedAt);
	}

}
