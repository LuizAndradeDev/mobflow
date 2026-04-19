package com.mobflow.notificationservice.integration;

import com.mobflow.notificationservice.model.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.repository.NotificationRepository;
import com.mobflow.notificationservice.service.NotificationService;
import com.mobflow.notificationservice.testsupport.AbstractMongoNotificationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.notification;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "security.jwt.secret-key=c3VwZXItc2VjdXJlLXRlc3Qta2V5LWZvci1ub3RpZmljYXRpb24tc2VydmljZQ==",
        "management.health.mail.enabled=false",
        "spring.mail.host=localhost",
        "spring.mail.port=1025",
        "spring.mail.username=noreply@mobflow.test",
        "spring.mail.password=test",
        "app.kafka.topics.auth=auth-events",
        "app.kafka.topics.task=task-events",
        "app.kafka.topics.workspace=workspace-events",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class NotificationIntegrationTest extends AbstractMongoNotificationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void saveListAndMarkNotifications_readFlowPersistsAcrossMongoLayer() {
        Notification first = notificationService.save(notification("user-1"));
        Notification second = notificationService.save(notification("user-1"));

        List<NotificationResponseDTO> listed = notificationService.listForUser("user-1");

        assertThat(listed).hasSize(2);
        assertThat(notificationService.countUnread("user-1")).isEqualTo(2L);

        notificationService.markAsRead(first.getId(), "user-1");
        long markedAll = notificationService.markAllAsRead("user-1");

        assertThat(markedAll).isEqualTo(1L);
        assertThat(notificationRepository.findById(second.getId())).isPresent().get().extracting(Notification::isRead).isEqualTo(true);
    }
}
