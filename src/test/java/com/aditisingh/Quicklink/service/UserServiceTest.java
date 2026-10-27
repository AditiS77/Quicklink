package com.aditisingh.Quicklink.service;

import com.aditisingh.Quicklink.entity.Role;
import com.aditisingh.Quicklink.entity.User;
import com.aditisingh.Quicklink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder);
    }

    // ---------------------------------------------------------------- registerUser

    @Test
    void registerUserTrimsAndLowercasesInputHashesThePasswordAndAssignsTheUserRole() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");

        service.registerUser("  Ada Lovelace  ", "  Ada@Example.COM ", "secret123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ada Lovelace");
        assertThat(saved.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed-secret");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerUserRejectsAnEmailThatAlreadyExists() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser("Ada", "ADA@example.com", "secret123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerUserTranslatesARaceOnTheUniqueConstraintIntoTheSameFriendlyError() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.registerUser("Ada", "ada@example.com", "secret123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- loadUserByUsername

    @Test
    void loadUserByUsernameBuildsSpringSecurityDetailsWithTheRolePrefix() {
        User admin = new User("Root", "root@example.com", "stored-hash", Role.ADMIN);
        when(userRepository.findByEmail("root@example.com")).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("root@example.com");

        assertThat(details.getUsername()).isEqualTo("root@example.com");
        assertThat(details.getPassword()).isEqualTo("stored-hash");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsernameIsCaseAndWhitespaceInsensitive() {
        User user = new User("Ada", "ada@example.com", "stored-hash", Role.USER);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("  ADA@Example.com ");

        assertThat(details.getUsername()).isEqualTo("ada@example.com");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsernameThrowsWhenTheUserDoesNotExist() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ---------------------------------------------------------------- lookups

    @Test
    void findNameByEmailReturnsTheName() {
        when(userRepository.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(new User("Ada", "ada@example.com", "hash", Role.USER)));

        assertThat(service.findNameByEmail("ada@example.com")).isEqualTo("Ada");
    }

    @Test
    void findNameByEmailFallsBackToTheEmailWhenTheUserIsMissing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThat(service.findNameByEmail("ghost@example.com")).isEqualTo("ghost@example.com");
    }
}
