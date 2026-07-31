package in.tubalaw.courtos.modules.clients.service;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.modules.clients.entity.Client;
import in.tubalaw.courtos.modules.clients.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository repo;
    private static final String TENANT = "default";

    public List<Client> list(String type) {
        if (type != null && !type.isBlank()) return repo.findAllByTenantIdAndType(TENANT, type);
        return repo.findAllByTenantId(TENANT);
    }

    public Client getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    public List<Client> search(String q) {
        return repo.search(TENANT, q);
    }

    @Transactional
    public Client create(Client client) {
        client.setTenantId(TENANT);
        if (client.getCode() == null || client.getCode().isBlank()) {
            long count = repo.count();
            client.setCode("CLT" + String.format("%03d", count + 1));
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
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

        // Extended fields mapping
        if (updates.getDisplayName() != null) existing.setDisplayName(updates.getDisplayName());
        if (updates.getDob() != null) existing.setDob(updates.getDob());
        if (updates.getGender() != null) existing.setGender(updates.getGender());
        if (updates.getFatherSpouseName() != null) existing.setFatherSpouseName(updates.getFatherSpouseName());
        if (updates.getAlternateMobile() != null) existing.setAlternateMobile(updates.getAlternateMobile());
        if (updates.getBillingAddress() != null) existing.setBillingAddress(updates.getBillingAddress());
        if (updates.getIdProofType() != null) existing.setIdProofType(updates.getIdProofType());
        if (updates.getIdProofNumber() != null) existing.setIdProofNumber(updates.getIdProofNumber());
        if (updates.getAssignedAdvocate() != null) existing.setAssignedAdvocate(updates.getAssignedAdvocate());
        if (updates.getClientSince() != null) existing.setClientSince(updates.getClientSince());
        if (updates.getReferralSource() != null) existing.setReferralSource(updates.getReferralSource());

        existing.setVakalatnamaOnFile(updates.isVakalatnamaOnFile());
        existing.setEngagementLetterSigned(updates.isEngagementLetterSigned());
        if (updates.getConflictNotes() != null) existing.setConflictNotes(updates.getConflictNotes());
        existing.setDataConsent(updates.isDataConsent());

        // Corporate Extension
        if (updates.getCin() != null) existing.setCin(updates.getCin());
        if (updates.getRegisteredOfficeAddress() != null) existing.setRegisteredOfficeAddress(updates.getRegisteredOfficeAddress());
        if (updates.getAuthorizedSignatoryName() != null) existing.setAuthorizedSignatoryName(updates.getAuthorizedSignatoryName());
        if (updates.getAuthorizedSignatoryDesignation() != null) existing.setAuthorizedSignatoryDesignation(updates.getAuthorizedSignatoryDesignation());
        if (updates.getIncorporationDate() != null) existing.setIncorporationDate(updates.getIncorporationDate());
        if (updates.getCreatedBy() != null) existing.setCreatedBy(updates.getCreatedBy());

        return repo.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client", id));
        repo.deleteById(id);
    }

    public Map<String, Object> summary() {
        long total    = repo.findAllByTenantId(TENANT).size();
        long active   = repo.findAllByTenantId(TENANT).stream().filter(c -> "active".equals(c.getStatus())).count();
        return Map.of("total", total, "active", active);
    }
}
