package com.moodtracker.domain;

public record Name(String value) {
    public Name {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
    }
}
