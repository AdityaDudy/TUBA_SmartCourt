package in.tubalaw.courtos.modules.tracker.service.resolver;

import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for resolving a Case Number (caseType, number, year, courtCode) into a CNR
 * by looking up the registered Matter in the local database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CnrResolverService {

    private final MatterRepository matterRepository;

    /**
     * Resolves a case number string into a CNR using the local database matters table.
     *
     * @param caseType E.g. "CS(OS)"
     * @param number   E.g. "403"
     * @param year     E.g. "2026"
     * @return Optional containing resolved CNR from local Matter record
     */
    public Optional<String> resolve(String caseType, String number, String year) {
        String searchQuery = formatSearchQuery(caseType, number, year);
        log.info("[CnrResolverService] Resolving CNR from database for query: '{}'", searchQuery);

        List<Matter> matches = matterRepository.findByTenantIdAndCaseNoMatching("default", searchQuery);
        if (matches.isEmpty()) {
            matches = matterRepository.findByCaseNoLike("default", number.trim() + "/" + year.trim());
        }

        for (Matter m : matches) {
            if (m.getCnrNumber() != null && !m.getCnrNumber().isBlank()) {
                String cnr = m.getCnrNumber().trim().toUpperCase();
                log.info("[CnrResolverService] Resolved CNR '{}' from Matter table for case number '{}'", cnr, searchQuery);
                return Optional.of(cnr);
            }
        }

        log.warn("[CnrResolverService] No matter record with valid CNR found for case number: '{}'", searchQuery);
        return Optional.empty();
    }

    private String formatSearchQuery(String caseType, String number, String year) {
        if (caseType != null && !caseType.isBlank()) {
            return caseType.trim() + " " + number.trim() + "/" + year.trim();
        }
        return number.trim() + "/" + year.trim();
    }
}
