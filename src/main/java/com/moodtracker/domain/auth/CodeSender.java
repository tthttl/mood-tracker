package com.moodtracker.domain.auth;

public interface CodeSender {

	void send(String email, String code);

}
