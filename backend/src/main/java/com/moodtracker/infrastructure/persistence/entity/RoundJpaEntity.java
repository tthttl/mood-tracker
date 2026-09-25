package com.moodtracker.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "rounds")
public class RoundJpaEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private String id;

	@Column(nullable = false)
	private int moodRange;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "round_eligible_members", joinColumns = @JoinColumn(name = "round_id"))
	@Column(name = "email", nullable = false)
	private Set<String> eligibleMemberEmails = new HashSet<>();

	@Column(nullable = false)
	private String startedBy;

	@Column(nullable = false)
	private Instant startedAt;

	private Instant closedAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "submissions", joinColumns = @JoinColumn(name = "round_id"))
	private List<SubmissionEmbeddable> submissions = new ArrayList<>();

	protected RoundJpaEntity() {
	}

	public RoundJpaEntity(String id, int moodRange, Set<String> eligibleMemberEmails,
			String startedBy, Instant startedAt, Instant closedAt, List<SubmissionEmbeddable> submissions) {
		this.id = id;
		this.moodRange = moodRange;
		this.eligibleMemberEmails = eligibleMemberEmails;
		this.startedBy = startedBy;
		this.startedAt = startedAt;
		this.closedAt = closedAt;
		this.submissions = submissions;
	}

	public String getId() {
		return id;
	}

	public int getMoodRange() {
		return moodRange;
	}

	public Set<String> getEligibleMemberEmails() {
		return eligibleMemberEmails;
	}

	public String getStartedBy() {
		return startedBy;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getClosedAt() {
		return closedAt;
	}

	public List<SubmissionEmbeddable> getSubmissions() {
		return submissions;
	}

}
