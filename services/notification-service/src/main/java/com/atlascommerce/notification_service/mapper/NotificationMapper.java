package com.atlascommerce.notification_service.mapper;

import com.atlascommerce.notification_service.dto.NotificationResponse;
import com.atlascommerce.notification_service.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getFailureReason(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}