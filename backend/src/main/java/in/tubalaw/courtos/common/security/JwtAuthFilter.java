package in.tubalaw.courtos.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                Claims claims = jwtService.parseToken(token);
                String email = claims.getSubject();
                String role = claims.get("role", String.class);
                String department = claims.get("department", String.class);
                String scope = claims.get("scope", String.class);
                Number uidNum = claims.get("uid", Number.class);
                Long userId = uidNum != null ? uidNum.longValue() : null;

                List<?> rawPerms = claims.get("perms", List.class);
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }
                if (rawPerms != null) {
                    for (Object perm : rawPerms) {
                        if (perm instanceof String p) {
                            authorities.add(new SimpleGrantedAuthority(p));
                        }
                    }
                }
                if (scope != null) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope.toUpperCase()));
                }

                var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);

                Map<String, Object> detailsMap = new java.util.HashMap<>();
                detailsMap.put("uid", userId);
                detailsMap.put("role", role);
                detailsMap.put("department", department);
                detailsMap.put("scope", scope);

                auth.setDetails(detailsMap);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            System.err.println("JWT Validation Error: " + e.getMessage());
            e.printStackTrace();
        }

        chain.doFilter(request, response);
    }
}
