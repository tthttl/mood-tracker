package com.moodtracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("moodtracker")
public record MoodTrackerProperties(String frontendBaseUrl, Mail mail) {

	public record Mail(String provider, String from) {
	}

}
