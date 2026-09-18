package com.moodtracker.domain;

import java.time.Instant;

public record Submission(String email, int value, Instant submittedAt) {

}
