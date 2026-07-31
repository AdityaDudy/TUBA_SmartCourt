package in.tubalaw.courtos.modules.tracker.service.provider;

import in.tubalaw.courtos.modules.tracker.dto.CaseDetailResponse;
import in.tubalaw.courtos.modules.tracker.dto.CaseSearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Licensed API Provider implementation for CNR lookups (e.g. Legal API Vendor, Devise, Kleopatra, etc.)
 *
 * Activated when tracker.provider.type=api in application.yml
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tracker.provider.type", havingValue = "api")
@RequiredArgsConstructor
public class LicensedApiProvider implements CourtDataProvider {

    @Value("${tracker.provider.api-key:}")
    private String apiKey;

    @Value("${tracker.provider.api-url:https://api.legaldata-vendor.com/v1/cases/cnr/}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;

    @Override
    public CaseDetailResponse fetchByCnr(String cnr) throws ProviderException {
        log.info("[LicensedApiProvider] Fetching CNR {} via API", cnr);

        if (apiKey == null || apiKey.isBlank()) {
            throw new ProviderException(
                ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                "API Key is missing. Please set tracker.provider.api-key in application.yml or environment variable TRACKER_API_KEY."
            );
        }

        try {
            WebClient webClient = webClientBuilder.build();
            CaseDetailResponse response = webClient.get()
                .uri(apiUrl + cnr)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-API-Key", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(CaseDetailResponse.class)
                .block();

            if (response == null) {
                throw new ProviderException(ProviderException.ErrorCode.NOT_FOUND, "No case found for CNR: " + cnr);
            }

            return response;

        } catch (Exception e) {
            log.error("[LicensedApiProvider] Error calling external API for CNR {}: {}", cnr, e.getMessage());
            throw new ProviderException(
                ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                "External Legal API error: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public java.util.List<CaseSearchResultDto> searchByCaseNumber(String value, String courtCode)
            throws ProviderException {
        throw new ProviderException(
                ProviderException.ErrorCode.NOT_FOUND,
                "Case-number search is not supported by this licensed API provider.");
    }
}
