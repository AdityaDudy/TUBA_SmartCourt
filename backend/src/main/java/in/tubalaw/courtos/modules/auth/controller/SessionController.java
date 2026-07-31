package in.tubalaw.courtos.modules.auth.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.auth.entity.UserSession;
import in.tubalaw.courtos.modules.auth.repository.UserSessionRepository;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final UserSessionRepository userSessionRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;

    private static final String TENANT = "default";

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllSessions() {
        java.time.Instant activeThreshold = java.time.Instant.now().minus(6, java.time.temporal.ChronoUnit.HOURS);
        List<UserSession> sessions = userSessionRepository.findAllByTenantId(TENANT).stream()
                .filter(s -> s.getLastActive().isAfter(activeThreshold))
                .collect(Collectors.toList());
        // Sort newest sessions first
        sessions.sort((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()));

        String currentIp = request.getRemoteAddr();
        String currentUserAgent = request.getHeader("User-Agent");
        boolean matchedCurrent = false;

        List<Map<String, Object>> response = new java.util.ArrayList<>();
        for (UserSession s : sessions) {
            boolean isCurrent = false;
            if (!matchedCurrent && currentIp.equals(s.getIpAddress()) && currentUserAgent.equals(s.getUserAgent())) {
                isCurrent = true;
                matchedCurrent = true;
            }
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", s.getId());
            map.put("userId", s.getUser().getId());
            map.put("userEmail", s.getUser().getEmail());
            map.put("user", s.getUser().getName());
            map.put("device", s.getDevice() == null ? "Unknown Device" : s.getDevice());
            map.put("ip", s.getIpAddress() == null ? "0.0.0.0" : s.getIpAddress());
            map.put("location", s.getLocation() == null ? "Unknown" : s.getLocation());
            map.put("started", s.getCreatedAt().toString());
            map.put("current", isCurrent);
            response.add(map);
        }

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> killSession(@PathVariable String sessionId) {
        userSessionRepository.findById(sessionId).ifPresent(s -> {
            userSessionRepository.delete(s);
            auditLogService.log(TENANT, s.getUser().getId(), s.getUser().getEmail(), "Session Terminated", "Session",
                    sessionId,
                    "Session manually terminated by administrator. Device: " + s.getDevice(), request.getRemoteAddr(),
                    "MEDIUM");
        });
        return ResponseEntity.ok(ApiResponse.ok(null, "Session terminated successfully"));
    }

    @DeleteMapping("/kill-all")
    public ResponseEntity<ApiResponse<Void>> killAllSessions() {
        List<UserSession> sessions = userSessionRepository.findAllByTenantId(TENANT);
        String currentIp = request.getRemoteAddr();
        String currentUserAgent = request.getHeader("User-Agent");

        // Delete all sessions except the current one
        for (UserSession s : sessions) {
            boolean isCurrent = currentIp.equals(s.getIpAddress()) && currentUserAgent.equals(s.getUserAgent());
            if (!isCurrent) {
                userSessionRepository.delete(s);
                auditLogService.log(TENANT, s.getUser().getId(), s.getUser().getEmail(), "Session Terminated",
                        "Session", s.getId(),
                        "Session terminated via Kill All. Device: " + s.getDevice(), currentIp, "MEDIUM");
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "All other sessions terminated"));
    }
}
