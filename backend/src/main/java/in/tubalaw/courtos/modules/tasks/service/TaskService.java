package in.tubalaw.courtos.modules.tasks.service;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.modules.tasks.entity.Task;
import in.tubalaw.courtos.modules.tasks.repository.TaskRepository;
import in.tubalaw.courtos.modules.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repo;
    private final NotificationService notificationService;
    private static final String TENANT = "default";

    public List<Task> list(String status) {
        in.tubalaw.courtos.common.security.SecurityScopeContext.UserSecurityDetails user =
                in.tubalaw.courtos.common.security.SecurityScopeContext.getCurrentUser();
        in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope scope =
                user != null ? user.getScope() : in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope.OWN;

        if (scope == in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope.ORG) {
            if (status != null && !status.isBlank()) return repo.findAllByTenantIdAndStatus(TENANT, status);
            return repo.findAllByTenantId(TENANT);
        } else {
            String userName = user != null && user.getEmail() != null ? user.getEmail().split("@")[0].replace(".", " ") : "";
            if (status != null && !status.isBlank()) return repo.findAllByTenantIdAndStatusAndUserScope(TENANT, status, userName);
            return repo.findAllByTenantIdAndUserScope(TENANT, userName);
        }
    }

    public Task getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

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
}
