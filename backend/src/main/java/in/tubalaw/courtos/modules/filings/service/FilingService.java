package in.tubalaw.courtos.modules.filings.service;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.SpecificationBuilder;
import in.tubalaw.courtos.modules.filings.entity.Filing;
import in.tubalaw.courtos.modules.filings.repository.FilingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FilingService {

    private final FilingRepository repo;
    private static final String TENANT = "default";

    /**
     * Scope-aware, filtered page of filings.
     * OWN scope filters to filings where advocate name matches the logged-in user.
     *
     * @param status optional status filter
     * @param search optional free-text search across title / court / filingType / advocate
     */
    public Page<Filing> list(String status, String search, Pageable pageable) {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        SecurityScopeContext.DataScope scope =
                user != null ? user.getScope() : SecurityScopeContext.DataScope.OWN;

        Specification<Filing> spec = SpecificationBuilder.tenantEq(TENANT);

        if (status != null && !status.isBlank())
            spec = spec.and(SpecificationBuilder.fieldEq("status", status));

        if (search != null && !search.isBlank())
            spec = spec.and(SpecificationBuilder.multiFieldSearch(search, "title", "court", "filingType", "advocate"));

        if (scope == SecurityScopeContext.DataScope.OWN && user != null) {
            String name = extractNameFromEmail(user.getEmail());
            final String n = name;
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("advocate")), "%" + n.toLowerCase() + "%"));
        }

        return repo.findAll(spec, pageable);
    }

    private static String extractNameFromEmail(String email) {
        if (email == null) return "";
        return email.split("@")[0].replace(".", " ");
    }
}
