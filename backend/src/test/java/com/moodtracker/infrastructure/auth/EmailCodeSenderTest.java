package com.moodtracker.infrastructure.auth;

import com.moodtracker.domain.notification.Message;
import com.moodtracker.domain.notification.MessageSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailCodeSenderTest {

	@Test
	void sendsTheCodeInTheVerificationMail() {
		MessageSender messageSender = mock(MessageSender.class);

		new EmailCodeSender(messageSender).send("ann@x.com", "042917");

		ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
		verify(messageSender).send(eq("ann@x.com"), message.capture());
		assertThat(message.getValue().subject()).isEqualTo("Your Mood Tracker verification code");
		assertThat(message.getValue().body()).contains("Your verification code is 042917.");
	}

}
