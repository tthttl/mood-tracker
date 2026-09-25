package com.moodtracker.domain;

import com.moodtracker.domain.exception.InvalidGroupConfigurationException;
import com.moodtracker.domain.exception.InvalidMoodValueException;

public record MoodRange(int value) {
    public MoodRange {
        if (value <= 0) {
            throw new InvalidGroupConfigurationException("Mood range must be > 0");
        }
    }

    public void validate(int value) {
        if (value < -this.value || value > this.value) {
            throw new InvalidMoodValueException(value, this.value);
        }
    }
}
