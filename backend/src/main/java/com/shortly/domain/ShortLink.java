package com.shortly.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "short_links")
public class ShortLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "short_code", nullable = false, unique = true, length = 12) private String shortCode;
    @Column(name = "original_url", nullable = false, length = 2048) private String originalUrl;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "click_count", nullable = false) private long clickCount;
    protected ShortLink() {}
    public ShortLink(String shortCode, String originalUrl) { this.shortCode = shortCode; this.originalUrl = originalUrl; }
    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public long getClickCount() { return clickCount; }
    public void incrementClicks() { clickCount++; }
}
