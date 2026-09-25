package com.moodtracker.infrastructure.auth;

import com.moodtracker.domain.auth.VerificationCode;
import com.moodtracker.domain.auth.VerificationCodeStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryVerificationCodeStore implements VerificationCodeStore {

	private final Map<String, VerificationCode> codesByEmail = new ConcurrentHashMap<>();

	@Override
	public void save(VerificationCode verificationCode) {
		codesByEmail.put(verificationCode.email(), verificationCode);
	}

	@Override
	public Optional<VerificationCode> find(String email) {
		return Optional.ofNullable(codesByEmail.get(email));
	}

}
