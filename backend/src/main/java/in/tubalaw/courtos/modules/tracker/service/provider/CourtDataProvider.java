package in.tubalaw.courtos.modules.tracker.service.provider;

import in.tubalaw.courtos.modules.tracker.dto.CaseDetailResponse;
import in.tubalaw.courtos.modules.tracker.dto.CaseSearchResultDto;

import java.util.List;

/**
 * Abstraction boundary between TrackerService/ScrapeWorker and any concrete data source.
 *
 * Current implementations:
 *   - EcourtsScraperProvider  (Jsoup + Playwright, default)
 *   - EcourtsIndiaApiProvider (official partner API, activated via tracker.provider.type=ecourtsindia)
 */
public interface CourtDataProvider {

    /**
     * Fetch full case details for the given CNR from the data source.
     *
     * @throws ProviderException with ErrorCode.CAPTCHA_REQUIRED if user interaction is needed
     * @throws ProviderException with ErrorCode.NOT_FOUND if the CNR returns no results
     * @throws ProviderException with ErrorCode.SERVICE_UNAVAILABLE on network/parse failure
     */
    CaseDetailResponse fetchByCnr(String cnr) throws ProviderException;

    /**
     * Search for cases by case number, returning a lightweight candidate list.
     * The caller is expected to present these to the user, who then selects one
     * and feeds its {@code cnr} back into {@link #fetchByCnr(String)}.
     *
     * @param value      The case number query string, e.g. "CS(OS) 403/2026"
     * @param courtCode  Optional eCourtsIndia court code to narrow the search, e.g. "DLHC01"
     * @return           Ordered list of matched cases (may be empty, never null)
     * @throws ProviderException with ErrorCode.NOT_FOUND if the provider does not support this operation
     * @throws ProviderException with ErrorCode.SERVICE_UNAVAILABLE on network/parse failure
     */
    List<CaseSearchResultDto> searchByCaseNumber(String value, String courtCode) throws ProviderException;

    /** Quick health check — used by ScrapeWorker to skip processing if source is known down */
    boolean isAvailable();
}
