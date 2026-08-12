package in.tubalaw.courtos.modules.clients.service;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.common.util.SpecificationBuilder;
import in.tubalaw.courtos.modules.clients.entity.Client;
import in.tubalaw.courtos.modules.clients.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository repo;
    private static final String TENANT = "default";

    // ── Paginated list ────────────────────────────────────────────────────

    /**
     * Filtered, sorted page of clients.
     *
     * @param type   optional type filter ("Individual", "Company", ...)
     * @param search optional free-text search across name / email / mobile
     */
    public Page<Client> list(String type, String search, Pageable pageable) {
        Specification<Client> spec = SpecificationBuilder.tenantEq(TENANT);

        if (type != null && !type.isBlank())
            spec = spec.and(SpecificationBuilder.fieldEq("type", type));

        if (search != null && !search.isBlank())
            spec = spec.and(SpecificationBuilder.multiFieldSearch(search, "name", "email", "mobile"));

        return repo.findAll(spec, pageable);
    }

    // ── Non-paginated helpers ─────────────────────────────────────────────

    public Client getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    // ── Summary (uses aggregate count queries — no full table scan) ───────

    public Map<String, Object> summary() {
        long total  = repo.countByTenantId(TENANT);
        long active = repo.countByTenantIdAndStatus(TENANT, "active");
        return Map.of("total", total, "active", active);
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    @Transactional
    public Client create(Client client) {
        client.setTenantId(TENANT);
        if (client.getCode() == null || client.getCode().isBlank()) {
            long count = repo.count();
            client.setCode("CLT" + String.format("%03d", count + 1));
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            client.setCreatedBy(auth.getName());
        } else {
            client.setCreatedBy("System");
        }
        return repo.save(client);
    }

    @Transactional
    public Client update(Long id, Client updates) {
        Client existing = getById(id);
        if (updates.getName()    != null) existing.setName(updates.getName());
        if (updates.getType()    != null) existing.setType(updates.getType());
        if (updates.getMobile()  != null) existing.setMobile(updates.getMobile());
        if (updates.getEmail()   != null) existing.setEmail(updates.getEmail());
        if (updates.getPan()     != null) existing.setPan(updates.getPan());
        if (updates.getGstin()   != null) existing.setGstin(updates.getGstin());
        if (updates.getAadhar()  != null) existing.setAadhar(updates.getAadhar());
        if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
        if (updates.getNotes()   != null) existing.setNotes(updates.getNotes());
        if (updates.getStatus()  != null) existing.setStatus(updates.getStatus());

        if (updates.getDisplayName()                    != null) existing.setDisplayName(updates.getDisplayName());
        if (updates.getDob()                            != null) existing.setDob(updates.getDob());
        if (updates.getGender()                         != null) existing.setGender(updates.getGender());
        if (updates.getFatherSpouseName()               != null) existing.setFatherSpouseName(updates.getFatherSpouseName());
        if (updates.getAlternateMobile()                != null) existing.setAlternateMobile(updates.getAlternateMobile());
        if (updates.getBillingAddress()                 != null) existing.setBillingAddress(updates.getBillingAddress());
        if (updates.getIdProofType()                    != null) existing.setIdProofType(updates.getIdProofType());
        if (updates.getIdProofNumber()                  != null) existing.setIdProofNumber(updates.getIdProofNumber());
        if (updates.getAssignedAdvocate()               != null) existing.setAssignedAdvocate(updates.getAssignedAdvocate());
        if (updates.getClientSince()                    != null) existing.setClientSince(updates.getClientSince());
        if (updates.getReferralSource()                 != null) existing.setReferralSource(updates.getReferralSource());

        existing.setVakalatnamaOnFile(updates.isVakalatnamaOnFile());
        existing.setEngagementLetterSigned(updates.isEngagementLetterSigned());
        if (updates.getConflictNotes() != null) existing.setConflictNotes(updates.getConflictNotes());
        existing.setDataConsent(updates.isDataConsent());

        if (updates.getCin()                            != null) existing.setCin(updates.getCin());
        if (updates.getRegisteredOfficeAddress()        != null) existing.setRegisteredOfficeAddress(updates.getRegisteredOfficeAddress());
        if (updates.getAuthorizedSignatoryName()        != null) existing.setAuthorizedSignatoryName(updates.getAuthorizedSignatoryName());
        if (updates.getAuthorizedSignatoryDesignation() != null) existing.setAuthorizedSignatoryDesignation(updates.getAuthorizedSignatoryDesignation());
        if (updates.getIncorporationDate()              != null) existing.setIncorporationDate(updates.getIncorporationDate());
        if (updates.getCreatedBy()                      != null) existing.setCreatedBy(updates.getCreatedBy());

        return repo.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client", id));
        repo.deleteById(id);
    }
}
