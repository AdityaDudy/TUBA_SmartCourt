package in.tubalaw.courtos.modules.notifications.repository;
import in.tubalaw.courtos.modules.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);
    long countByTenantIdAndRead(String tenantId, boolean read);
    @Modifying @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.tenantId = :tenantId")
    void markAllRead(String tenantId);
}
