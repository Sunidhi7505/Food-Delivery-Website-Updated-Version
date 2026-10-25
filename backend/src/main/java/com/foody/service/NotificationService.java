package com.foody.service;

import com.foody.model.CartItem;
import com.foody.model.Order;
import com.foody.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${foody.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${foody.mail.from:}")
    private String fromAddress;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderPlacedEmail(User user, Order order) {
        if (user == null || order == null) {
            return;
        }

        String items = order.getItems()
                .stream()
                .map(item -> item.getName() + " x " + item.getQuantity())
                .collect(Collectors.joining(", "));

        String body = "Hi " + user.getName() + ",\n\n"
                + "Your Foody order has been placed successfully.\n"
                + "Order ID: " + order.getId() + "\n"
                + "Items: " + items + "\n"
                + "Total: Rs " + order.getTotalAmount() + "\n"
                + "Status: " + order.getStatus() + "\n\n"
                + "We will keep you updated as your food is prepared.";

        send(user.getEmail(), "Foody order placed - " + order.getId(), body);
    }

    public void sendOrderDeliveredEmail(Order order) {
        if (order == null) {
            return;
        }

        String body = "Hi " + safe(order.getCustomerName()) + ",\n\n"
                + "Your Foody order " + order.getId() + " has been delivered.\n"
                + "Please open your order history and share feedback for the dishes you ordered.";

        send(order.getUserEmail(), "Foody order delivered - feedback requested", body);
    }

    public void sendPasswordResetEmail(User user, String link, int validMinutes) {
        if (user == null) {
            return;
        }
        String body = "Hi " + safe(user.getName()) + ",\n\n"
                + "Use the link below to reset your Foody password. It is valid for " + validMinutes + " minutes "
                + "and works only once.\n" + link + "\n\n"
                + "If you did not request this, you can ignore this email.";
        send(user.getEmail(), "Reset your Foody password", body);
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled && to != null) {
            // Local/dev mode: no SMTP configured, so show the mail in the console instead.
            log.info("[mail disabled] To: {} | {}\n{}", to, subject, body);
        }
        if (!mailEnabled || to == null || to.isBlank()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            System.out.println("Email skipped: " + ex.getMessage());
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "there" : value;
    }
}
