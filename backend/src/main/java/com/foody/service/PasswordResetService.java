package com.foody.service;

import com.foody.exception.BadRequestException;
import com.foody.model.User;
import com.foody.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Forgot / reset password: a random single-use token is mailed; only its SHA-256 hash is stored. */
@Service
public class PasswordResetService {

    static final int TOKEN_MINUTES = 15;
    static final String GENERIC_MESSAGE = "If that email is registered, a reset link has been sent.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

    @Value("${foody.frontend-url:http://localhost:5500}")
    private String frontendUrl;

    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    /** Always answers with the same message so emails cannot be enumerated. */
    public String requestReset(String email) {
        userRepository.findByEmail(email == null ? "" : email.trim())
                .filter(user -> !user.isBlocked())
                .ifPresent(user -> {
                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

                    user.setResetTokenHash(hash(token));
                    user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(TOKEN_MINUTES));
                    userRepository.save(user);

                    notificationService.sendPasswordResetEmail(user,
                            frontendUrl + "/reset-password.html?token=" + token, TOKEN_MINUTES);
                });
        return GENERIC_MESSAGE;
    }

    public String resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetTokenHash(hash(token == null ? "" : token))
                .filter(u -> u.getResetTokenExpiry() != null && u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException("Reset link is invalid or has expired"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetTokenHash(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return "Password updated. You can login now.";
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
