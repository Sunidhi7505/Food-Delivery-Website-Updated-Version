package com.foody.service;

import com.foody.exception.BadRequestException;
import com.foody.model.User;
import com.foody.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock NotificationService notificationService;
    @InjectMocks PasswordResetService service;

    @Test
    void requestResetStoresOnlyHashAndMailsLink() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5500");
        User user = new User();
        user.setEmail("a@b.com");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        String message = service.requestReset("a@b.com");

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendPasswordResetEmail(any(User.class), link.capture(), anyInt());
        String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);
        assertEquals(PasswordResetService.GENERIC_MESSAGE, message);
        assertNotEquals(token, user.getResetTokenHash());
        assertEquals(PasswordResetService.hash(token), user.getResetTokenHash());
    }

    @Test
    void unknownEmailGetsSameAnswerAndNoMail() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());

        assertEquals(PasswordResetService.GENERIC_MESSAGE, service.requestReset("x@y.com"));
        verify(notificationService, never()).sendPasswordResetEmail(any(), anyString(), anyInt());
    }

    @Test
    void resetChangesPasswordAndClearsTokenButRejectsExpired() {
        User user = new User();
        user.setResetTokenHash(PasswordResetService.hash("tok"));
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByResetTokenHash(PasswordResetService.hash("tok"))).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass1")).thenReturn("encoded");

        service.resetPassword("tok", "newpass1");

        assertEquals("encoded", user.getPassword());
        assertNull(user.getResetTokenHash());

        User expired = new User();
        expired.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByResetTokenHash(PasswordResetService.hash("old"))).thenReturn(Optional.of(expired));
        assertThrows(BadRequestException.class, () -> service.resetPassword("old", "newpass1"));
    }
}
