package in.tubalaw.courtos.modules.matters.service;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.SpecificationBuilder;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatterService {

    private final MatterRepository repo;
    private static final String TENANT = "default";

    // ── Paginated list (primary entry point for the list controller) ──────

    /**
     * Returns a scoped, filtered, sorted page of matters.
     *
     * @param status   optional status filter (e.g. "Active", "Closed")
     * @param type     optional type filter
     * @param search   optional free-text search across title / clientName / caseNo / cnrNumber
     * @param pageable Spring Pageable (page, size, sort)
     */
    public Page<Matter> list(String status, String type, String search, Pageable pageable) {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        SecurityScopeContext.DataScope scope =
                user != null ? user.getScope() : SecurityScopeContext.DataScope.OWN;

        // Build the base specification
        Specification<Matter> spec = SpecificationBuilder.tenantEq(TENANT);

        if (status != null && !status.isBlank())
            spec = spec.and(SpecificationBuilder.fieldEq("status", status));

        if (type != null && !type.isBlank())
            spec = spec.and(SpecificationBuilder.fieldEq("type", type));

        if (search != null && !search.isBlank())
            spec = spec.and(SpecificationBuilder.multiFieldSearch(search, "title", "clientName", "caseNo", "cnrNumber"));

        // Scope enforcement — OWN: restrict to matters where the advocate name
        // matches the logged-in user (kept as a LIKE match to avoid a data migration;
        // future: replace with advocateUserId column)
        if (scope == SecurityScopeContext.DataScope.OWN && user != null) {
            String advocateName = extractNameFromEmail(user.getEmail());
            final String nameFilter = advocateName;
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("advocate")),  "%" + nameFilter.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("coCounsel")), "%" + nameFilter.toLowerCase() + "%")
            ));
        }
        // TEAM scope intentionally falls through to ORG view until team membership
        // is available on the principal (future work).

        return repo.findAll(spec, pageable);
    }

    // ── Non-paginated helpers (used internally or by other modules) ───────

    public Matter getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Matter", id));
    }

    public List<Matter> search(String q) {
        return repo.search(TENANT, q);
    }

    public List<Matter> findUnlinked() {
        return repo.findUnlinked(TENANT);
    }

    public List<Matter> byClient(Long clientId) {
        return repo.findAllByTenantIdAndClientId(TENANT, clientId);
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    @Transactional
    public Matter create(Matter matter) {
        matter.setTenantId(TENANT);
        return repo.save(matter);
    }

    @Transactional
    public Matter update(Long id, Matter updates) {
        Matter existing = getById(id);
        if (updates.getTitle()              != null) existing.setTitle(updates.getTitle());
        if (updates.getCaseNo()             != null) existing.setCaseNo(updates.getCaseNo());
        if (updates.getCnrNumber()          != null) existing.setCnrNumber(updates.getCnrNumber());
        if (updates.getClientId()           != null) existing.setClientId(updates.getClientId());
        if (updates.getClientName()         != null) existing.setClientName(updates.getClientName());
        if (updates.getCourt()              != null) existing.setCourt(updates.getCourt());
        if (updates.getType()               != null) existing.setType(updates.getType());
        if (updates.getArea()               != null) existing.setArea(updates.getArea());
        if (updates.getNextHearing()        != null) existing.setNextHearing(updates.getNextHearing());
        if (updates.getAdvocate()           != null) existing.setAdvocate(updates.getAdvocate());
        if (updates.getStatus()             != null) existing.setStatus(updates.getStatus());
        if (updates.getStage()              != null) existing.setStage(updates.getStage());
        if (updates.getOppositeParty()      != null) existing.setOppositeParty(updates.getOppositeParty());
        if (updates.getBg()                 != null) existing.setBg(updates.getBg());
        if (updates.getNotes()              != null) existing.setNotes(updates.getNotes());
        if (updates.getFilingDate()         != null) existing.setFilingDate(updates.getFilingDate());
        if (updates.getPriority()           != null) existing.setPriority(updates.getPriority());
        if (updates.getCoCounsel()          != null) existing.setCoCounsel(updates.getCoCounsel());
        if (updates.getOpposingCounsel()    != null) existing.setOpposingCounsel(updates.getOpposingCounsel());
        if (updates.getLimitationDeadline() != null) existing.setLimitationDeadline(updates.getLimitationDeadline());
        if (updates.getRelatedMatterId()    != null) {
            existing.setRelatedMatterId(updates.getRelatedMatterId() == 0 ? null : updates.getRelatedMatterId());
        }
        return repo.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Matter", id));
        repo.deleteById(id);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /** Derives a display-name token from email for OWN-scope LIKE matching. */
    private static String extractNameFromEmail(String email) {
        if (email == null) return "";
        return email.split("@")[0].replace(".", " ");
    }
}
