package in.tubalaw.courtos.common.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SsePublisherService {

    private final List<SseEmitter> auditEmitters = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> notificationEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter registerAuditEmitter() {
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L); // 24 hours
        auditEmitters.add(emitter);
        emitter.onCompletion(() -> auditEmitters.remove(emitter));
        emitter.onTimeout(() -> auditEmitters.remove(emitter));
        emitter.onError((ex) -> auditEmitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to Audit Stream"));
        } catch (IOException ignored) {}
        return emitter;
    }

    public SseEmitter registerNotificationEmitter() {
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);
        notificationEmitters.add(emitter);
        emitter.onCompletion(() -> notificationEmitters.remove(emitter));
        emitter.onTimeout(() -> notificationEmitters.remove(emitter));
        emitter.onError((ex) -> notificationEmitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to Notification Stream"));
        } catch (IOException ignored) {}
        return emitter;
    }

    public void publishAuditEvent(Object auditEntry) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : auditEmitters) {
            try {
                emitter.send(SseEmitter.event().name("audit").data(auditEntry));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        auditEmitters.removeAll(dead);
    }

    public void publishNotificationEvent(Object notification) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : notificationEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        notificationEmitters.removeAll(dead);
    }
}
