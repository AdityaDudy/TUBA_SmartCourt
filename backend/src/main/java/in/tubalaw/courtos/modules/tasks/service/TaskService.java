package in.tubalaw.courtos.modules.tasks.service;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.SpecificationBuilder;
import in.tubalaw.courtos.modules.notifications.service.NotificationService;
import in.tubalaw.courtos.modules.tasks.entity.Task;
import in.tubalaw.courtos.modules.tasks.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repo;
    private final NotificationService notificationService;
    private static final String TENANT = "default";

    // ── Paginated list ────────────────────────────────────────────────────

    /**
     * Scope-aware, filtered page of tasks.
     *
     * @param status optional status filter
     * @param search optional free-text search across title / description / assignedTo
     */
    public Page<Task> list(String status, String search, Pageable pageable) {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        SecurityScopeContext.DataScope scope =
                user != null ? user.getScope() : SecurityScopeContext.DataScope.OWN;

        Specification<Task> spec = SpecificationBuilder.tenantEq(TENANT);

        if (status != null && !status.isBlank())
            spec = spec.and(SpecificationBuilder.fieldEq("status", status));

        if (search != null && !search.isBlank())
            spec = spec.and(SpecificationBuilder.multiFieldSearch(search, "title", "description", "assignedTo"));

        // OWN scope: limit to tasks assigned to or created by the logged-in user
        if (scope == SecurityScopeContext.DataScope.OWN && user != null) {
            String userName = extractNameFromEmail(user.getEmail());
            final String name = userName;
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("assignedTo")), "%" + name.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("createdBy")),  "%" + name.toLowerCase() + "%")
            ));
        }

        return repo.findAll(spec, pageable);
    }

    // ── Non-paginated helpers ─────────────────────────────────────────────

    public Task getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    @Transactional
    public Task create(Task task) {
        task.setTenantId(TENANT);
        Task saved = repo.save(task);
        notificationService.sendNotification(TENANT, null, "New Task Assigned",
                "Task: " + saved.getTitle() + " assigned to " + saved.getAssignedTo(), "task", "/app/tasks");
        return saved;
    }

    @Transactional
    public Task update(Long id, Task updates) {
        Task existing = getById(id);
        if (updates.getTitle()       != null) existing.setTitle(updates.getTitle());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getAssignedTo()  != null) existing.setAssignedTo(updates.getAssignedTo());
        if (updates.getPriority()    != null) existing.setPriority(updates.getPriority());
        if (updates.getDueDate()     != null) existing.setDueDate(updates.getDueDate());
        if (updates.getStatus()      != null) {
            existing.setStatus(updates.getStatus());
            existing.setDone("Done".equalsIgnoreCase(updates.getStatus()));
        }
        Task saved = repo.save(existing);
        if ("Done".equalsIgnoreCase(saved.getStatus())) {
            notificationService.sendNotification(TENANT, null, "Task Completed",
                    "Task: " + saved.getTitle() + " has been marked complete.", "task", "/app/tasks");
        }
        return saved;
    }

    @Transactional
    public Task toggleDone(Long id) {
        Task task = getById(id);
        task.setDone(!task.isDone());
        task.setStatus(task.isDone() ? "Done" : "To Do");
        Task saved = repo.save(task);
        if (saved.isDone()) {
            notificationService.sendNotification(TENANT, null, "Task Completed",
                    "Task: " + saved.getTitle() + " has been marked complete.", "task", "/app/tasks");
        } else {
            notificationService.sendNotification(TENANT, null, "Task Reopened",
                    "Task: " + saved.getTitle() + " has been reopened.", "task", "/app/tasks");
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
        repo.deleteById(id);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private static String extractNameFromEmail(String email) {
        if (email == null) return "";
        return email.split("@")[0].replace(".", " ");
    }
}
