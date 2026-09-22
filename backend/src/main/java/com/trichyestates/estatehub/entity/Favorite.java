package com.trichyestates.estatehub.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorites_user_property", columnNames = {"user_id", "property_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() { }

    public Favorite(User user, Property property) {
        this.user = user;
        this.property = property;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Property getProperty() { return property; }
    public Instant getCreatedAt() { return createdAt; }
}
