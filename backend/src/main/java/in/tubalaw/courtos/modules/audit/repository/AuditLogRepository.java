package in.tubalaw.courtos.modules.audit.repository;

import in.tubalaw.courtos.modules.audit.entity.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditEntry, Long> {
    List<AuditEntry> findTop100ByTenantIdOrderByCreatedAtDesc(String tenantId);
    Page<AuditEntry> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
