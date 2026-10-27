package com.aditisingh.Quicklink.dto;

import com.aditisingh.Quicklink.entity.Visibility;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateShortUrlRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com",
            "http://example.com",
            "https://example.com/path/to/page?query=1&other=2#fragment"
    })
    void acceptsHttpAndHttpsUrls(String url) {
        assertThat(violatedProperties(request(url))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingUrls(String url) {
        assertThat(violatedProperties(request(url))).contains("originalUrl");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "javascript:alert(1)",
            "file:///etc/passwd",
            "not a url",
            "example.com"
    })
    void rejectsUrlsThatAreNotHttpOrHttps(String url) {
        assertThat(violatedProperties(request(url))).contains("originalUrl");
    }

    @Test
    void rejectsUrlsLongerThan2048Characters() {
        String tooLong = "https://example.com/" + "a".repeat(2048);

        assertThat(violatedProperties(request(tooLong))).contains("originalUrl");
    }

    @Test
    void expirationIsOptional() {
        CreateShortUrlRequest request = request("https://example.com");
        request.setExpiresAt(null);

        assertThat(violatedProperties(request)).isEmpty();
    }

    @Test
    void acceptsAFutureExpiration() {
        CreateShortUrlRequest request = request("https://example.com");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        assertThat(violatedProperties(request)).isEmpty();
    }

    @Test
    void rejectsAPastExpiration() {
        CreateShortUrlRequest request = request("https://example.com");
        request.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThat(violatedProperties(request)).containsExactly("expiresAt");
    }

    @Test
    void visibilityDefaultsToPublic() {
        assertThat(new CreateShortUrlRequest().getVisibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test
    void rejectsANullVisibility() {
        CreateShortUrlRequest request = request("https://example.com");
        request.setVisibility(null);

        assertThat(violatedProperties(request)).containsExactly("visibility");
    }

    private static CreateShortUrlRequest request(String url) {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl(url);
        return request;
    }

    private static Set<String> violatedProperties(CreateShortUrlRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
