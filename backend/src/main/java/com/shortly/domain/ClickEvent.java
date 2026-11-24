package com.shortly.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "click_events", indexes = @Index(name = "idx_click_code_time", columnList = "short_code,clicked_at"))
public class ClickEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "short_code", nullable = false, length = 12) private String shortCode;
    @Column(name = "clicked_at", nullable = false) private Instant clickedAt = Instant.now();
    protected ClickEvent() {}
    public ClickEvent(String shortCode) { this.shortCode = shortCode; }
    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public Instant getClickedAt() { return clickedAt; }
}
