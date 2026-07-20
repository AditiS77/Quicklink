package com.aditisingh.Quicklink.service;

import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.entity.Visibility;
import com.aditisingh.Quicklink.exception.PrivateUrlAccessDeniedException;
import com.aditisingh.Quicklink.repository.ShortUrlRepository;
import com.aditisingh.Quicklink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class ShortUrlService {

    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final ShortUrlRepository shortUrlRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ShortUrlService(ShortUrlRepository shortUrlRepository, UserRepository userRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ShortUrl createShortUrl(String originalUrl, LocalDateTime expiresAt, Visibility visibility, String ownerEmail) {
        var owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        Instant expirationInstant = expiresAt == null ? null : expiresAt.toInstant(ZoneOffset.UTC);
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String shortCode = generateShortCode();
            if (!shortUrlRepository.existsByShortCode(shortCode)) {
                return shortUrlRepository.save(new ShortUrl(originalUrl, shortCode, expirationInstant, owner, visibility));
            }
        }

        throw new IllegalStateException("Unable to generate a unique short URL code");
    }

    @Transactional(readOnly = true)
    public List<ShortUrl> findUrlsForOwner(String ownerEmail) {
        return shortUrlRepository.findAllByOwner_EmailOrderByCreatedAtDesc(ownerEmail);
    }

    @Transactional
    public Optional<String> resolveAndRecordClick(String shortCode, String requesterEmail) {
        Optional<ShortUrl> shortUrl = shortUrlRepository.findByShortCode(shortCode);
        if (shortUrl.isEmpty()) {
            return Optional.empty();
        }

        ShortUrl resolvedUrl = shortUrl.get();
        if (resolvedUrl.getVisibility() == Visibility.PRIVATE && !resolvedUrl.isOwnedBy(requesterEmail)) {
            throw new PrivateUrlAccessDeniedException();
        }
        if (resolvedUrl.isExpired()) {
            return Optional.empty();
        }

        return incrementClickCount(resolvedUrl);
    }

    private Optional<String> incrementClickCount(ShortUrl shortUrl) {
        int updatedRows = shortUrlRepository.incrementClickCount(shortUrl.getId());
        if (updatedRows == 1) {
            return Optional.of(shortUrl.getOriginalUrl());
        }
        return Optional.empty();
    }

    private String generateShortCode() {
        StringBuilder shortCode = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            shortCode.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return shortCode.toString();
    }
}
