package com.aditisingh.Quicklink.service;

import com.aditisingh.Quicklink.entity.Role;
import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.entity.User;
import com.aditisingh.Quicklink.entity.Visibility;
import com.aditisingh.Quicklink.exception.ExpiredShortUrlException;
import com.aditisingh.Quicklink.exception.PrivateUrlAccessDeniedException;
import com.aditisingh.Quicklink.repository.ShortUrlRepository;
import com.aditisingh.Quicklink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    private static final String OWNER_EMAIL = "ada@example.com";
    private static final String CODE = "Ab3dE6gH";

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UserRepository userRepository;

    private ShortUrlService service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new ShortUrlService(shortUrlRepository, userRepository);
        owner = new User("Ada", OWNER_EMAIL, "hash", Role.USER);
    }

    // ---------------------------------------------------------------- createShortUrl

    @Test
    void createShortUrlSavesAnEightCharacterAlphanumericCode() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl created = service.createShortUrl("https://example.com/page", null, Visibility.PUBLIC, OWNER_EMAIL);

        assertThat(created.getShortCode()).matches("[A-Za-z0-9]{8}");
        assertThat(created.getOriginalUrl()).isEqualTo("https://example.com/page");
        assertThat(created.getOwner()).isSameAs(owner);
        assertThat(created.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(created.getExpiresAt()).isNull();
    }

    @Test
    void createShortUrlInterpretsTheExpirationAsUtc() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl created = service.createShortUrl("https://example.com",
                LocalDateTime.of(2030, 1, 15, 10, 30), Visibility.PRIVATE, OWNER_EMAIL);

        assertThat(created.getExpiresAt()).isEqualTo(Instant.parse("2030-01-15T10:30:00Z"));
        assertThat(created.getVisibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void createShortUrlRetriesWhenAGeneratedCodeAlreadyExists() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(true, true, false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl created = service.createShortUrl("https://example.com", null, Visibility.PUBLIC, OWNER_EMAIL);

        assertThat(created).isNotNull();
        verify(shortUrlRepository, times(3)).existsByShortCode(anyString());
        verify(shortUrlRepository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    void createShortUrlGivesUpAfterTenCollidingAttempts() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.createShortUrl("https://example.com", null, Visibility.PUBLIC, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unique");

        verify(shortUrlRepository, times(10)).existsByShortCode(anyString());
        verify(shortUrlRepository, never()).save(any(ShortUrl.class));
    }

    @Test
    void createShortUrlFailsWhenTheOwnerNoLongerExists() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createShortUrl("https://example.com", null, Visibility.PUBLIC, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(shortUrlRepository);
    }

    // ---------------------------------------------------------------- resolveAndRecordClick

    @Test
    void resolveReturnsEmptyForAnUnknownCode() {
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.empty());

        assertThat(service.resolveAndRecordClick(CODE, null)).isEmpty();

        verify(shortUrlRepository, never()).incrementClickCount(anyLong());
    }

    @Test
    void resolveReturnsTheOriginalUrlAndCountsTheClickForPublicLinks() {
        ShortUrl url = shortUrl(Visibility.PUBLIC, null);
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(url));
        when(shortUrlRepository.incrementClickCount(42L)).thenReturn(1);

        Optional<String> target = service.resolveAndRecordClick(CODE, null);

        assertThat(target).contains("https://example.com/target");
        verify(shortUrlRepository).incrementClickCount(42L);
    }

    @Test
    void resolveAllowsAnyoneToUseALinkThatHasNotYetExpired() {
        ShortUrl url = shortUrl(Visibility.PUBLIC, Instant.now().plusSeconds(3600));
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(url));
        when(shortUrlRepository.incrementClickCount(42L)).thenReturn(1);

        assertThat(service.resolveAndRecordClick(CODE, "someone@else.com")).isPresent();
    }

    @Test
    void resolveAllowsTheOwnerToUseAPrivateLink() {
        ShortUrl url = shortUrl(Visibility.PRIVATE, null);
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(url));
        when(shortUrlRepository.incrementClickCount(42L)).thenReturn(1);

        assertThat(service.resolveAndRecordClick(CODE, OWNER_EMAIL)).contains("https://example.com/target");
    }

    @Test
    void resolveDeniesAPrivateLinkToAnotherUser() {
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(shortUrl(Visibility.PRIVATE, null)));

        assertThatThrownBy(() -> service.resolveAndRecordClick(CODE, "mallory@example.com"))
                .isInstanceOf(PrivateUrlAccessDeniedException.class);

        verify(shortUrlRepository, never()).incrementClickCount(anyLong());
    }

    @Test
    void resolveDeniesAPrivateLinkToAnAnonymousVisitor() {
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(shortUrl(Visibility.PRIVATE, null)));

        assertThatThrownBy(() -> service.resolveAndRecordClick(CODE, null))
                .isInstanceOf(PrivateUrlAccessDeniedException.class);
    }

    @Test
    void resolveRejectsAnExpiredLinkAndDoesNotCountTheClick() {
        ShortUrl url = shortUrl(Visibility.PUBLIC, Instant.now().minusSeconds(60));
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(url));

        assertThatThrownBy(() -> service.resolveAndRecordClick(CODE, null))
                .isInstanceOf(ExpiredShortUrlException.class);

        verify(shortUrlRepository, never()).incrementClickCount(anyLong());
    }

    @Test
    void privacyIsCheckedBeforeExpiryHenceStrangersCannotLearnThatAPrivateLinkExpired() {
        ShortUrl url = shortUrl(Visibility.PRIVATE, Instant.now().minusSeconds(60));
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(url));

        assertThatThrownBy(() -> service.resolveAndRecordClick(CODE, "mallory@example.com"))
                .isInstanceOf(PrivateUrlAccessDeniedException.class);
    }

    @Test
    void theOwnerOfAnExpiredPrivateLinkIsToldItExpired() {
        ShortUrl url = shortUrl(Visibility.PRIVATE, Instant.now().minusSeconds(60));
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(url));

        assertThatThrownBy(() -> service.resolveAndRecordClick(CODE, OWNER_EMAIL))
                .isInstanceOf(ExpiredShortUrlException.class);
    }

    @Test
    void resolveReturnsEmptyWhenTheRowDisappearsBeforeTheClickIsRecorded() {
        when(shortUrlRepository.findByShortCode(CODE)).thenReturn(Optional.of(shortUrl(Visibility.PUBLIC, null)));
        when(shortUrlRepository.incrementClickCount(42L)).thenReturn(0);

        assertThat(service.resolveAndRecordClick(CODE, null)).isEmpty();
    }

    // ---------------------------------------------------------------- helpers

    private ShortUrl shortUrl(Visibility visibility, Instant expiresAt) {
        ShortUrl url = new ShortUrl("https://example.com/target", CODE, expiresAt, owner, visibility);
        ReflectionTestUtils.setField(url, "id", 42L);
        return url;
    }
}
