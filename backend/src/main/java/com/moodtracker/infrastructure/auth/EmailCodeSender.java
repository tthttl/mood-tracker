package com.moodtracker.infrastructure.auth;

import com.moodtracker.domain.auth.CodeSender;
import com.moodtracker.domain.notification.Message;
import com.moodtracker.domain.notification.MessageSender;
import org.springframework.stereotype.Component;

/**
 * Delivers verification codes through whichever {@link MessageSender} is active. Sent
 * synchronously on purpose: if delivery fails the caller must know the code never left.
 */
@Component
public class EmailCodeSender implements CodeSender {

	private final MessageSender messageSender;

	public EmailCodeSender(MessageSender messageSender) {
		this.messageSender = messageSender;
	}

	@Override
	public void send(String email, String code) {
		messageSender.send(email, new Message("Your Mood Tracker verification code",
				"Your verification code is " + code + ".\n\nIt expires shortly and can be used once.\n"));
	}

}
