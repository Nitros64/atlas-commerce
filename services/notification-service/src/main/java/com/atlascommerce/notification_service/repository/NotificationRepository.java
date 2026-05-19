package com.atlascommerce.notification_service.repository;

import com.atlascommerce.notification_service.entity.Notification;
import com.atlascommerce.notification_service.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByStatus(NotificationStatus status);
}