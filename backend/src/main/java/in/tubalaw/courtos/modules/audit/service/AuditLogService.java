package in.tubalaw.courtos.modules.audit.service;

import in.tubalaw.courtos.common.sse.SsePublisherService;
import in.tubalaw.courtos.modules.audit.entity.AuditEntry;
import in.tubalaw.courtos.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SsePublisherService ssePublisherService;

    @Transactional
    public void log(String tenantId, Long userId, String email, String action, String entity, String entityId,
            String details, String ipAddress, String risk) {
        AuditEntry entry = AuditEntry.builder()
                .tenantId(tenantId == null ? "default" : tenantId)
                .userId(userId)
                .userEmail(email)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .risk(risk == null ? "LOW" : risk)
                .createdAt(Instant.now())
                .build();
        AuditEntry saved = auditLogRepository.save(entry);
        ssePublisherService.publishAuditEvent(saved);
    }
}
