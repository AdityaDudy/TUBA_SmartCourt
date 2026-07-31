package in.tubalaw.courtos.modules.tasks.repository;

import in.tubalaw.courtos.modules.tasks.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByTenantId(String tenantId);
    List<Task> findAllByTenantIdAndStatus(String tenantId, String status);
    List<Task> findAllByTenantIdAndDone(String tenantId, boolean done);
    long countByTenantIdAndDone(String tenantId, boolean done);
    long countByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT t FROM Task t WHERE t.tenantId = :tenantId AND (LOWER(t.assignedTo) LIKE LOWER(CONCAT('%', :userName, '%')) OR LOWER(t.createdBy) LIKE LOWER(CONCAT('%', :userName, '%')))")
    List<Task> findAllByTenantIdAndUserScope(String tenantId, String userName);

    @Query("SELECT t FROM Task t WHERE t.tenantId = :tenantId AND t.status = :status AND (LOWER(t.assignedTo) LIKE LOWER(CONCAT('%', :userName, '%')) OR LOWER(t.createdBy) LIKE LOWER(CONCAT('%', :userName, '%')))")
    List<Task> findAllByTenantIdAndStatusAndUserScope(String tenantId, String status, String userName);
}
