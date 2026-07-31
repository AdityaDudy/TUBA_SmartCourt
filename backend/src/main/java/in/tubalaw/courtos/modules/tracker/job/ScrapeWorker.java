package in.tubalaw.courtos.modules.tracker.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.tubalaw.courtos.modules.causelist.service.CauseListSyncService;
import in.tubalaw.courtos.modules.tracker.dto.CaseDetailResponse;
import in.tubalaw.courtos.modules.tracker.dto.OrderDto;
import in.tubalaw.courtos.modules.tracker.entity.CaseOrder;
import in.tubalaw.courtos.modules.tracker.entity.ScrapeJob;
import in.tubalaw.courtos.modules.tracker.entity.TrackedCase;
import in.tubalaw.courtos.modules.tracker.repository.CaseOrderRepository;
import in.tubalaw.courtos.modules.tracker.repository.ScrapeJobRepository;
import in.tubalaw.courtos.modules.tracker.repository.TrackedCaseRepository;
import in.tubalaw.courtos.modules.tracker.service.TrackerService;
import in.tubalaw.courtos.modules.tracker.service.provider.CourtDataProvider;
import in.tubalaw.courtos.modules.tracker.service.provider.ProviderException;
import in.tubalaw.courtos.modules.tracker.storage.OrderFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;

/**
 * DB-backed async scrape worker.
 *
 * Polls the scrape_jobs table every 5s for PENDING jobs.
 * Processes one job at a time to avoid parallel eCourts requests (rate
 * limiting).
 *
 * Flow:
 * PENDING → RUNNING → CourtDataProvider.fetchByCnr()
 * → DONE : persist TrackedCase, diff hearings, download order PDFs to S3
 * → CAPTCHA_REQUIRED : save captcha S3 URL, wait for user input (solveCaptcha
 * endpoint)
 * → FAILED : save error message, log
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapeWorker {

    private final ScrapeJobRepository scrapeJobRepo;
    private final TrackedCaseRepository trackedCaseRepo;
    private final CaseOrderRepository orderRepo;
    private final TrackerService trackerService;
    private final CourtDataProvider courtDataProvider;
    private final ObjectMapper objectMapper;
    private final OrderFileStorage orderFileStorage;
    private final CauseListSyncService causeListSyncService;

    @Scheduled(fixedDelayString = "${tracker.scrape.worker-poll-ms:1000}")
    public void processNextJob() {
        Optional<ScrapeJob> opt = scrapeJobRepo.findNextPending();
        if (opt.isEmpty())
            return;

        ScrapeJob job = opt.get();
        log.info("[ScrapeWorker] Processing job {} for CNR: {}", job.getId(), job.getCnr());

        // Mark RUNNING
        job.setStatus("RUNNING");
        job.setStartedAt(Instant.now());
        scrapeJobRepo.save(job);

        try {
            if (!courtDataProvider.isAvailable()) {
                failJob(job, "eCourts service is currently unavailable. Will retry.");
                return;
            }

            CaseDetailResponse result = courtDataProvider.fetchByCnr(job.getCnr());

            // Persist case data + diff hearings
            int[] counts = trackerService.persistScrapedResult(job.getCnr(), job.getTenantId(), result);

            // Bug fix: this sync was previously only reachable via a manual
            // /api/hearings/sync admin call — the nightly DailyCauseListRefreshJob
            // and the 6-hourly CaseRefreshJob queued scrapes, but nothing ever
            // pushed the refreshed data into the `hearings` table the Cause List
            // and Diary pages read from, so cause-list entries silently went stale.
            // Sync automatically after every successful scrape instead.
            trackedCaseRepo.findByCnrAndTenantId(job.getCnr(), job.getTenantId()).ifPresent(tc -> {
                try {
                    causeListSyncService.syncFromTrackedCase(tc);
                } catch (Exception e) {
                    log.warn("[ScrapeWorker] Cause-list sync failed for CNR {}: {}", job.getCnr(), e.getMessage());
                }
            });

            // Note: Automatic bulk download of order PDFs disabled to save API credits
            // (5.25 per order PDF).
            // Order PDFs are now fetched on-demand only when user clicks "Download PDF" or
            // "Save to Matter Vault".
            int newOrders = 0;

            // Store full snapshot JSON
            result.setCacheSource("LIVE");
            job.setResultJson(objectMapper.writeValueAsString(result));
            job.setStatus("DONE");
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(null);
            scrapeJobRepo.save(job);

            // Trigger alert notifications
            trackerService.diffAndNotify(job.getCnr(), job.getTenantId(), counts[0], newOrders);

            log.info("[ScrapeWorker] Job {} done: {} new hearings, {} new orders", job.getId(), counts[0], newOrders);

        } catch (ProviderException pe) {
            log.error("[ScrapeWorker] Provider error for job {} (CNR: {}): {}", job.getId(), job.getCnr(),
                    pe.getMessage());
            failJob(job, pe.getMessage());
        } catch (Exception e) {
            log.error("[ScrapeWorker] Unexpected error processing job {}: {}", job.getId(), e.getMessage(), e);
            failJob(job, "Internal worker error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Order PDF download + S3 upload (Disabled by default to save API credits;
    // available for on-demand fetch)
    // ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    private int downloadOrderDocuments(ScrapeJob job, CaseDetailResponse result) {
        if (result.getOrders() == null || result.getOrders().isEmpty())
            return 0;

        // We need the TrackedCase ID to store orders
        Optional<TrackedCase> tcOpt = trackedCaseRepo.findByCnrAndTenantId(job.getCnr(), job.getTenantId());
        if (tcOpt.isEmpty())
            return 0; // will be created by persistScrapedResult momentarily
        TrackedCase tc = tcOpt.get();

        int newCount = 0;

        for (OrderDto order : result.getOrders()) {
            if (order.getDownloadUrl() == null)
                continue;
            try {
                // Download PDF bytes via eCourtsIndia API order-md endpoint or direct URL
                byte[] bytes = fetchOrderPdfBytes(job.getCnr(), order.getDownloadUrl());
                if (bytes == null || bytes.length == 0)
                    continue;

                // SHA-256 dedup
                String hash = sha256hex(bytes);
                if (orderRepo.findByTrackedCaseIdAndContentHash(tc.getId(), hash).isPresent()) {
                    log.debug("[ScrapeWorker] Order already stored (hash match) for job {}", job.getId());
                    continue;
                }

                // Save to local OrderFileStorage
                String filename = (order.getOrderDate() != null ? order.getOrderDate() : "order") + "_"
                        + hash.substring(0, 8) + ".pdf";
                String storageKey = orderFileStorage.store(job.getTenantId(), job.getCnr(), filename, bytes);

                CaseOrder caseOrder = CaseOrder.builder()
                        .trackedCaseId(tc.getId())
                        .tenantId(job.getTenantId())
                        .orderDate(tryParseDate(order.getOrderDate()))
                        .orderNo(order.getOrderNo())
                        .orderType(order.getOrderType())
                        .orderCategory(order.getOrderCategory())
                        .s3Key(storageKey)
                        .s3Url(null)
                        .fileSize((long) bytes.length)
                        .mimeType("application/pdf")
                        .contentHash(hash)
                        .externalUrl(order.getDownloadUrl())
                        .downloadedAt(Instant.now())
                        .build();
                orderRepo.save(caseOrder);

                // Patch the DTO's downloadUrl to point to our proxied endpoint
                order.setDownloadUrl("/api/tracker/" + job.getCnr() + "/orders/" + caseOrder.getId() + "/download");
                newCount++;

            } catch (Exception e) {
                log.warn("[ScrapeWorker] Failed to download order for job {}: {}", job.getId(), e.getMessage());
            }
        }

        return newCount;
    }

    // ──────────────────────────────────────────────────────────────
    // eCourtsIndia API Order PDF fetcher (order-md endpoint or direct URL)
    // ──────────────────────────────────────────────────────────────

    @Value("${tracker.provider.api-key:}")
    private String apiKey;

    @Value("${tracker.provider.base-url:https://webapi.ecourtsindia.com}")
    private String apiBaseUrl;

    private byte[] fetchOrderPdfBytes(String cnr, String filenameOrUrl) {
        if (filenameOrUrl == null || filenameOrUrl.isBlank())
            return null;

        // If it's a full URL (legacy), fetch directly
        if (filenameOrUrl.startsWith("http")) {
            return fetchBytes(filenameOrUrl);
        }

        // eCourtsIndia API order-md endpoint: GET
        // /api/partner/case/{cnr}/order-md/{filename}
        if (apiKey == null || apiKey.isBlank()) {
            // Return dummy PDF byte structure for local dev without live API key
            return ("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000052 00000 n\n0000000101 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/api/partner/case/" + cnr + "/order-md/" + filenameOrUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200 && resp.body() != null) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(resp.body());
                String base64 = root.path("data").path("pdfBase64").asText(null);
                if (base64 != null && !base64.isBlank()) {
                    return java.util.Base64.getDecoder().decode(base64);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[ScrapeWorker] Failed to fetch order PDF for filename {}: {}", filenameOrUrl, e.getMessage());
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private void failJob(ScrapeJob job, String message) {
        job.setStatus("FAILED");
        job.setErrorMessage(message);
        job.setCompletedAt(Instant.now());
        scrapeJobRepo.save(job);
        log.warn("[ScrapeWorker] Job {} failed: {}", job.getId(), message);
    }

    private byte[] fetchBytes(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET().build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            return resp.statusCode() == 200 ? resp.body() : null;
        } catch (Exception e) {
            log.warn("[ScrapeWorker] fetchBytes failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String sha256hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(data));
    }

    private static final java.time.format.DateTimeFormatter[] DATE_FORMATTERS = {
            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH),
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
    };

    private LocalDate tryParseDate(String s) {
        if (s == null || s.isBlank())
            return null;
        String cleaned = s.trim();
        for (java.time.format.DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}