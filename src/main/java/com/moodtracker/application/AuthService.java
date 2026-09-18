package com.moodtracker.application;

import com.moodtracker.domain.auth.CodeSender;
import com.moodtracker.domain.auth.VerificationCode;
import com.moodtracker.domain.auth.VerificationCodeStore;
import com.moodtracker.domain.auth.exception.InvalidVerificationCodeException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

	private static final int CODE_LENGTH = 6;
	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final int BOUND = (int) Math.pow(10, CODE_LENGTH);

	private final VerificationCodeStore store;
	private final CodeSender codeSender;
	private final SecureRandom random = new SecureRandom();

	public AuthService(VerificationCodeStore store, CodeSender codeSender) {
		this.store = store;
		this.codeSender = codeSender;
	}

	public void requestCode(String email) {
		String code = generateCode();
		VerificationCode verificationCode = VerificationCode.issue(email, code, Instant.now().plus(CODE_TTL));
		store.save(verificationCode);
		codeSender.send(email, code);
	}

	public void verifyCode(String email, String code) {
		VerificationCode verificationCode = store.find(email)
				.orElseThrow(InvalidVerificationCodeException::new);
		verificationCode.verify(code);
		store.save(verificationCode);
	}

	private String generateCode() {
		return String.format("%0" + CODE_LENGTH + "d", random.nextInt(BOUND));
	}

}
