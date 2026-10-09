package com.moodtracker.infrastructure.mail;

import com.moodtracker.config.MoodTrackerProperties;
import com.moodtracker.domain.notification.Message;
import com.moodtracker.domain.notification.MessageSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends plain-text mail through the SMTP server configured under {@code spring.mail.*}. */
@Component
@ConditionalOnProperty(name = "moodtracker.mail.provider", havingValue = "smtp")
public class SmtpMessageSender implements MessageSender {

	private final JavaMailSender mailSender;
	private final String from;

	public SmtpMessageSender(JavaMailSender mailSender, MoodTrackerProperties properties) {
		this.mailSender = mailSender;
		this.from = properties.mail().from();
	}

	@Override
	public void send(String recipientEmail, Message message) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(from);
		mail.setTo(recipientEmail);
		mail.setSubject(message.subject());
		mail.setText(message.body());
		mailSender.send(mail);
	}

}
