package in.tubalaw.courtos.modules.tracker.service.resolver;

import in.tubalaw.courtos.modules.tracker.service.provider.ProviderException;

import java.util.Optional;

/**
 * Interface for court-specific direct portal fallback resolvers.
 * Used when eCourtsIndia search API does not return a CNR for a given case number.
 */
public interface CourtCnrResolver {

    /**
     * Checks if this resolver supports the specified court code (e.g. "DLHC01").
     */
    boolean supports(String courtCode);

    /**
     * Attempts to resolve the 16-character CNR for the given case details directly from the court's portal.
     *
     * @param caseType E.g. "CS(OS)", "WP(C)", "BAIL APPL."
     * @param number   E.g. "403"
     * @param year     E.g. "2026"
     * @return Optional containing the resolved CNR string if found
     */
    Optional<String> resolveCnr(String caseType, String number, String year) throws ProviderException;
}
