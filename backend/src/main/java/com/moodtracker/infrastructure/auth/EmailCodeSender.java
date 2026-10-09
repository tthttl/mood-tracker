package com.moodtracker.infrastructure.auth;

import com.moodtracker.domain.auth.CodeSender;
import com.moodtracker.domain.notification.MessageSender;
import com.moodtracker.domain.notification.MessageTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailCodeSender implements CodeSender {
	private static final MessageTemplate TEMPLATE = MessageTemplate.fromResource("verification-code");

	private final MessageSender messageSender;

	public EmailCodeSender(MessageSender messageSender) {
		this.messageSender = messageSender;
	}

	@Override
	public void send(String email, String code) {
		messageSender.send(email, TEMPLATE.render(Map.of("code", code)));
	}

}
