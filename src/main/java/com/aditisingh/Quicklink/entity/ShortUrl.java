package com.aditisingh.Quicklink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility;

    protected ShortUrl() {
    }

    public ShortUrl(String originalUrl, String shortCode, Instant expiresAt, User owner, Visibility visibility) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.expiresAt = expiresAt;
        this.owner = owner;
        this.visibility = visibility;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getClickCount() {
        return clickCount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public User getOwner() {
        return owner;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isOwnedBy(String email) {
        return email != null && owner.getEmail().equals(email);
    }

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(Instant.now());
    }
}
