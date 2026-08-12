package in.tubalaw.courtos.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SecurityScopeContext {

    public enum DataScope {
        ORG,
        TEAM,
        OWN
    }

    public static class UserSecurityDetails {
        private final String email;
        private final Long userId;
        private final String role;
        private final String department;
        private final DataScope scope;
        private final Collection<? extends GrantedAuthority> authorities;

        public UserSecurityDetails(String email, Long userId, String role, String department, DataScope scope, Collection<? extends GrantedAuthority> authorities) {
            this.email = email;
            this.userId = userId;
            this.role = role;
            this.department = department;
            this.scope = scope;
            this.authorities = authorities;
        }

        public String getEmail() { return email; }
        public Long getUserId() { return userId; }
        public String getRole() { return role; }
        public String getDepartment() { return department; }
        public DataScope getScope() { return scope; }
        public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    }

    public static UserSecurityDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String email = auth.getName();
        Long userId = null;
        String role = "advocate";
        String department = null;
        DataScope scope = DataScope.OWN;

        if (auth.getDetails() instanceof Map<?, ?> details) {
            if (details.containsKey("uid") && details.get("uid") instanceof Number num) {
                userId = num.longValue();
            }
            if (details.containsKey("role") && details.get("role") instanceof String r) {
                role = r;
            }
            if (details.containsKey("department") && details.get("department") instanceof String d) {
                department = d;
            }
            if (details.containsKey("scope") && details.get("scope") instanceof String s) {
                try {
                    scope = DataScope.valueOf(s.toUpperCase());
                } catch (Exception ignored) { }
            }
        }

        // Admin override always gets ORG scope
        if ("admin".equalsIgnoreCase(role)) {
            scope = DataScope.ORG;
        }

        return new UserSecurityDetails(email, userId, role, department, scope, auth.getAuthorities() != null ? auth.getAuthorities() : Collections.emptyList());
    }

    public static DataScope getCurrentScope() {
        UserSecurityDetails details = getCurrentUser();
        return details != null ? details.getScope() : DataScope.OWN;
    }
}
