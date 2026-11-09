package com.example.student.service.impl;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.exception.EmailAlreadyExistsException;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.mapper.UserMapperImpl;
import com.example.student.model.Role;
import com.example.student.model.User;
import com.example.student.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Модульные тесты сервиса пользователей: репозиторий и шифратор паролей подменены заглушками Mockito. */
class UserServiceImplTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserServiceImpl(userRepository, new UserMapperImpl(), passwordEncoder);
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "hashed-" + inv.getArgument(0));
        // имитируем базу: при сохранении пользователь получает id
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(1L);
            return user;
        });
    }

    @Test
    void existsByEmailDelegatesToRepository() {
        when(userRepository.existsByEmail("a@b.ru")).thenReturn(true);
        assertThat(userService.existsByEmail("a@b.ru")).isTrue();
    }

    @Test
    void registerSavesNewUserWithEncodedPassword() {
        when(userRepository.existsByEmail("new@b.ru")).thenReturn(false);

        boolean result = userService.register(new UserRequest("new@b.ru", "pass", Set.of(Role.TEACHER)));

        assertThat(result).isTrue();
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("pass");
    }

    @Test
    void registerWithoutPasswordDoesNotEncode() {
        when(userRepository.existsByEmail("new@b.ru")).thenReturn(false);

        assertThat(userService.register(new UserRequest("new@b.ru", " ", Set.of()))).isTrue();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepository.existsByEmail("old@b.ru")).thenReturn(true);

        assertThat(userService.register(new UserRequest("old@b.ru", "pass", Set.of()))).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsMissingEmail() {
        assertThat(userService.register(new UserRequest(null, "pass", Set.of()))).isFalse();
    }

    @Test
    void findByEmailReturnsUser() {
        User user = User.builder().email("a@b.ru").password("x").build();
        when(userRepository.findByEmail("a@b.ru")).thenReturn(Optional.of(user));

        assertThat(userService.findByEmail("a@b.ru")).isSameAs(user);
    }

    @Test
    void findByEmailThrowsWhenMissing() {
        when(userRepository.findByEmail("no@b.ru")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("no@b.ru"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("no@b.ru");
    }

    @Test
    void saveCreatesUser() {
        when(userRepository.existsByEmail("new@b.ru")).thenReturn(false);

        UserResponse response = userService.save(new UserRequest("new@b.ru", "pass", Set.of(Role.ADMIN)));

        assertThat(response.getEmail()).isEqualTo("new@b.ru");
        assertThat(response.getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void saveThrowsWhenEmailTaken() {
        when(userRepository.existsByEmail("old@b.ru")).thenReturn(true);

        assertThatThrownBy(() -> userService.save(new UserRequest("old@b.ru", "pass", Set.of())))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void updateUserChangesRoles() {
        User user = User.builder().email("a@b.ru").password("x").roles(Set.of(Role.TEACHER)).build();
        when(userRepository.findByEmail("a@b.ru")).thenReturn(Optional.of(user));

        userService.updateUser(new UserRequest("a@b.ru", null, Set.of(Role.ADMIN)));

        assertThat(user.getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void deleteByIdDelegatesToRepository() {
        userService.deleteById(5L);
        verify(userRepository).deleteById(5L);
    }
}
