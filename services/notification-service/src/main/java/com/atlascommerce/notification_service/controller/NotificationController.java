package com.atlascommerce.notification_service.controller;

import com.atlascommerce.notification_service.dto.*;
import com.atlascommerce.notification_service.enums.NotificationStatus;
import com.atlascommerce.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public NotificationResponse getById(@PathVariable Long id) {
        return notificationService.getById(id);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public List<NotificationResponse> getByUserId(@PathVariable Long userId) {
        return notificationService.getByUserId(userId);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public List<NotificationResponse> getByStatus(@PathVariable NotificationStatus status) {
        return notificationService.getByStatus(status);
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public NotificationResponse send(@PathVariable Long id) {
        return notificationService.send(id);
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public NotificationResponse fail(
            @PathVariable Long id,
            @Valid @RequestBody FailNotificationRequest request
    ) {
        return notificationService.fail(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public NotificationResponse cancel(@PathVariable Long id) {
        return notificationService.cancel(id);
    }
}