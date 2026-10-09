package com.moodtracker.domain.notification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageTemplate {
	private static final String SUBJECT_PREFIX = "Subject: ";
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");

	private final String subject;
	private final String body;

	private MessageTemplate(String subject, String body) {
		this.subject = subject;
		this.body = body;
	}

	public static MessageTemplate parse(String text) {
		String[] parts = text.split("\\R", 3);
		if (parts.length < 3 || !parts[0].startsWith(SUBJECT_PREFIX) || !parts[1].isEmpty()) {
			throw new IllegalArgumentException(
					"A template is 'Subject: <subject>', an empty line, then the body");
		}
		String subject = parts[0].substring(SUBJECT_PREFIX.length()).strip();
		if (subject.isEmpty()) {
			throw new IllegalArgumentException("The template subject must not be empty");
		}
		return new MessageTemplate(subject, parts[2]);
	}

	public static MessageTemplate fromResource(String name) {
		String path = "/mail/" + name + ".txt";
		try (InputStream in = MessageTemplate.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Message template not found: " + path);
			}
			return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read message template " + path, e);
		}
	}

	public Set<String> placeholders() {
		Set<String> names = new LinkedHashSet<>(namesIn(subject));
		names.addAll(namesIn(body));
		return names;
	}

	public Message render(Map<String, String> values) {
		Set<String> expected = placeholders();
		if (!values.keySet().equals(expected)) {
			throw new IllegalArgumentException(
					"Template needs values for " + expected + " but got " + values.keySet());
		}
		return new Message(renderSubject(values), renderBody(values));
	}

	private String renderSubject(Map<String, String> values) {
		return fill(subject, name -> withoutLineBreaks(values.get(name)));
	}

	private String renderBody(Map<String, String> values) {
		return fill(body, values::get);
	}

	private static String withoutLineBreaks(String value) {
		return value.replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
	}

	private String fill(String text, Function<String, String> valueOf) {
		Matcher matcher = PLACEHOLDER.matcher(text);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(result, Matcher.quoteReplacement(valueOf.apply(matcher.group(1))));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private Set<String> namesIn(String text) {
		Set<String> names = new LinkedHashSet<>();
		Matcher matcher = PLACEHOLDER.matcher(text);
		while (matcher.find()) {
			names.add(matcher.group(1));
		}
		return names;
	}

}
