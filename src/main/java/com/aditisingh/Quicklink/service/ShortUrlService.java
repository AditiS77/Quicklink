package com.aditisingh.Quicklink.service;

import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.repository.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class ShortUrlService {

    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final ShortUrlRepository shortUrlRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ShortUrlService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @Transactional
    public ShortUrl createShortUrl(String originalUrl) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String shortCode = generateShortCode();
            if (!shortUrlRepository.existsByShortCode(shortCode)) {
                return shortUrlRepository.save(new ShortUrl(originalUrl, shortCode));
            }
        }

        throw new IllegalStateException("Unable to generate a unique short URL code");
    }

    @Transactional(readOnly = true)
    public Optional<String> findOriginalUrl(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .map(ShortUrl::getOriginalUrl);
    }

    private String generateShortCode() {
        StringBuilder shortCode = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            shortCode.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return shortCode.toString();
    }
}
