package com.atlascommerce.notification_service.service;

import com.atlascommerce.notification_service.dto.*;
import com.atlascommerce.notification_service.entity.Notification;
import com.atlascommerce.notification_service.enums.NotificationChannel;
import com.atlascommerce.notification_service.enums.NotificationStatus;
import com.atlascommerce.notification_service.exception.InvalidNotificationStateException;
import com.atlascommerce.notification_service.mapper.NotificationMapper;
import com.atlascommerce.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repository;
    @Mock NotificationMapper mapper;

    @InjectMocks NotificationService notificationService;

    private Notification notification;
    private NotificationResponse response;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipient("test@atlas.com")
                .subject("Payment captured")
                .message("Your payment was successful")
                .createdAt(Instant.now())
                .build();

        response = new NotificationResponse(
                1L,
                1L,
                NotificationChannel.EMAIL,
                NotificationStatus.PENDING,
                "test@atlas.com",
                "Payment captured",
                "Your payment was successful",
                null,
                notification.getCreatedAt(),
                null
        );
    }

    @Test
    void create_shouldCreateNotificationSuccessfully() {
        var request = new CreateNotificationRequest(
                1L,
                NotificationChannel.EMAIL,
                "test@atlas.com",
                "Payment captured",
                "Your payment was successful"
        );

        when(repository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(any(Notification.class))).thenReturn(response);

        NotificationResponse result = notificationService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(NotificationStatus.PENDING);

        verify(repository).save(any(Notification.class));
    }

    @Test
    void getById_shouldReturnNotification() {
        when(repository.findById(1L)).thenReturn(Optional.of(notification));
        when(mapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void getByUserId_shouldReturnNotifications() {
        when(repository.findByUserId(1L)).thenReturn(List.of(notification));
        when(mapper.toResponse(notification)).thenReturn(response);

        List<NotificationResponse> result = notificationService.getByUserId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void send_shouldMarkNotificationAsSent() {
        when(repository.findById(1L)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(any(Notification.class))).thenReturn(response);

        notificationService.send(1L);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    void send_shouldThrow_whenNotificationIsNotPending() {
        notification.setStatus(NotificationStatus.FAILED);

        when(repository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.send(1L))
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    @Test
    void fail_shouldMarkNotificationAsFailed() {
        var request = new FailNotificationRequest("SMTP error");

        when(repository.findById(1L)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(any(Notification.class))).thenReturn(response);

        notificationService.fail(1L, request);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getFailureReason()).isEqualTo("SMTP error");
    }

    @Test
    void cancel_shouldCancelPendingNotification() {
        when(repository.findById(1L)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(any(Notification.class))).thenReturn(response);

        notificationService.cancel(1L);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
    }
}