package com.moodtracker.api;

import com.moodtracker.api.generated.model.ErrorResponse;
import com.moodtracker.api.generated.model.FieldError;
import com.moodtracker.domain.auth.exception.InvalidVerificationCodeException;
import com.moodtracker.domain.exception.DuplicateMemberException;
import com.moodtracker.domain.exception.DuplicateSubmissionException;
import com.moodtracker.domain.exception.GroupNotFoundException;
import com.moodtracker.domain.exception.MoodTrackerException;
import com.moodtracker.domain.exception.NoOpenRoundException;
import com.moodtracker.domain.exception.NotAGroupMemberException;
import com.moodtracker.domain.exception.RoundAlreadyOpenException;
import com.moodtracker.domain.exception.RoundClosedException;
import com.moodtracker.domain.exception.RoundStillOpenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MoodTrackerException.class)
	ResponseEntity<Object> handleDomainException(MoodTrackerException ex) {
		return body(statusOf(ex), codeOf(ex), ex.getMessage(), null);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", null);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), String.valueOf(error.getDefaultMessage())))
				.toList();
		return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fieldErrors);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed", null);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		String code = status != null ? status.name() : "ERROR";
		return body(statusCode, code, ex.getMessage(), null);
	}

	private static ResponseEntity<Object> body(HttpStatusCode status, String code, String message,
			List<FieldError> fieldErrors) {
		ErrorResponse response = new ErrorResponse(status.value(), code, message).fieldErrors(fieldErrors);
		return ResponseEntity.status(status).body(response);
	}

	private static HttpStatus statusOf(MoodTrackerException ex) {
		return switch (ex) {
			case InvalidVerificationCodeException _ -> HttpStatus.UNAUTHORIZED;
			case NotAGroupMemberException _ -> HttpStatus.FORBIDDEN;
			case GroupNotFoundException _,NoOpenRoundException _ -> HttpStatus.NOT_FOUND;
			case RoundAlreadyOpenException _,RoundClosedException _,RoundStillOpenException _,DuplicateSubmissionException _,DuplicateMemberException _ ->
				HttpStatus.CONFLICT;
			// InvalidMoodValueException, InvalidGroupConfigurationException and any future
			// input rule
			default -> HttpStatus.BAD_REQUEST;
		};
	}

	private static String codeOf(MoodTrackerException ex) {
		return ex.getClass().getSimpleName()
				.replaceAll("Exception$", "")
				.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
				.replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
				.toUpperCase();
	}

}
