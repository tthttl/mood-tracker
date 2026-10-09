package com.moodtracker.infrastructure.mail;

import com.moodtracker.domain.notification.Message;
import com.moodtracker.domain.notification.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "moodtracker.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingMessageSender implements MessageSender {
	private static final Logger log = LoggerFactory.getLogger(LoggingMessageSender.class);

	@Override
	public void send(String recipientEmail, Message message) {
		log.info("Message to {} | {}\n{}", recipientEmail, message.subject(), message.body());
	}

}
