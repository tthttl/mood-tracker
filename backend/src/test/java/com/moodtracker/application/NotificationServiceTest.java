package com.moodtracker.application;

import com.moodtracker.config.MoodTrackerProperties;
import com.moodtracker.domain.RoundResult;
import com.moodtracker.domain.event.RoundClosed;
import com.moodtracker.domain.event.RoundStarted;
import com.moodtracker.domain.notification.Message;
import com.moodtracker.domain.notification.MessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private MessageSender messageSender;

	private NotificationService service;

	@BeforeEach
	void setUp() {
		service = new NotificationService(messageSender,
				new MoodTrackerProperties("http://localhost:4200", new MoodTrackerProperties.Mail("log", "from@x.com")));
	}

	@Test
	void roundStartedSendsOneMessagePerRecipient() {
		service.onRoundStarted(new RoundStarted("g-1", "Team", "r-1", "ann@x.com", List.of("bob@x.com", "cy@x.com")));

		ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
		verify(messageSender).send(eq("bob@x.com"), message.capture());
		verify(messageSender).send(eq("cy@x.com"), any(Message.class));
		assertThat(message.getValue().subject()).contains("Team");
	}

	@Test
	void roundClosedSendsTheResultToEveryRecipient() {
		service.onRoundClosed(new RoundClosed("g-1", "Team", new RoundResult("r-1", 0, 1, 0.5, 2, Instant.now()),
				List.of("ann@x.com", "bob@x.com")));

		ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
		verify(messageSender).send(eq("ann@x.com"), message.capture());
		verify(messageSender).send(eq("bob@x.com"), any(Message.class));
		assertThat(message.getValue().body()).contains("Average: 0.50");
	}

	@Test
	void nobodyToNotifyMeansNothingIsSent() {
		service.onRoundClosed(new RoundClosed("g-1", "Team", new RoundResult("r-1", 0, 1, 0.5, 2, Instant.now()),
				List.of()));

		verifyNoInteractions(messageSender);
	}

	@Test
	void aFailureForOneRecipientDoesNotStopTheOthers() {
		doThrow(new IllegalStateException("smtp down")).when(messageSender).send(eq("bob@x.com"), any());

		service.onRoundStarted(new RoundStarted("g-1", "Team", "r-1", "ann@x.com", List.of("bob@x.com", "cy@x.com")));

		verify(messageSender).send(eq("cy@x.com"), any(Message.class));
		verify(messageSender, never()).send(eq("ann@x.com"), any());
	}

}
