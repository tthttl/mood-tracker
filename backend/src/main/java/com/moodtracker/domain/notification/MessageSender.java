package com.moodtracker.domain.notification;

public interface MessageSender {

	void send(String recipientEmail, Message message);

}
