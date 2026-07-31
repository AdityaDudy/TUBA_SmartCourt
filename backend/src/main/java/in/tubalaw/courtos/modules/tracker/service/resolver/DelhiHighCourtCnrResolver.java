package in.tubalaw.courtos.modules.tracker.service.resolver;

import in.tubalaw.courtos.modules.tracker.service.provider.ProviderException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Direct scraper fallback for Delhi High Court (DLHC / DLHC01).
 * Queries official Delhi High Court status portal when eCourts search index
 * lags.
 */
@Slf4j
@Component
public class DelhiHighCourtCnrResolver implements CourtCnrResolver {

    private static final String DLHC_URL = "https://delhihighcourt.nic.in/app/get-case-type-status";
    private static final String DLHC_CAUSE_HISTORY_URL = "https://delhihighcourt.nic.in/app/online-cause-history";
    private static final Pattern CNR_PATTERN = Pattern.compile("[A-Z]{4}\\d{10,14}");

    @Override
    public boolean supports(String courtCode) {
        return courtCode != null && courtCode.trim().toUpperCase().startsWith("DLHC");
    }

    @Override
    public Optional<String> resolveCnr(String caseType, String number, String year) throws ProviderException {
        log.info("[DelhiHighCourtCnrResolver] Attempting dynamic portal lookup for caseType='{}' number='{}' year='{}'", caseType, number, year);

        String cleanType = caseType != null ? caseType.trim() : "CS(OS)";
        String cleanNum = number != null ? number.trim() : "";
        String cleanYear = year != null ? year.trim() : "";

        // 1. Try Base64-encoded JSON URL path (Delhi High Court's online-cause-history endpoint)
        Optional<String> cnrFromBase64 = tryBase64UrlRequest(cleanType, cleanNum, cleanYear);
        if (cnrFromBase64.isPresent()) {
            return cnrFromBase64;
        }

        // 2. Try POST form requests with different param variations
        String formattedCaseNo = cleanType + "-" + cleanNum + "/" + cleanYear;
        String[] targetUrls = { DLHC_URL, DLHC_CAUSE_HISTORY_URL };

        for (String url : targetUrls) {
            Optional<String> cnr = tryPortalRequest(url, cleanType, cleanNum, cleanYear, formattedCaseNo);
            if (cnr.isPresent())
                return cnr;
        }

        log.warn("[DelhiHighCourtCnrResolver] Dynamic portal lookup yielded 0 matches for {} {}/{}", caseType, number, year);
        return Optional.empty();
    }

    private Optional<String> tryBase64UrlRequest(String caseType, String number, String year) {
        try {
            // Build Delhi High Court JSON payload: {"case_type":"CS(OS)","case_no":"403","case_year":"2026"}
            String jsonPayload = String.format("{\"case_type\":\"%s\",\"case_no\":\"%s\",\"case_year\":\"%s\"}", caseType, number, year);
            String base64Path = java.util.Base64.getEncoder().encodeToString(jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String fullUrl = DLHC_CAUSE_HISTORY_URL + "/" + base64Path;

            log.info("[DelhiHighCourtCnrResolver] Requesting Base64 URL: {}", fullUrl);

            Document doc = Jsoup.connect(fullUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(12_000)
                    .ignoreHttpErrors(true)
                    .get();

            String html = doc.html();
            Matcher matcher = CNR_PATTERN.matcher(html);
            if (matcher.find()) {
                String cnr = matcher.group();
                log.info("[DelhiHighCourtCnrResolver] Successfully resolved CNR from Base64 URL: {}", cnr);
                return Optional.of(cnr);
            }
        } catch (Exception e) {
            log.warn("[DelhiHighCourtCnrResolver] Base64 URL lookup failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> tryPortalRequest(String url, String caseType, String number, String year, String formattedCaseNo) {
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(12_000)
                    .ignoreHttpErrors(true);

            if (caseType != null && !caseType.isBlank()) {
                conn.data("case_type", caseType)
                        .data("casetype", caseType)
                        .data("case_no", number)
                        .data("caseno", number)
                        .data("case_year", year)
                        .data("caseyear", year);
            }
            conn.data("query", formattedCaseNo)
                    .data("search", formattedCaseNo);

            Document doc = conn.post();
            String html = doc.html();

            Matcher matcher = CNR_PATTERN.matcher(html);
            if (matcher.find()) {
                String cnr = matcher.group();
                log.info("[DelhiHighCourtCnrResolver] Successfully resolved CNR: {} from {}", cnr, url);
                return Optional.of(cnr);
            }
        } catch (Exception e) {
            log.debug("[DelhiHighCourtCnrResolver] Portal request to {} failed: {}", url, e.getMessage());
        }
        return Optional.empty();
    }
}
