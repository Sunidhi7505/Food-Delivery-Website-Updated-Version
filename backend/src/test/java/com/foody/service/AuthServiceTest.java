package com.foody.service;

import com.foody.dto.LoginRequest;
import com.foody.model.User;
import com.foody.repository.UserRepository;
import com.foody.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    private LoginRequest request(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private User user(boolean blocked) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", "u1");
        u.setEmail("a@b.com");
        u.setPassword("hash");
        u.setRole("ROLE_CUSTOMER");
        u.setBlocked(blocked);
        return u;
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtUtil.generateToken("a@b.com", "ROLE_CUSTOMER")).thenReturn("jwt-token");

        Map<String, Object> result = authService.login(request("a@b.com", "secret"));

        assertEquals("jwt-token", result.get("token"));
        assertEquals("ROLE_CUSTOMER", result.get("role"));
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request("a@b.com", "bad")));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(jwtUtil, never()).generateToken("a@b.com", "ROLE_CUSTOMER");
    }

    @Test
    void loginRejectsBlockedUser() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user(true)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request("a@b.com", "secret")));

        assertEquals("Your account is blocked. Please contact admin.", ex.getMessage());
    }
}
