package com.aditisingh.Quicklink.dto;

import com.aditisingh.Quicklink.entity.Visibility;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CreateShortUrlRequest {

    private String originalUrl;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expiresAt;

    private Visibility visibility = Visibility.PUBLIC;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
}
