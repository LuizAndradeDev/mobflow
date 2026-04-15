package com.mobflow.notificationservice.mail;

import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final NotificationService notificationService;
    private final String fromAddress;
    private final int maxAttempts;

    public MailService(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            NotificationService notificationService,
            @Value("${spring.mail.username}") String fromAddress,
            @Value("${app.mail.max-attempts:3}") int maxAttempts
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.notificationService = notificationService;
        this.fromAddress = fromAddress;
        this.maxAttempts = maxAttempts;
    }

    public void sendConfirmationEmail(Notification notification, String confirmationLink) {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            log.warn("Skipping confirmation email because recipient email is missing for notification {}", notification.getId());
            return;
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(notification.getRecipientEmail());
                helper.setSubject(notification.getTitle());
                helper.setText(buildConfirmationEmailBody(notification, confirmationLink), true);

                mailSender.send(mimeMessage);
                notificationService.markAsSent(notification.getId());
                return;
            } catch (Exception exception) {
                log.warn(
                        "Failed to send confirmation email for notification {} on attempt {}/{}",
                        notification.getId(),
                        attempt,
                        maxAttempts,
                        exception
                );
                notificationService.incrementRetryCount(notification.getId());
            }
        }
    }

    private String buildConfirmationEmailBody(Notification notification, String confirmationLink) {
        Context context = new Context();
        context.setVariable("title", notification.getTitle());
        context.setVariable("body", notification.getBody());
        context.setVariable("confirmationLink", confirmationLink);
        return templateEngine.process("confirmation-email", context);
    }
}
