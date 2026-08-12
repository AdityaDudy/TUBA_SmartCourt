package in.tubalaw.courtos.modules.tasks.repository;

import in.tubalaw.courtos.modules.tasks.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>,
        JpaSpecificationExecutor<Task> {

    List<Task> findAllByTenantId(String tenantId);
    List<Task> findAllByTenantIdAndStatus(String tenantId, String status);
    List<Task> findAllByTenantIdAndDone(String tenantId, boolean done);
    long countByTenantIdAndDone(String tenantId, boolean done);
    long countByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT t FROM Task t WHERE t.tenantId = :tenantId AND (LOWER(t.assignedTo) LIKE LOWER(CONCAT('%', :userName, '%')) OR LOWER(t.createdBy) LIKE LOWER(CONCAT('%', :userName, '%')))")
    List<Task> findAllByTenantIdAndUserScope(String tenantId, String userName);

    @Query("SELECT t FROM Task t WHERE t.tenantId = :tenantId AND t.status = :status AND (LOWER(t.assignedTo) LIKE LOWER(CONCAT('%', :userName, '%')) OR LOWER(t.createdBy) LIKE LOWER(CONCAT('%', :userName, '%')))")
    List<Task> findAllByTenantIdAndStatusAndUserScope(String tenantId, String status, String userName);

    // ── Dashboard aggregation ─────────────────────────────────────────────
    /**
     * Single-query replacement for the N+1 loop in DashboardController.teamPerformance().
     * Groups tasks by assignedTo and counts total vs done.
     */
    @Query("SELECT t.assignedTo as name, COUNT(t) as total, " +
           "SUM(CASE WHEN t.done = true THEN 1L ELSE 0L END) as done " +
           "FROM Task t WHERE t.tenantId = :tenantId GROUP BY t.assignedTo")
    List<TaskUserSummary> summarizeByUser(String tenantId);

    /** Projection returned by summarizeByUser() */
    interface TaskUserSummary {
        String getName();
        long getTotal();
        long getDone();
    }
}

