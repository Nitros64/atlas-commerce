package com.atlascommerce.notification_service.service;

import com.atlascommerce.notification_service.dto.*;
import com.atlascommerce.notification_service.entity.Notification;
import com.atlascommerce.notification_service.enums.NotificationStatus;
import com.atlascommerce.notification_service.exception.InvalidNotificationStateException;
import com.atlascommerce.notification_service.exception.NotificationNotFoundException;
import com.atlascommerce.notification_service.mapper.NotificationMapper;
import com.atlascommerce.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.userId())
                .channel(request.channel())
                .recipient(request.recipient())
                .subject(request.subject())
                .message(request.message())
                .status(NotificationStatus.PENDING)
                .build();

        return mapper.toResponse(repository.save(notification));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByUserId(Long userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByStatus(NotificationStatus status) {
        return repository.findByStatus(status)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse send(Long id) {
        Notification notification = findById(id);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new InvalidNotificationStateException("Only PENDING notifications can be sent");
        }

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now());

        return mapper.toResponse(repository.save(notification));
    }

    @Transactional
    public NotificationResponse fail(Long id, FailNotificationRequest request) {
        Notification notification = findById(id);

        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new InvalidNotificationStateException("SENT notifications cannot be marked as FAILED");
        }

        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailureReason(request.failureReason());

        return mapper.toResponse(repository.save(notification));
    }

    @Transactional
    public NotificationResponse cancel(Long id) {
        Notification notification = findById(id);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new InvalidNotificationStateException("Only PENDING notifications can be cancelled");
        }

        notification.setStatus(NotificationStatus.CANCELLED);

        return mapper.toResponse(repository.save(notification));
    }

    private Notification findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}