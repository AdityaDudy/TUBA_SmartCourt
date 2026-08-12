package in.tubalaw.courtos.modules.tasks.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.common.util.PagedApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.tasks.entity.Task;
import in.tubalaw.courtos.modules.tasks.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    @GetMapping
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_tasks', 'manage_tasks_close', 'ROLE_ADMIN')")
    public ResponseEntity<PagedApiResponse<Task>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order) {
        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC, sort));
        Page<Task> result = taskService.list(status, search, pageable);
        return ResponseEntity.ok(PagedApiResponse.of(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_tasks', 'manage_tasks_close', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Task>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('manage_tasks', 'manage_tasks_assign', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Task>> create(@RequestBody Task task) {
        Task saved = taskService.create(task);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Task Created", "Task", saved.getId().toString(),
                "Created task: " + saved.getTitle() + " (assigned to: " + saved.getAssignedTo() + ")",
                request.getRemoteAddr(), "LOW");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Task created!"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('manage_tasks', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Task>> update(@PathVariable Long id, @RequestBody Task task) {
        Task saved = taskService.update(id, task);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Task Updated", "Task", id.toString(),
                "Updated task: " + saved.getTitle(), request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('manage_tasks', 'manage_tasks_close', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Task>> toggle(@PathVariable Long id) {
        Task t = taskService.toggleDone(id);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                t.isDone() ? "Task Completed" : "Task Reopened", "Task", id.toString(),
                (t.isDone() ? "Marked complete: " : "Reopened: ") + t.getTitle(),
                request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(t, t.isDone() ? "Task complete!" : "Task reopened."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('manage_tasks', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Task task = taskService.getById(id);
        taskService.delete(id);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Task Deleted", "Task", id.toString(),
                "Deleted task: " + task.getTitle(), request.getRemoteAddr(), "MEDIUM");
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
