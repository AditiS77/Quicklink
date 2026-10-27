package com.aditisingh.Quicklink.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlTest {

    private final User owner = new User("Ada", "ada@example.com", "hash", Role.USER);

    @Test
    void aLinkWithNoExpirationNeverExpires() {
        assertThat(link(null).isExpired()).isFalse();
    }

    @Test
    void aLinkWithAFutureExpirationIsNotExpired() {
        assertThat(link(Instant.now().plusSeconds(3600)).isExpired()).isFalse();
    }

    @Test
    void aLinkWithAPastExpirationIsExpired() {
        assertThat(link(Instant.now().minusSeconds(1)).isExpired()).isTrue();
    }

    @Test
    void isOwnedByMatchesTheOwnersEmailOnly() {
        ShortUrl url = link(null);

        assertThat(url.isOwnedBy("ada@example.com")).isTrue();
        assertThat(url.isOwnedBy("mallory@example.com")).isFalse();
    }

    @Test
    void isOwnedByIsFalseForAnAnonymousRequester() {
        assertThat(link(null).isOwnedBy(null)).isFalse();
    }

    private ShortUrl link(Instant expiresAt) {
        return new ShortUrl("https://example.com", "Ab3dE6gH", expiresAt, owner, Visibility.PUBLIC);
    }
}
