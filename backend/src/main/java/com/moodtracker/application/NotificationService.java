package com.moodtracker.application;

import com.moodtracker.config.MoodTrackerProperties;
import com.moodtracker.domain.event.RoundClosed;
import com.moodtracker.domain.event.RoundStarted;
import com.moodtracker.domain.notification.Message;
import com.moodtracker.domain.notification.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Turns round events into notifications. Runs after the transaction has committed and off the
 * request thread, so a slow or failing mail server can never affect the API call that caused it.
 */
@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final MessageSender messageSender;
	private final String frontendBaseUrl;

	public NotificationService(MessageSender messageSender, MoodTrackerProperties properties) {
		this.messageSender = messageSender;
		this.frontendBaseUrl = properties.frontendBaseUrl();
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRoundStarted(RoundStarted event) {
		notifyAll(event.recipients(), NotificationMessages.roundStarted(event, frontendBaseUrl));
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRoundClosed(RoundClosed event) {
		notifyAll(event.recipients(), NotificationMessages.roundClosed(event, frontendBaseUrl));
	}

	private void notifyAll(List<String> recipients, Message message) {
		for (String recipient : recipients) {
			try {
				messageSender.send(recipient, message);
			} catch (RuntimeException e) {
				log.warn("Could not notify {}", recipient, e);
			}
		}
	}

}
