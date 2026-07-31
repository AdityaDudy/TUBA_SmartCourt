package in.tubalaw.courtos.modules.diary.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.auth.entity.User;
import in.tubalaw.courtos.modules.auth.repository.UserRepository;
import in.tubalaw.courtos.modules.diary.entity.DiaryEvent;
import in.tubalaw.courtos.modules.diary.service.DiaryScopeResolver;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
interface DiaryEventRepository extends JpaRepository<DiaryEvent, Long> {
    @Query("SELECT d FROM DiaryEvent d WHERE d.tenantId = :tenantId AND YEAR(d.eventDate) = :year AND MONTH(d.eventDate) = :month AND (d.ownerId IN :ownerIds OR d.ownerId IS NULL) ORDER BY d.eventDate ASC, d.eventTime ASC")
    List<DiaryEvent> findByMonthAndOwners(String tenantId, int year, int month, List<Long> ownerIds);

    @Query("SELECT d FROM DiaryEvent d WHERE d.tenantId = :tenantId AND (d.ownerId IN :ownerIds OR d.ownerId IS NULL) ORDER BY d.eventDate ASC, d.eventTime ASC")
    List<DiaryEvent> findByOwners(String tenantId, List<Long> ownerIds);
}

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryEventRepository repo;
    private final UserRepository userRepo;
    private final MatterRepository matterRepo;
    private final DiaryScopeResolver scopeResolver;

    private static final String TENANT = "default";

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String email) {
            Optional<User> userOpt = userRepo.findByEmailAndTenantId(email, TENANT);
            if (userOpt.isPresent())
                return userOpt.get();
        }
        // Fallback to first admin/user if context missing during local dev
        return userRepo.findAllByTenantId(TENANT).stream().findFirst().orElse(null);
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<DiaryEvent>>> events(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "own") String scope,
            @RequestParam(required = false) Long memberId) {

        User current = getCurrentUser();
        List<Long> visibleUserIds = scopeResolver.resolveUserIds(current, scope, memberId);

        List<DiaryEvent> events;
        if (year != null && month != null) {
            events = repo.findByMonthAndOwners(TENANT, year, month, visibleUserIds);
        } else {
            events = repo.findByOwners(TENANT, visibleUserIds);
        }
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/scope-options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getScopeOptions() {
        User current = getCurrentUser();
        Map<String, Object> res = new HashMap<>();

        if (current == null) {
            res.put("canTeam", false);
            res.put("canOrg", false);
            res.put("teamMembers", List.of());
            res.put("orgMembers", List.of());
            return ResponseEntity.ok(ApiResponse.ok(res));
        }

        List<String> perms = current.getPermissions() != null
                ? Arrays.asList(current.getPermissions())
                : List.of();

        boolean canOrg = perms.contains("scope_org") || "admin".equalsIgnoreCase(current.getRole());
        boolean canTeam = perms.contains("scope_team") || canOrg;

        res.put("canTeam", canTeam);
        res.put("canOrg", canOrg);

        if (canTeam) {
            String dept = current.getDepartment();
            List<User> teamUsers = (dept != null && !dept.isBlank())
                    ? userRepo.findAllByTenantIdAndDepartment(TENANT, dept)
                    : List.of(current);
            res.put("teamMembers", teamUsers.stream().map(this::toMemberDto).collect(Collectors.toList()));
        } else {
            res.put("teamMembers", List.of(toMemberDto(current)));
        }

        if (canOrg) {
            List<User> orgUsers = userRepo.findAllByTenantId(TENANT);
            res.put("orgMembers", orgUsers.stream().map(this::toMemberDto).collect(Collectors.toList()));
        } else {
            res.put("orgMembers", List.of());
        }

        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    private Map<String, Object> toMemberDto(User u) {
        return Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "role", u.getRole(),
                "initials",
                u.getInitials() != null ? u.getInitials()
                        : u.getName().substring(0, Math.min(2, u.getName().length())).toUpperCase(),
                "gradient", u.getGradient() != null ? u.getGradient() : "linear-gradient(135deg,#b45309,#d97706)");
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<DiaryEvent>> create(@RequestBody DiaryEvent event) {
        event.setTenantId(TENANT);
        User current = getCurrentUser();

        if (current != null) {
            if (event.getOwnerId() == null) {
                event.setOwnerId(current.getId());
                event.setOwnerName(current.getName());
            }
            event.setCreatedBy(current.getId());
        }

        // Link client details if matterId is present
        if (event.getMatterId() != null) {
            matterRepo.findById(event.getMatterId()).ifPresent(m -> {
                event.setMatterTitle(m.getTitle());
                if (m.getClientId() != null) {
                    event.setClientId(m.getClientId());
                    event.setClientName(m.getClientName());
                }
            });
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(repo.save(event), "Event added to diary."));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<ApiResponse<DiaryEvent>> update(@PathVariable Long id, @RequestBody DiaryEvent updates) {
        return repo.findById(id).map(e -> {
            if (updates.getTitle() != null)
                e.setTitle(updates.getTitle());
            if (updates.getEventDate() != null)
                e.setEventDate(updates.getEventDate());
            if (updates.getEventTime() != null)
                e.setEventTime(updates.getEventTime());
            if (updates.getNotes() != null)
                e.setNotes(updates.getNotes());
            if (updates.getMatterId() != null) {
                e.setMatterId(updates.getMatterId());
                matterRepo.findById(updates.getMatterId()).ifPresent(m -> {
                    e.setMatterTitle(m.getTitle());
                    if (m.getClientId() != null) {
                        e.setClientId(m.getClientId());
                        e.setClientName(m.getClientName());
                    }
                });
            }
            if (updates.getOwnerId() != null) {
                e.setOwnerId(updates.getOwnerId());
                userRepo.findById(updates.getOwnerId()).ifPresent(u -> e.setOwnerName(u.getName()));
            }
            return ResponseEntity.ok(ApiResponse.ok(repo.save(e)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
