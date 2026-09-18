package com.moodtracker.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
public class GroupJpaEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private String id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private int moodRange;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"))
	private List<MemberEmbeddable> members = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "group_id", nullable = false)
	private List<RoundJpaEntity> rounds = new ArrayList<>();

	protected GroupJpaEntity() {
	}

	public GroupJpaEntity(String id, String name, int moodRange, List<MemberEmbeddable> members,
			List<RoundJpaEntity> rounds) {
		this.id = id;
		this.name = name;
		this.moodRange = moodRange;
		this.members = members;
		this.rounds = rounds;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getMoodRange() {
		return moodRange;
	}

	public List<MemberEmbeddable> getMembers() {
		return members;
	}

	public List<RoundJpaEntity> getRounds() {
		return rounds;
	}

}
