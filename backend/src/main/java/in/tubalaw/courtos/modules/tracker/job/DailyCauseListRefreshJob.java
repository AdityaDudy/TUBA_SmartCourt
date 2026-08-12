package in.tubalaw.courtos.modules.tracker.job;

import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.tracker.entity.ScrapeJob;
import in.tubalaw.courtos.modules.tracker.entity.TrackedCase;
import in.tubalaw.courtos.modules.tracker.repository.ScrapeJobRepository;
import in.tubalaw.courtos.modules.tracker.repository.TrackedCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nightly cause-list refresh job.
 *
 * Runs once daily at 00:15 — a few minutes after midnight so any court-side
 * end-of-day batch updates have settled, and staggered away from exact midnight
 * to avoid clashing with other cron jobs.
 *
 * Queues a ScrapeJob for every TrackedCase that is linked to a Matter.
 * ScrapeWorker processes the queue; CauseListSyncService fires automatically
 * after each job completes, writing fresh hearings rows before the morning.
 *
 * Deliberately separate from CaseRefreshJob (6-hourly alert notifications) —
 * that job's cadence and scope are unchanged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCauseListRefreshJob {

    private final TrackedCaseRepository trackedCaseRepo;
    private final ScrapeJobRepository scrapeJobRepo;
    private final MatterRepository matterRepo;

    @Scheduled(cron = "0 15 0 * * *") // daily at 00:15
    public void refreshAllMatterLinkedCases() {
        // Bug fix: previously only matters that had already been opened at least
        // once in Court Tracker (and therefore already had a TrackedCase row) got
        // refreshed nightly. Matters with a CNR filled in directly on the Matter
        // record but never manually searched were silently skipped forever. Bootstrap
        // a TrackedCase + first ScrapeJob for those now so every matter registered
        // with a CNR gets pulled in.
        bootstrapUntrackedMatterCnrs();

        List<TrackedCase> linked = trackedCaseRepo.findAllByMatterIdIsNotNullAndTenantId("default");

        if (linked.isEmpty()) {
            log.debug("[DailyCauseListRefreshJob] No matter-linked tracked cases yet.");
            return;
        }

        log.info("[DailyCauseListRefreshJob] Queueing daily refresh for {} matter-linked cases", linked.size());

        int queued = 0;
        for (TrackedCase tc : linked) {
            boolean alreadyQueued = scrapeJobRepo
                    .findTopByCnrAndTenantIdOrderByCreatedAtDesc(tc.getCnr(), tc.getTenantId())
                    .filter(j -> "PENDING".equals(j.getStatus()) || "RUNNING".equals(j.getStatus()))
                    .isPresent();

            if (alreadyQueued) {
                log.debug("[DailyCauseListRefreshJob] Skipping CNR {} — job already in queue", tc.getCnr());
                continue;
            }

            scrapeJobRepo.save(ScrapeJob.builder()
                    .cnr(tc.getCnr())
                    .tenantId(tc.getTenantId())
                    .status("PENDING")
                    .forceRefresh(true)
                    .build());
            queued++;
        }

        log.info("[DailyCauseListRefreshJob] Queued {} new refresh jobs ({} already in queue)",
                queued, linked.size() - queued);
    }

    private void bootstrapUntrackedMatterCnrs() {
        List<Matter> untracked = matterRepo.findWithCnrButNotTracked("default");
        if (untracked.isEmpty()) {
            return;
        }

        log.info("[DailyCauseListRefreshJob] Bootstrapping {} matter(s) with a CNR that were never tracked",
                untracked.size());

        for (Matter m : untracked) {
            String cnr = m.getCnrNumber().trim().toUpperCase();

            TrackedCase tc = TrackedCase.builder()
                    .cnr(cnr)
                    .matterId(m.getId())
                    .build();
            tc.setTenantId("default");
            trackedCaseRepo.save(tc);

            scrapeJobRepo.save(ScrapeJob.builder()
                    .cnr(cnr)
                    .tenantId("default")
                    .status("PENDING")
                    .forceRefresh(true)
                    .build());

            log.info("[DailyCauseListRefreshJob] Bootstrapped tracking for Matter {} ('{}') — CNR {}",
                    m.getId(), m.getTitle(), cnr);
        }
    }
}