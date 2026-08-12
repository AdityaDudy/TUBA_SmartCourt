package in.tubalaw.courtos.modules.diary.service;

import in.tubalaw.courtos.modules.auth.entity.User;
import in.tubalaw.courtos.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryScopeResolver {

    private final UserRepository userRepository;

    public List<Long> resolveUserIds(User current, String scope, Long memberId) {
        if (current == null) {
            return List.of();
        }

        List<String> perms = current.getPermissions() != null
                ? Arrays.asList(current.getPermissions())
                : List.of();

        boolean canOrg = perms.contains("scope_org") || "admin".equalsIgnoreCase(current.getRole());
        boolean canTeam = perms.contains("scope_team") || canOrg;

        String tenantId = current.getTenantId();

        if ("org".equalsIgnoreCase(scope) && canOrg) {
            List<User> orgUsers = userRepository.findAllByTenantId(tenantId);
            List<Long> orgUserIds = orgUsers.stream().map(User::getId).collect(Collectors.toList());
            if (memberId != null) {
                return orgUserIds.contains(memberId) ? List.of(memberId) : List.of(current.getId());
            }
            return orgUserIds;
        }

        if ("team".equalsIgnoreCase(scope) && canTeam) {
            String dept = current.getDepartment();
            List<User> teamUsers = (dept != null && !dept.isBlank())
                    ? userRepository.findAllByTenantIdAndDepartment(tenantId, dept)
                    : List.of(current);

            List<Long> teamUserIds = teamUsers.stream().map(User::getId).collect(Collectors.toList());
            if (teamUserIds.isEmpty()) {
                teamUserIds = List.of(current.getId());
            }
            if (memberId != null) {
                return teamUserIds.contains(memberId) ? List.of(memberId) : List.of(current.getId());
            }
            return teamUserIds;
        }

        // Default: own scope or insufficient permissions
        return List.of(current.getId());
    }
}
