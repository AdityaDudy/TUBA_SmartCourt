package in.tubalaw.courtos.modules.notifications.service;

import in.tubalaw.courtos.modules.notifications.entity.Notification;
import in.tubalaw.courtos.modules.notifications.repository.NotificationRepository;
import in.tubalaw.courtos.common.sse.SsePublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SsePublisherService ssePublisherService;

    @Transactional
    public Notification sendNotification(String tenantId, Long userId, String title, String message, String type, String link) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type == null ? "info" : type)
                .link(link)
                .read(false)
                .build();
        notification.setTenantId(tenantId == null ? "default" : tenantId);
        Notification saved = notificationRepository.save(notification);
        ssePublisherService.publishNotificationEvent(saved);
        return saved;
    }
}
