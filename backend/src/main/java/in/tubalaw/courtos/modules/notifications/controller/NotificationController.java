package in.tubalaw.courtos.modules.notifications.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.common.sse.SsePublisherService;
import in.tubalaw.courtos.modules.notifications.entity.Notification;
import in.tubalaw.courtos.modules.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repo;
    private final SsePublisherService ssePublisherService;
    private static final String TENANT = "default";

    @GetMapping("/stream")
    public SseEmitter stream() {
        return ssePublisherService.registerNotificationEmitter();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(repo.findAllByTenantIdOrderByCreatedAtDesc(TENANT)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(repo.countByTenantIdAndRead(TENANT, false)));
    }

    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setRead(true);
            repo.save(n);
        });
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        repo.markAllRead(TENANT);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
