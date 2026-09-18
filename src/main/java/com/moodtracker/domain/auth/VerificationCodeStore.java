package com.moodtracker.domain.auth;

import java.util.Optional;

public interface VerificationCodeStore {

	void save(VerificationCode verificationCode);

	Optional<VerificationCode> find(String email);

}
