package com.mobflow.notificationservice.mail;

import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.service.NotificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Properties;

import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.notification;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private NotificationService notificationService;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(javaMailSender, templateEngine, notificationService, "noreply@mobflow.test", 3);
    }

    @Test
    void sendConfirmationEmail_validNotification_sendsMessageAndMarksAsSent() {
        Notification notification = notification("user-1");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("confirmation-email"), any())).thenReturn("<html>email</html>");

        mailService.sendConfirmationEmail(notification, "http://mobflow.test/confirm?token=123");

        verify(javaMailSender).send(mimeMessage);
        verify(notificationService).markAsSent(notification.getId());
    }

    @Test
    void sendConfirmationEmail_mailSendFails_retriesAndIncrementsRetryCount() {
        Notification notification = notification("user-1");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("confirmation-email"), any())).thenReturn("<html>email</html>");
        doThrow(new RuntimeException("mail failure")).when(javaMailSender).send(any(MimeMessage.class));

        mailService.sendConfirmationEmail(notification, "http://mobflow.test/confirm?token=123");

        verify(notificationService, times(3)).incrementRetryCount(notification.getId());
        verify(notificationService, never()).markAsSent(notification.getId());
    }

    @Test
    void sendConfirmationEmail_missingRecipientEmail_skipsSending() {
        Notification notification = notification("user-1");
        notification.setRecipientEmail(" ");

        mailService.sendConfirmationEmail(notification, "http://mobflow.test/confirm?token=123");

        verify(javaMailSender, never()).createMimeMessage();
    }
}
