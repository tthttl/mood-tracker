package com.moodtracker.infrastructure.mail;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.moodtracker.config.MoodTrackerProperties;
import com.moodtracker.domain.notification.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmtpMessageSenderTest {

	@RegisterExtension
	static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort());

	private static SmtpMessageSender senderTo(int port) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("localhost");
		mailSender.setPort(port);
		mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "2000");
		return new SmtpMessageSender(mailSender,
				new MoodTrackerProperties("http://localhost:4200", new MoodTrackerProperties.Mail("smtp", "noreply@mood.test")));
	}

	@Test
	void deliversAPlainTextMailOverARealSmtpConnection() throws Exception {
		senderTo(GREEN_MAIL.getSmtp().getPort()).send("bob@x.com", new Message("Hello Bob", "Line one\nLine two"));

		assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
		MimeMessage received = GREEN_MAIL.getReceivedMessages()[0];
		assertThat(received.getSubject()).isEqualTo("Hello Bob");
		assertThat(received.getFrom()[0].toString()).isEqualTo("noreply@mood.test");
		assertThat(received.getAllRecipients()[0].toString()).isEqualTo("bob@x.com");
		assertThat(GreenMailUtil.getBody(received)).contains("Line one").contains("Line two");
	}

	@Test
	void anUnreachableServerSurfacesAsAnException() {
		assertThatThrownBy(() -> senderTo(1).send("bob@x.com", new Message("s", "b")))
				.isInstanceOf(MailSendException.class);
	}

}
