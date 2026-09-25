package com.moodtracker.infrastructure.auth;

import com.moodtracker.domain.auth.CodeSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs the verification code instead of sending a real email. No SMTP host is configured
 * yet — swap in a JavaMailSender-backed CodeSender once one is.
 */
@Component
public class LoggingCodeSender implements CodeSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingCodeSender.class);

	@Override
	public void send(String email, String code) {
		log.info("Verification code for {}: {}", email, code);
	}

}
