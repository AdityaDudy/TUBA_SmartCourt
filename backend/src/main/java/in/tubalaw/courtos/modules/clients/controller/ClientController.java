package in.tubalaw.courtos.modules.clients.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.clients.entity.Client;
import in.tubalaw.courtos.modules.clients.service.ClientService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    @GetMapping
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_clients', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Client>>> list(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.list(type)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_clients', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Client>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.search(q)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_clients', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Client>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('manage_clients', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Client>> create(@RequestBody Client client) {
        Client saved = clientService.create(client);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Client Created", "Client", saved.getId().toString(),
                "Created client: " + saved.getName() + " (" + saved.getType() + ")", request.getRemoteAddr(), "LOW");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Client added successfully!"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('manage_clients', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Client>> update(@PathVariable Long id, @RequestBody Client client) {
        Client saved = clientService.update(id, client);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Client Updated", "Client", id.toString(),
                "Updated client: " + saved.getName(), request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(saved, "Client updated."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('delete_clients', 'manage_clients', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Client client = clientService.getById(id);
        clientService.delete(id);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Client Deleted", "Client", id.toString(),
                "Deleted client: " + client.getName(), request.getRemoteAddr(), "HIGH");
        return ResponseEntity.ok(ApiResponse.ok(null, "Client deleted."));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_clients', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(clientService.summary()));
    }
}
