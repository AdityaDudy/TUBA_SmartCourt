package in.tubalaw.courtos.modules.matters.service;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatterService {

    private final MatterRepository repo;
    private static final String TENANT = "default";

    public List<Matter> list(String status, String type) {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        SecurityScopeContext.DataScope scope = user != null ? user.getScope() : SecurityScopeContext.DataScope.OWN;

        if (scope == SecurityScopeContext.DataScope.ORG) {
            if (status != null && !status.isBlank()) return repo.findAllByTenantIdAndStatus(TENANT, status);
            if (type   != null && !type.isBlank())   return repo.findAllByTenantIdAndType(TENANT, type);
            return repo.findAllByTenantId(TENANT);
        } else {
            // OWN / TEAM scope filtering: filter by advocate/coCounsel matching logged-in user name
            String userName = user != null ? user.getEmail() : "";
            // Extract clean name if email is used or find user details
            if (user != null && user.getEmail() != null) {
                // If email contains name before @, or fallback to user search
                String emailPrefix = user.getEmail().split("@")[0].replace(".", " ");
                userName = emailPrefix;
            }
            // For robust matching, extract first/last name tokens
            if (status != null && !status.isBlank()) return repo.findAllByTenantIdAndStatusAndAdvocateContaining(TENANT, status, userName);
            if (type   != null && !type.isBlank())   return repo.findAllByTenantIdAndTypeAndAdvocateContaining(TENANT, type, userName);
            return repo.findAllByTenantIdAndAdvocateContaining(TENANT, userName);
        }
    }

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

    @Transactional
    public Matter create(Matter matter) {
        matter.setTenantId(TENANT);
        return repo.save(matter);
    }

    @Transactional
    public Matter update(Long id, Matter updates) {
        Matter existing = getById(id);
        if (updates.getTitle()         != null) existing.setTitle(updates.getTitle());
        if (updates.getCaseNo()        != null) existing.setCaseNo(updates.getCaseNo());
        if (updates.getCnrNumber()     != null) existing.setCnrNumber(updates.getCnrNumber());
        if (updates.getClientId()      != null) existing.setClientId(updates.getClientId());
        if (updates.getClientName()    != null) existing.setClientName(updates.getClientName());
        if (updates.getCourt()         != null) existing.setCourt(updates.getCourt());
        if (updates.getType()          != null) existing.setType(updates.getType());
        if (updates.getArea()          != null) existing.setArea(updates.getArea());
        if (updates.getNextHearing()   != null) existing.setNextHearing(updates.getNextHearing());
        if (updates.getAdvocate()      != null) existing.setAdvocate(updates.getAdvocate());
        if (updates.getStatus()        != null) existing.setStatus(updates.getStatus());
        if (updates.getStage()         != null) existing.setStage(updates.getStage());
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
}
