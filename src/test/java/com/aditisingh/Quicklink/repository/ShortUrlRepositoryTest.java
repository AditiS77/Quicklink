package com.aditisingh.Quicklink.repository;

import com.aditisingh.Quicklink.entity.Role;
import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.entity.User;
import com.aditisingh.Quicklink.entity.Visibility;
import com.aditisingh.Quicklink.support.AbstractPostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShortUrlRepositoryTest extends AbstractPostgresTest {

    private static final Instant T1 = Instant.parse("2030-01-01T10:00:00Z");
    private static final Instant T2 = Instant.parse("2030-01-01T11:00:00Z");
    private static final Instant T3 = Instant.parse("2030-01-01T12:00:00Z");

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User ada;
    private User bob;

    @BeforeEach
    void setUp() {
        // Runs inside the test transaction, so it is rolled back and never touches other test classes' data.
        shortUrlRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        ada = userRepository.saveAndFlush(new User("Ada", "ada@example.com", "hash", Role.USER));
        bob = userRepository.saveAndFlush(new User("Bob", "bob@example.com", "hash", Role.USER));
    }

    @Test
    void findByShortCodeAndExistsByShortCode() {
        save(ada, "CodeAAAA", Visibility.PUBLIC, T1);

        assertThat(shortUrlRepository.findByShortCode("CodeAAAA")).isPresent();
        assertThat(shortUrlRepository.findByShortCode("Nope0000")).isEmpty();
        assertThat(shortUrlRepository.existsByShortCode("CodeAAAA")).isTrue();
        assertThat(shortUrlRepository.existsByShortCode("Nope0000")).isFalse();
    }

    @Test
    void findAllByOwnerReturnsOnlyThatOwnersLinksNewestFirst() {
        save(ada, "AdaOldes", Visibility.PUBLIC, T1);
        save(ada, "AdaNewes", Visibility.PRIVATE, T3);
        save(ada, "AdaMiddl", Visibility.PUBLIC, T2);
        save(bob, "BobLinkk", Visibility.PUBLIC, T2);

        assertThat(shortUrlRepository.findAllByOwner_EmailOrderByCreatedAtDesc("ada@example.com"))
                .extracting(ShortUrl::getShortCode)
                .containsExactly("AdaNewes", "AdaMiddl", "AdaOldes");
    }

    @Test
    void findAllByOwnerReturnsNothingForAUserWithNoLinksOrAnUnknownEmail() {
        save(ada, "AdaLinkk", Visibility.PUBLIC, T1);

        assertThat(shortUrlRepository.findAllByOwner_EmailOrderByCreatedAtDesc("bob@example.com")).isEmpty();
        assertThat(shortUrlRepository.findAllByOwner_EmailOrderByCreatedAtDesc("ghost@example.com")).isEmpty();
    }

    @Test
    void findAllWithOwnerReturnsEveryUsersLinksNewestFirst() {
        save(ada, "AdaOldes", Visibility.PUBLIC, T1);
        save(bob, "BobNewes", Visibility.PRIVATE, T3);
        save(ada, "AdaMiddl", Visibility.PUBLIC, T2);
        entityManager.clear();

        assertThat(shortUrlRepository.findAllWithOwnerOrderByCreatedAtDesc())
                .extracting(ShortUrl::getShortCode)
                .containsExactly("BobNewes", "AdaMiddl", "AdaOldes");
        assertThat(shortUrlRepository.findAllWithOwnerOrderByCreatedAtDesc())
                .extracting(url -> url.getOwner().getEmail())
                .containsExactly("bob@example.com", "ada@example.com", "ada@example.com");
    }

    @Test
    void incrementClickCountAddsOneEachTimeAndReportsTheRowUpdated() {
        ShortUrl saved = save(ada, "CodeAAAA", Visibility.PUBLIC, T1);

        assertThat(shortUrlRepository.incrementClickCount(saved.getId())).isEqualTo(1);
        assertThat(shortUrlRepository.incrementClickCount(saved.getId())).isEqualTo(1);
        assertThat(shortUrlRepository.incrementClickCount(saved.getId())).isEqualTo(1);
        entityManager.clear();

        assertThat(shortUrlRepository.findById(saved.getId()).orElseThrow().getClickCount()).isEqualTo(3);
    }

    @Test
    void incrementClickCountOnlyAffectsTheTargetedLink() {
        ShortUrl first = save(ada, "CodeAAAA", Visibility.PUBLIC, T1);
        ShortUrl second = save(ada, "CodeBBBB", Visibility.PUBLIC, T2);

        shortUrlRepository.incrementClickCount(first.getId());
        entityManager.clear();

        assertThat(shortUrlRepository.findById(first.getId()).orElseThrow().getClickCount()).isEqualTo(1);
        assertThat(shortUrlRepository.findById(second.getId()).orElseThrow().getClickCount()).isZero();
    }

    @Test
    void incrementClickCountReturnsZeroForAnUnknownId() {
        assertThat(shortUrlRepository.incrementClickCount(-1L)).isZero();
    }

    private ShortUrl save(User owner, String code, Visibility visibility, Instant createdAt) {
        ShortUrl url = new ShortUrl("https://example.com/" + code, code, null, owner, visibility);
        // createdAt is normally set by @PrePersist; pre-setting it makes ordering assertions deterministic.
        ReflectionTestUtils.setField(url, "createdAt", createdAt);
        return shortUrlRepository.saveAndFlush(url);
    }
}
