package in.tubalaw.courtos.modules.tracker.job;

import in.tubalaw.courtos.modules.tracker.entity.ScrapeJob;
import in.tubalaw.courtos.modules.tracker.entity.TrackedCase;
import in.tubalaw.courtos.modules.tracker.repository.ScrapeJobRepository;
import in.tubalaw.courtos.modules.tracker.repository.TrackedCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled alert refresh job.
 *
 * Every 6 hours: finds all TrackedCase records that have ≥1 active CaseAlertSubscription
 * and haven't been fetched in the last 6 hours, then enqueues a new ScrapeJob for each.
 *
 * ScrapeWorker processes the jobs and calls TrackerService.diffAndNotify() on completion,
 * which fires notifications through NotificationService for any new hearings or orders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaseRefreshJob {

    private final TrackedCaseRepository trackedCaseRepo;
    private final ScrapeJobRepository   scrapeJobRepo;

    @Value("${tracker.cache.ttl-hours:6}")
    private int cacheTtlHours;

    @Scheduled(cron = "0 0 */6 * * *") // every 6 hours
    public void refreshStaleAlertedCases() {
        Instant cutoff = Instant.now().minus(cacheTtlHours, ChronoUnit.HOURS);

        // Find stale cases that still have active subscribers (multi-tenant: iterate all tenants)
        // For simplicity, we pass "default" here; in a multi-tenant scenario you'd enumerate tenants
        List<TrackedCase> staleCases = trackedCaseRepo.findStaleWithActiveAlerts("default", cutoff);

        if (staleCases.isEmpty()) {
            log.debug("[CaseRefreshJob] No stale alerted cases found.");
            return;
        }

        log.info("[CaseRefreshJob] Queueing refresh for {} alerted cases", staleCases.size());

        for (TrackedCase tc : staleCases) {
            // Only queue if no PENDING/RUNNING job already exists for this CNR
            scrapeJobRepo.findTopByCnrAndTenantIdOrderByCreatedAtDesc(tc.getCnr(), tc.getTenantId())
                .filter(j -> "PENDING".equals(j.getStatus()) || "RUNNING".equals(j.getStatus()))
                .ifPresentOrElse(
                    existing -> log.debug("[CaseRefreshJob] Skipping CNR {} — job {} already in queue", tc.getCnr(), existing.getId()),
                    () -> {
                        ScrapeJob job = ScrapeJob.builder()
                            .cnr(tc.getCnr())
                            .tenantId(tc.getTenantId())
                            .status("PENDING")
                            .forceRefresh(true)
                            .build();
                        scrapeJobRepo.save(job);
                        log.info("[CaseRefreshJob] Queued refresh job for CNR: {}", tc.getCnr());
                    }
                );
        }
    }
}
