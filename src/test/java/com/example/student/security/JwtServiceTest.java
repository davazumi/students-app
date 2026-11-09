package com.example.student.security;

import com.example.student.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @Test
    void tokenContainsUsernameAndRoles() {
        String token = jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN, Role.TEACHER));

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
        assertThat(jwtService.extractRoles(token)).containsExactlyInAnyOrder(Role.ADMIN, Role.TEACHER);
    }

    @Test
    void tokenIsValidOnlyForItsOwner() {
        String token = jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN));
        UserDetails owner = User.withUsername("admin@example.com").password("x").build();
        UserDetails other = User.withUsername("teacher@example.com").password("x").build();

        assertThat(jwtService.validateToken(token, owner)).isTrue();
        assertThat(jwtService.validateToken(token, other)).isFalse();
    }

    @Test
    void brokenTokenIsRejected() {
        assertThatThrownBy(() -> jwtService.extractUsername("not.a.token"))
                .isInstanceOf(RuntimeException.class);
    }
}
