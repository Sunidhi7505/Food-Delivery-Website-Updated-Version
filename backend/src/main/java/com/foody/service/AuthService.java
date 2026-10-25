package com.foody.service;

import com.foody.exception.ForbiddenException;
import com.foody.exception.UnauthorizedException;
import com.foody.dto.LoginRequest;
import com.foody.model.User;
import com.foody.repository.UserRepository;
import com.foody.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isBlocked()) {
            throw new ForbiddenException("Your account is blocked. Please contact admin.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "role", user.getRole(),   
            "token", token
        );
    }
}
