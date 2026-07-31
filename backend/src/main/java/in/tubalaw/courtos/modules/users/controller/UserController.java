package in.tubalaw.courtos.modules.users.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.auth.entity.User;
import in.tubalaw.courtos.modules.auth.entity.UserSession;
import in.tubalaw.courtos.modules.auth.repository.UserRepository;
import in.tubalaw.courtos.modules.auth.repository.UserSessionRepository;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.settings.entity.SystemSetting;
import in.tubalaw.courtos.modules.settings.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private final SystemSettingRepository systemSettingRepository;

    private static final String TENANT = "default";

    @GetMapping
    @PreAuthorize("hasAnyAuthority('manage_users', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> list() {
        List<User> users = userRepo.findAllByTenantId(TENANT);
        users.forEach(u -> u.setPasswordHash(null)); // never expose hash
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('manage_users', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<User>> getById(@PathVariable Long id) {
        return userRepo.findById(id).map(u -> {
            u.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.ok(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyAuthority('manage_users', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<User>> invite(@RequestBody Map<String, Object> req) {
        User user = new User();
        user.setTenantId(TENANT);
        user.setName((String) req.get("name"));
        user.setEmail((String) req.get("email"));
        user.setRole(req.getOrDefault("role", "advocate").toString());
        user.setDepartment((String) req.get("dept"));
        user.setDesignation((String) req.get("designation"));
        user.setMobile((String) req.get("mobile"));
        user.setBarCouncilNo((String) req.get("barCouncilNo"));
        if (req.containsKey("password") && req.get("password") != null && !((String) req.get("password")).isBlank()) {
            user.setPasswordHash(passwordEncoder.encode((String) req.get("password")));
        } else {
            user.setPasswordHash(passwordEncoder.encode("Welcome@123"));
        }
        user.setStatus("active");

        // Default initial permissions for the role
        String roleKey = "role.permissions." + user.getRole().toLowerCase().replace("sr. ", "senior");
        String permsStr = systemSettingRepository.findById(roleKey)
                .map(SystemSetting::getValue)
                .orElse("");
        if (!permsStr.isEmpty()) {
            user.setPermissions(permsStr.split(","));
        } else {
            // fallback
            user.setPermissions(new String[]{"view_all","view_docs"});
        }

        String name = user.getName();
        String[] parts = name.split(" ");
        user.setInitials(parts.length > 1 ? ("" + parts[0].charAt(0) + parts[parts.length-1].charAt(0)).toUpperCase() : name.substring(0, Math.min(2, name.length())).toUpperCase());
        
        // Random nice gradient matching mockup
        String[] gradients = {
            "linear-gradient(135deg,#b45309,#d97706)", // amber
            "linear-gradient(135deg,#0d6637,#16a34a)", // green
            "linear-gradient(135deg,#0f766e,#0d9488)", // teal
            "linear-gradient(135deg,#6b21a8,#9333ea)", // purple
            "linear-gradient(135deg,#4338ca,#4f46e5)"  // indigo
        };
        int idx = (int) (Math.random() * gradients.length);
        user.setGradient(gradients[idx]);

        User saved = userRepo.save(user);
        saved.setPasswordHash(null);

        auditLogService.log(TENANT, null, null, "Create User", "User", saved.getId().toString(), 
            "Created user " + saved.getName() + " (" + saved.getEmail() + ") as " + saved.getRole(), request.getRemoteAddr(), "LOW");

        return ResponseEntity.ok(ApiResponse.ok(saved, "User " + user.getName() + " created successfully!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> update(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return userRepo.findById(id).map(u -> {
            String oldRole = u.getRole();
            if (updates.containsKey("name"))         u.setName((String) updates.get("name"));
            if (updates.containsKey("role"))         u.setRole((String) updates.get("role"));
            if (updates.containsKey("department"))   u.setDepartment((String) updates.get("department"));
            if (updates.containsKey("designation"))  u.setDesignation((String) updates.get("designation"));
            if (updates.containsKey("mobile"))       u.setMobile((String) updates.get("mobile"));
            if (updates.containsKey("status"))       u.setStatus((String) updates.get("status"));
            if (updates.containsKey("barCouncilNo")) u.setBarCouncilNo((String) updates.get("barCouncilNo"));
            if (updates.containsKey("avatar"))       u.setAvatar((String) updates.get("avatar"));
            if (updates.containsKey("password") && updates.get("password") != null && !((String) updates.get("password")).isBlank()) {
                u.setPasswordHash(passwordEncoder.encode((String) updates.get("password")));
            }
            
            User saved = userRepo.save(u);
            saved.setPasswordHash(null);

            if (!oldRole.equalsIgnoreCase(saved.getRole())) {
                auditLogService.log(TENANT, saved.getId(), saved.getEmail(), "Role Changed", "User", saved.getId().toString(), 
                    "Changed role for " + saved.getName() + " from " + oldRole + " to " + saved.getRole(), request.getRemoteAddr(), "MEDIUM");
            } else {
                auditLogService.log(TENANT, saved.getId(), saved.getEmail(), "Update Profile", "User", saved.getId().toString(), 
                    "Updated profile details for " + saved.getName(), request.getRemoteAddr(), "LOW");
            }

            return ResponseEntity.ok(ApiResponse.ok(saved, "User updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable Long id) {
        userRepo.findById(id).ifPresent(u -> { 
            u.setStatus("inactive"); 
            userRepo.save(u); 
            auditLogService.log(TENANT, u.getId(), u.getEmail(), "User Suspended", "User", u.getId().toString(), 
                "Suspended user: " + u.getName(), request.getRemoteAddr(), "MEDIUM");
        });
        return ResponseEntity.ok(ApiResponse.ok(null, "User suspended."));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        userRepo.findById(id).ifPresent(u -> { 
            u.setStatus("active"); 
            userRepo.save(u); 
            auditLogService.log(TENANT, u.getId(), u.getEmail(), "User Activated", "User", u.getId().toString(), 
                "Activated user: " + u.getName(), request.getRemoteAddr(), "LOW");
        });
        return ResponseEntity.ok(ApiResponse.ok(null, "User activated."));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAnyAuthority('manage_roles', 'manage_users', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String[]>> updatePermissions(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body) {
        return userRepo.findById(id).map(u -> {
            List<String> perms = body.get("permissions");
            u.setPermissions(perms.toArray(new String[0]));
            User saved = userRepo.save(u);
            auditLogService.log(TENANT, saved.getId(), saved.getEmail(), "Permissions Updated", "User", saved.getId().toString(), 
                "Updated custom permissions for " + saved.getName(), request.getRemoteAddr(), "HIGH");
            return ResponseEntity.ok(ApiResponse.ok(saved.getPermissions()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/roles/permissions")
    @PreAuthorize("hasAnyAuthority('manage_roles', 'manage_users', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getRolePermissions() {
        Map<String, List<String>> map = new java.util.HashMap<>();
        for (String role : List.of("admin", "senior", "advocate", "clerk", "readonly")) {
            String val = systemSettingRepository.findById("role.permissions." + role)
                    .map(SystemSetting::getValue)
                    .orElse("");
            map.put(role, val.isEmpty() ? List.of() : List.of(val.split(",")));
        }
        return ResponseEntity.ok(ApiResponse.ok(map));
    }

    @PutMapping("/roles/{role}/permissions")
    @PreAuthorize("hasAnyAuthority('manage_roles', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @PathVariable String role,
            @RequestBody Map<String, List<String>> body) {
        List<String> perms = body.get("permissions");
        if (perms == null) {
            return ResponseEntity.badRequest().build();
        }

        // Save to system settings as default for this role
        String roleKey = "role.permissions." + role.toLowerCase().replace("sr. ", "senior");
        systemSettingRepository.save(SystemSetting.builder()
                .key(roleKey)
                .value(String.join(",", perms))
                .build());

        List<User> users = userRepo.findAllByRoleAndTenantId(role, TENANT);
        for (User u : users) {
            u.setPermissions(perms.toArray(new String[0]));
            userRepo.save(u);
        }
        auditLogService.log(TENANT, 0L, "admin@smartcourt.com", "Role Permissions Updated", "Role", role, 
            "Updated default permissions for role " + role + " (applied to " + users.size() + " users)", request.getRemoteAddr(), "HIGH");
        return ResponseEntity.ok(ApiResponse.ok(null, "Role permissions updated successfully"));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> sessions(@PathVariable Long id) {
        java.time.Instant activeThreshold = java.time.Instant.now().minus(6, java.time.temporal.ChronoUnit.HOURS);
        List<UserSession> sessions = userSessionRepository.findAllByUserIdAndTenantId(id, TENANT).stream()
                .filter(s -> s.getLastActive().isAfter(activeThreshold))
                .collect(Collectors.toList());
        String currentIp = request.getRemoteAddr();
        String currentUserAgent = request.getHeader("User-Agent");

        List<Map<String, Object>> response = sessions.stream().map(s -> {
            boolean isCurrent = s.getIpAddress().equals(currentIp) && s.getUserAgent().equals(currentUserAgent);
            return Map.<String, Object>of(
                "id", s.getId(),
                "user", s.getUser().getName(),
                "device", s.getDevice() == null ? "Unknown Device" : s.getDevice(),
                "ip", s.getIpAddress() == null ? "0.0.0.0" : s.getIpAddress(),
                "location", s.getLocation() == null ? "Unknown" : s.getLocation(),
                "started", s.getCreatedAt().toString(),
                "current", isCurrent
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
