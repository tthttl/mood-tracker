package com.moodtracker.application;

import com.moodtracker.domain.auth.CodeSender;
import com.moodtracker.domain.auth.VerificationCode;
import com.moodtracker.domain.auth.VerificationCodeStore;
import com.moodtracker.domain.auth.exception.InvalidVerificationCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private VerificationCodeStore store;

	@Mock
	private CodeSender codeSender;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(store, codeSender);
	}

	@Test
	void requestCodeSavesAndSendsASixDigitCode() {
		authService.requestCode("a@test.com");

		ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
		verify(store).save(captor.capture());
		assertThat(captor.getValue().email()).isEqualTo("a@test.com");

		ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
		verify(codeSender).send(eq("a@test.com"), codeCaptor.capture());
		assertThat(codeCaptor.getValue()).hasSize(6).containsOnlyDigits();
	}

	@Test
	void verifyCodeSucceedsAndPersistsTheUsedCode() {
		VerificationCode code = VerificationCode.issue("a@test.com", "123456",
				Instant.now().plus(10, ChronoUnit.MINUTES));
		when(store.find("a@test.com")).thenReturn(Optional.of(code));

		authService.verifyCode("a@test.com", "123456");

		verify(store, times(1)).save(code);
	}

	@Test
	void verifyCodeThrowsWhenNoCodeIsOnFileForEmail() {
		when(store.find("missing@test.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.verifyCode("missing@test.com", "123456"))
				.isInstanceOf(InvalidVerificationCodeException.class);

		verify(store, never()).save(any());
	}

	@Test
	void verifyCodeThrowsAndDoesNotSaveWhenCodeIsWrong() {
		VerificationCode code = VerificationCode.issue("a@test.com", "123456",
				Instant.now().plus(10, ChronoUnit.MINUTES));
		when(store.find("a@test.com")).thenReturn(Optional.of(code));

		assertThatThrownBy(() -> authService.verifyCode("a@test.com", "000000"))
				.isInstanceOf(InvalidVerificationCodeException.class);

		verify(store, never()).save(code);
	}

}
