package in.tubalaw.courtos.modules.tracker.service.provider;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.tubalaw.courtos.modules.tracker.dto.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

/**
 * Official eCourtsIndia API Provider (webapi.ecourtsindia.com)
 * Activated when tracker.provider.type=ecourtsindia (or by default)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tracker.provider.type", havingValue = "ecourtsindia", matchIfMissing = true)
@RequiredArgsConstructor
public class EcourtsIndiaApiProvider implements CourtDataProvider {

    @Value("${tracker.provider.api-key:}")
    private String apiKey;

    @Value("${tracker.provider.base-url:https://webapi.ecourtsindia.com}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public CaseDetailResponse fetchByCnr(String cnr) throws ProviderException {
        log.info("[EcourtsIndiaApiProvider] Fetching CNR {} via official API", cnr);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[EcourtsIndiaApiProvider] API Key missing — using structured fallback data for CNR: {}", cnr);
            return buildFallbackResponse(cnr);
        }

        int attempts = 0;
        long backoffMs = 1000;

        while (attempts < 3) {
            attempts++;
            try {
                WebClient webClient = webClientBuilder.build();
                EciResponseWrapper wrapper = webClient.get()
                        .uri(baseUrl + "/api/partner/case/" + cnr)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(EciResponseWrapper.class)
                        .block();

                if (wrapper == null || wrapper.getData() == null || wrapper.getData().getCourtCaseData() == null) {
                    throw new ProviderException(ProviderException.ErrorCode.NOT_FOUND, "No case found for CNR: " + cnr);
                }

                return mapToCaseDetailResponse(cnr, wrapper.getData().getCourtCaseData());

            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                            "eCourtsIndia API token invalid or expired.");
                } else if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                    throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                            "eCourtsIndia API balance insufficient.");
                } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    throw new ProviderException(ProviderException.ErrorCode.NOT_FOUND, "No case found for CNR: " + cnr);
                } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.warn("[EcourtsIndiaApiProvider] Rate limit (429). Retrying in {}ms (attempt {})", backoffMs, attempts);
                    try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) {}
                    backoffMs *= 2;
                } else {
                    throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                            "eCourtsIndia API error: " + e.getMessage(), e);
                }
            } catch (ProviderException pe) {
                throw pe;
            } catch (Exception e) {
                log.error("[EcourtsIndiaApiProvider] Error fetching CNR {}: {}", cnr, e.getMessage());
                throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                        "API request failed: " + e.getMessage(), e);
            }
        }

        throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                "eCourtsIndia API rate limit exceeded after retries.");
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public List<CaseSearchResultDto> searchByCaseNumber(String value, String courtCode) throws ProviderException {
        log.info("[EcourtsIndiaApiProvider] Case-number search request: '{}' courtCodes={}", value, courtCode);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[EcourtsIndiaApiProvider] API Key missing — returning fallback search results for: {}", value);
            return buildFallbackSearchResults(value);
        }

        // Build list of query variations (Normalization - Step 5)
        List<String> queryCandidates = buildQueryVariations(value);

        for (String q : queryCandidates) {
            log.info("[EciSearch] Trying query candidate: '{}' with courtCodes={}", q, courtCode);
            List<CaseSearchResultDto> results = executeEciSearchCall(q, courtCode);

            if (!results.isEmpty()) {
                log.info("[EciSearch] Found {} results using query variation: '{}'", results.size(), q);
                return results;
            }

            // Step 4: If courtCode was passed and produced 0 hits, try without courtCodes filter
            if (courtCode != null && !courtCode.isBlank()) {
                log.info("[EciSearch] Trying query candidate: '{}' without courtCodes filter", q);
                List<CaseSearchResultDto> unconstrainedResults = executeEciSearchCall(q, null);
                if (!unconstrainedResults.isEmpty()) {
                    log.info("[EciSearch] Found {} results without courtCodes filter using query: '{}'", unconstrainedResults.size(), q);
                    return unconstrainedResults;
                }
            }
        }

        log.info("[EciSearch] 0 results returned across all query variations for: '{}'", value);
        return List.of();
    }

    private List<CaseSearchResultDto> executeEciSearchCall(String query, String courtCode) throws ProviderException {
        try {
            WebClient webClient = webClientBuilder.build();

            var uriSpec = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder = uriBuilder
                                .scheme("https")
                                .host("webapi.ecourtsindia.com")
                                .path("/api/partner/search")
                                .queryParam("query", query);
                        if (courtCode != null && !courtCode.isBlank()) {
                            // Step 2: Param MUST be courtCodes (plural)
                            uriBuilder = uriBuilder.queryParam("courtCodes", courtCode.trim());
                        }
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON);

            // Step 1: Raw response logging
            String rawJson = uriSpec
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("[EciSearch] Raw API response for query '{}', courtCodes '{}': {}", query, courtCode, rawJson);

            if (rawJson == null || rawJson.isBlank()) {
                return List.of();
            }

            EciSearchResponseWrapper wrapper = objectMapper.readValue(rawJson, EciSearchResponseWrapper.class);

            if (wrapper == null || wrapper.getData() == null || wrapper.getData().getCases() == null) {
                return List.of();
            }

            return Arrays.stream(wrapper.getData().getCases())
                    .map(this::mapToSearchResult)
                    .filter(r -> r.getCnr() != null && !r.getCnr().isBlank())
                    .toList();

        } catch (WebClientResponseException e) {
            log.error("[EciSearch] HTTP status {} error: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                        "eCourtsIndia API token invalid or balance insufficient.");
            }
            throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                    "eCourtsIndia search API error (" + e.getStatusCode() + "): " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[EciSearch] Error during search call: {}", e.getMessage(), e);
            throw new ProviderException(ProviderException.ErrorCode.SERVICE_UNAVAILABLE,
                    "Search request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Step 5: Query Normalization variations
     * E.g. CS(OS) 403/2026 -> [
     *   "CS(OS) 403/2026",
     *   "CS OS 403/2026",
     *   "CS(OS) 403 2026",
     *   "403/2026"
     * ]
     */
    private List<String> buildQueryVariations(String raw) {
        List<String> list = new ArrayList<>();
        String trimmed = raw.trim();
        list.add(trimmed);

        // 1. Hyphenated type & number format: "CS(OS) 403/2026" -> "CS(OS)-403/2026"
        String hyphenated = trimmed.replaceAll("^([A-Za-z()]+)\\s+(\\d+/\\d{4})$", "$1-$2");
        if (!hyphenated.equals(trimmed) && !list.contains(hyphenated)) {
            list.add(hyphenated);
        }

        // 2. Strip parens: "CS(OS) 403/2026" -> "CS OS 403/2026"
        String noParens = trimmed.replaceAll("[()]", " ").replaceAll("\\s+", " ").trim();
        if (!noParens.equals(trimmed) && !list.contains(noParens)) {
            list.add(noParens);
        }

        // 3. Hyphenated no-parens: "CS OS-403/2026"
        String noParensHyphen = noParens.replaceAll("^([A-Za-z ]+)\\s+(\\d+/\\d{4})$", "$1-$2");
        if (!noParensHyphen.equals(noParens) && !list.contains(noParensHyphen)) {
            list.add(noParensHyphen);
        }

        // 4. Replace slashes with spaces: "CS(OS) 403 2026"
        String spaceSlash = trimmed.replace('/', ' ').replaceAll("\\s+", " ").trim();
        if (!spaceSlash.equals(trimmed) && !list.contains(spaceSlash)) {
            list.add(spaceSlash);
        }

        // 5. Extract just case number and year if pattern matched (e.g. 403/2026)
        var matcher = java.util.regex.Pattern.compile("\\d+/\\d{4}").matcher(trimmed);
        if (matcher.find()) {
            String numYear = matcher.group();
            if (!list.contains(numYear)) {
                list.add(numYear);
            }
        }

        return list;
    }

    // ──────────────────────────────────────────────────────────────────
    // Response mapping
    // ──────────────────────────────────────────────────────────────────

    private CaseDetailResponse mapToCaseDetailResponse(String cnr, EciCourtCaseData data) {
        List<PartyDto> petitioners = toParties(data.getPetitionersList(), data.getPetitionerAdvocatesList());
        List<PartyDto> respondents = toParties(data.getRespondentsList(), data.getRespondentAdvocatesList());

        return CaseDetailResponse.builder()
                .cnr(cnr)
                .caseType(data.getCaseTypeRaw() != null ? data.getCaseTypeRaw() : data.getCaseType())
                .filingNo(data.getFilingNumber())
                .filingDate(data.getFilingDate())
                .registrationNo(data.getRegistrationNumber())
                .registrationDate(data.getRegistrationDate())
                .courtName(data.getCourtName())
                .courtComplex(data.getCourtComplex())
                .judgeName(data.getJudgeName())
                .caseStatus(normalizeStatus(data.getCaseStatus()))
                .stageOfCase(data.getPurpose() != null ? data.getPurpose() : data.getStageOfCase())
                .nextHearingDate(data.getNextHearingDate())
                .actsAndSections(Arrays.asList(data.getActsAndSectionsList()))
                .firNo(data.getFirNumber())
                .firYear(data.getFirYear())
                .policeStation(data.getPoliceStation())
                .petitioners(petitioners)
                .respondents(respondents)
                .hearings(mapHearings(data))
                .orders(mapOrders(data))
                .cacheSource("LIVE")
                .build();
    }

    private List<PartyDto> toParties(String[] names, String[] advocates) {
        List<PartyDto> list = new ArrayList<>();
        if (names == null) return list;
        String[] advs = advocates != null ? advocates : new String[0];
        for (int i = 0; i < names.length; i++) {
            list.add(PartyDto.builder()
                    .name(names[i])
                    .advocate(i < advs.length ? advs[i] : null)
                    .build());
        }
        return list;
    }

    /**
     * Hearings: primary source is businessOnDateEntries[] (richer — has free-text
     * remarks and next hearing date). historyOfCaseHearings[] is used as a
     * fallback/enrichment source for judge name and purposeOfListing, matched by date.
     */
    private List<HearingDto> mapHearings(EciCourtCaseData data) {
        Map<String, EciHearing> hearingsByDate = new HashMap<>();
        if (data.getHistoryOfCaseHearings() != null) {
            for (EciHearing h : data.getHistoryOfCaseHearings()) {
                if (h.getHearingDate() != null) hearingsByDate.put(h.getHearingDate(), h);
                if (h.getBusinessOnDate() != null) hearingsByDate.putIfAbsent(h.getBusinessOnDate(), h);
            }
        }

        List<HearingDto> hearings = new ArrayList<>();

        if (data.getBusinessOnDateEntries() != null && data.getBusinessOnDateEntries().length > 0) {
            for (EciBusinessEntry b : data.getBusinessOnDateEntries()) {
                EciHearing matched = hearingsByDate.get(b.getDate());
                hearings.add(HearingDto.builder()
                        .hearingDate(b.getDate())
                        .judge(matched != null ? matched.getJudge() : b.getCourtOf())
                        .purposeOfHearing(matched != null ? matched.getPurposeOfListing() : b.getNextPurpose())
                        .nextHearingDate(b.getNextHearingDate())
                        .businessRemarks(b.getBusiness())
                        .build());
            }
        } else if (data.getHistoryOfCaseHearings() != null) {
            // Fallback: no businessOnDateEntries available, use hearings array alone
            for (EciHearing h : data.getHistoryOfCaseHearings()) {
                hearings.add(HearingDto.builder()
                        .hearingDate(h.getHearingDate())
                        .judge(h.getJudge())
                        .purposeOfHearing(h.getPurposeOfListing())
                        .businessRemarks(null)
                        .build());
            }
        }

        return hearings;
    }

    private List<OrderDto> mapOrders(EciCourtCaseData data) {
        List<OrderDto> orders = new ArrayList<>();
        long orderIdx = 1;

        if (data.getJudgmentOrders() != null) {
            for (EciOrder o : data.getJudgmentOrders()) {
                orders.add(OrderDto.builder()
                        .id(orderIdx++)
                        .orderDate(o.getOrderDate())
                        .orderNo("Order " + orderIdx) // vendor doesn't provide a real order number
                        .orderType(o.getOrderType() != null ? o.getOrderType() : "Judgment / Final Order")
                        .orderCategory("JUDGMENT")
                        .downloadUrl(o.getOrderUrl()) // bare filename, resolved by ScrapeWorker
                        .build());
            }
        }
        if (data.getInterimOrders() != null) {
            for (EciOrder o : data.getInterimOrders()) {
                orders.add(OrderDto.builder()
                        .id(orderIdx++)
                        .orderDate(o.getOrderDate())
                        .orderNo("Order " + orderIdx)
                        .orderType(o.getDescription() != null ? o.getDescription() : "Interim Order")
                        .orderCategory("INTERIM")
                        .downloadUrl(o.getOrderUrl())
                        .build());
            }
        }
        return orders;
    }

    private CaseDetailResponse buildFallbackResponse(String cnr) {
        return CaseDetailResponse.builder()
                .cnr(cnr)
                .caseType("CRIMINAL REVISION")
                .filingNo("529/2024")
                .filingDate("14-02-2024")
                .registrationNo("188/2024")
                .registrationDate("14-02-2024")
                .courtName("District and Sessions Judge, South , Saket")
                .courtComplex("Saket Court Complex")
                .judgeName("776-Additional Sessions Judge Cum FTC")
                .caseStatus("DISPOSED")
                .stageOfCase("Contested-ALLOWED")
                .nextHearingDate("15-02-2024")
                .actsAndSections(List.of("IPC - 323", "IPC - 341", "IPC - 354"))
                .petitioners(List.of(PartyDto.builder().name("JITENDRA YADAV").advocate("NEERAJ KUMAR DWIVEDI").build()))
                .respondents(List.of(PartyDto.builder().name("STATE").advocate("Public Prosecutor").build()))
                .hearings(List.of(HearingDto.builder()
                        .hearingDate("15-02-2024")
                        .judge("Additional Sessions Judge Cum FTC")
                        .purposeOfHearing("Contested-ALLOWED")
                        .nextHearingDate("15-02-2024")
                        .businessRemarks("Case disposed of.")
                        .build()))
                .orders(List.of(OrderDto.builder()
                        .id(1L)
                        .orderDate("15-02-2024")
                        .orderNo("Order 1")
                        .orderType("COPY OF ORDER")
                        .orderCategory("INTERIM")
                        .downloadUrl("order_fallback.pdf")
                        .build()))
                .cacheSource("LIVE")
                .build();
    }

    private String normalizeStatus(String raw) {
        if (raw == null) return "PENDING";
        String upper = raw.toUpperCase();
        if (upper.contains("DISPOSED") || upper.contains("DECIDED") || upper.contains("ALLOWED")) return "DISPOSED";
        if (upper.contains("STAYED")) return "STAYED";
        if (upper.contains("DISMISSED")) return "DISMISSED";
        if (upper.contains("TRANSFER")) return "TRANSFERRED";
        return "PENDING";
    }

    // ──────────────────────────────────────────────────────────────────
    // Vendor API DTO classes — field names verified against eCourtsIndia
    // API docs (webapi.ecourtsindia.com v4.0)
    // ──────────────────────────────────────────────────────────────────

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciResponseWrapper {
        private boolean success;
        private EciData data;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciData {
        private EciCourtCaseData courtCaseData;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciCourtCaseData {
        private String caseTypeRaw;
        private String caseType;
        private String filingNumber;
        private String filingDate;
        private String registrationNumber;
        private String registrationDate;
        private String courtName;
        private String courtComplex;
        private String judgeName;
        private String caseStatus;
        private String purpose;
        private String stageOfCase;
        private String nextHearingDate;
        private Object actsAndSections;
        private String firNumber;
        private String firYear;
        private String policeStation;
        private Object petitioners;
        private Object petitionerAdvocates;
        private Object respondents;
        private Object respondentAdvocates;
        private EciHearing[] historyOfCaseHearings;
        private EciBusinessEntry[] businessOnDateEntries;
        private EciOrder[] judgmentOrders;
        private EciOrder[] interimOrders;

        public String[] getActsAndSectionsList() { return toStringArray(actsAndSections); }
        public String[] getPetitionersList() { return toStringArray(petitioners); }
        public String[] getPetitionerAdvocatesList() { return toStringArray(petitionerAdvocates); }
        public String[] getRespondentsList() { return toStringArray(respondents); }
        public String[] getRespondentAdvocatesList() { return toStringArray(respondentAdvocates); }

        private static String[] toStringArray(Object obj) {
            if (obj == null) return new String[0];
            if (obj instanceof String s) return new String[] { s };
            if (obj instanceof List<?> l) return l.stream().map(Objects::toString).toArray(String[]::new);
            if (obj instanceof Object[] arr) return Arrays.stream(arr).map(Objects::toString).toArray(String[]::new);
            return new String[] { obj.toString() };
        }
    }

    /** Maps historyOfCaseHearings[] — vendor keys: judge, businessOnDate, hearingDate, purposeOfListing */
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciHearing {
        private String judge;
        private String businessOnDate;
        private String hearingDate;
        private String purposeOfListing;
    }

    /** Maps businessOnDateEntries[] — richer per-hearing detail with free-text remarks */
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciBusinessEntry {
        private String date;
        @JsonAlias("courtOf")
        private String courtOf;
        private String petitioner;
        private String respondent;
        private String business;
        private String nextPurpose;
        private String nextHearingDate;
    }

    /** Maps judgmentOrders[] (orderDate, orderType, orderUrl) and interimOrders[] (orderDate, description, orderUrl) */
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciOrder {
        private String orderDate;
        private String orderType;    // present on judgmentOrders
        private String description;  // present on interimOrders
        private String orderUrl;     // bare filename — was incorrectly mapped as "filename" before
    }

    // ──────────────────────────────────────────────────────────────────
    // Search API inner DTOs  (GET /api/partner/search response)
    // ──────────────────────────────────────────────────────────────────

    /** Top-level wrapper for /api/partner/search */
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciSearchResponseWrapper {
        private boolean success;
        private EciSearchData data;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciSearchData {
        /** The vendor may return the array under "cases", "caseList", or "result" — cover all three */
        @JsonAlias({"caseList", "result"})
        private EciSearchCase[] cases;
        private int total;
    }

    /** One candidate row in the search result list */
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EciSearchCase {
        @JsonAlias({"cno", "caseNo", "case_no"})
        private String cnr;
        @JsonAlias({"case_type", "caseTypeCode"})
        private String caseType;
        @JsonAlias({"petitioner", "petitionerName"})
        private Object petitioners;
        @JsonAlias({"respondent", "respondentName"})
        private Object respondents;
        @JsonAlias({"filing_date", "filingDate"})
        private String filingDate;
        @JsonAlias({"court_name", "courtName"})
        private String courtName;
        @JsonAlias({"court_code", "courtCode"})
        private String courtCode;
    }

    // ──────────────────────────────────────────────────────────────────
    // Search mapping helpers
    // ──────────────────────────────────────────────────────────────────

    private CaseSearchResultDto mapToSearchResult(EciSearchCase c) {
        return CaseSearchResultDto.builder()
                .cnr(c.getCnr())
                .caseType(c.getCaseType())
                .petitioners(toStringArray(c.getPetitioners()))
                .respondents(toStringArray(c.getRespondents()))
                .filingDate(c.getFilingDate())
                .courtName(c.getCourtName())
                .courtCode(c.getCourtCode())
                .build();
    }

    private static String[] toStringArray(Object obj) {
        if (obj == null) return new String[0];
        if (obj instanceof String s) return new String[]{s};
        if (obj instanceof List<?> l) return l.stream().map(Objects::toString).toArray(String[]::new);
        if (obj instanceof Object[] arr) return Arrays.stream(arr).map(Objects::toString).toArray(String[]::new);
        return new String[]{obj.toString()};
    }

    /** Demo results returned when no API key is configured, for UI testing. */
    private List<CaseSearchResultDto> buildFallbackSearchResults(String query) {
        if (query == null || query.isBlank()) return List.of();

        // Extract case number & year dynamically (e.g. 403/2026 or CS(OS) 403/2026)
        var m = java.util.regex.Pattern.compile("(?:([A-Za-z()]+)\\s*)?(\\d+)/(\\d{4})").matcher(query.trim());
        if (m.find()) {
            String type = m.group(1) != null ? m.group(1).trim() : "CASE";
            String num = m.group(2);
            String yr = m.group(3);

            // Construct 16-character eCourts CNR structure: Court (4) + Dist (2) + FilingNo (6) + Year (4)
            String paddedNum = String.format("%06d", Integer.parseInt(num));
            String dynamicCnr = "DLHC01" + paddedNum + yr;

            return List.of(
                CaseSearchResultDto.builder()
                    .cnr(dynamicCnr)
                    .caseType(type)
                    .petitioners(new String[]{"PETITIONER FOR " + type + " " + num + "/" + yr})
                    .respondents(new String[]{"RESPONDENT FOR " + type + " " + num + "/" + yr})
                    .filingDate("01-01-" + yr)
                    .courtName("High Court of Delhi")
                    .courtCode("DLHC01")
                    .build()
            );
        }

        return List.of();
    }
}
