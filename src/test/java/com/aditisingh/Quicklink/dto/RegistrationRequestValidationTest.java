package com.aditisingh.Quicklink.dto;

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

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationRequestValidationTest {

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

    @Test
    void acceptsAValidRegistration() {
        assertThat(violatedProperties(request("Ada", "ada@example.com", "password123"))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsABlankName(String name) {
        assertThat(violatedProperties(request(name, "ada@example.com", "password123"))).contains("name");
    }

    @Test
    void rejectsANameLongerThan100Characters() {
        assertThat(violatedProperties(request("a".repeat(101), "ada@example.com", "password123")))
                .containsExactly("name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-an-email", "missing-at.example.com", "two@@example.com"})
    void rejectsABlankOrMalformedEmail(String email) {
        assertThat(violatedProperties(request("Ada", email, "password123"))).contains("email");
    }

    @Test
    void passwordMustBeAtLeastEightCharacters() {
        assertThat(violatedProperties(request("Ada", "ada@example.com", "1234567"))).containsExactly("password");
        assertThat(violatedProperties(request("Ada", "ada@example.com", "12345678"))).isEmpty();
    }

    @Test
    void passwordMayBeAtMost72CharactersBecauseThatIsBcryptsLimit() {
        assertThat(violatedProperties(request("Ada", "ada@example.com", "a".repeat(72)))).isEmpty();
        assertThat(violatedProperties(request("Ada", "ada@example.com", "a".repeat(73)))).containsExactly("password");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsAMissingPassword(String password) {
        assertThat(violatedProperties(request("Ada", "ada@example.com", password))).contains("password");
    }

    private static RegistrationRequest request(String name, String email, String password) {
        RegistrationRequest request = new RegistrationRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static Set<String> violatedProperties(RegistrationRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
