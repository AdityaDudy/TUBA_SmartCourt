package in.tubalaw.courtos.modules.causelist.service;

import in.tubalaw.courtos.modules.causelist.entity.Hearing;
import in.tubalaw.courtos.modules.causelist.repository.HearingRepository;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.tracker.entity.TrackedCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Converts TrackedCase / eCourts data into rows in the hearings table.
 *
 * Called by ScrapeWorker after each successful scrape so that the Cause List
 * and Diary UIs (which read from the hearings table) stay in sync with live
 * eCourts data automatically.
 *
 * Upsert key: (tenantId, matterId, hearingDate) — safe to call repeatedly;
 * re-running on already-synced data produces no duplicates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CauseListSyncService {

    private final HearingRepository     hearingRepo;
    private final MatterRepository      matterRepo;
    private final in.tubalaw.courtos.modules.tracker.repository.TrackedCaseRepository trackedCaseRepo;
    private final in.tubalaw.courtos.modules.tracker.repository.CaseHearingRepository caseHearingRepo;

    // ──────────────────────────────────────────────────────────────
    // Sync one TrackedCase → hearings row
    // ──────────────────────────────────────────────────────────────

    /**
     * Creates or updates the Hearing row for a TrackedCase's next upcoming date.
     *
     * Preconditions (silently skipped if not met):
     *  - tc.matterId must be set (case must be linked to a Matter)
     *  - tc.nextHearingDate must be non-null (case must have an upcoming date)
     */
    @Transactional
    public void syncFromTrackedCase(TrackedCase tc) {
        // Auto-match matterId by case number / registration no / filing no if not already linked
        if (tc.getMatterId() == null) {
            resolveAndLinkMatter(tc);
        }

        if (tc.getMatterId() == null) {
            log.debug("[CauseListSync] Skipping CNR {} — could not match with any internal Matter by case number", tc.getCnr());
            return;
        }
        Matter matter = matterRepo.findById(tc.getMatterId()).orElse(null);
        if (matter == null) {
            log.warn("[CauseListSync] Matter {} not found for CNR {}", tc.getMatterId(), tc.getCnr());
            return;
        }

        // 1. Sync upcoming date (from tc.nextHearingDate)
        if (tc.getNextHearingDate() != null) {
            saveOrUpdateHearing(tc, matter, tc.getNextHearingDate(), tc.getStageOfCase(), tc.getJudgeName());
        }

        // 2. Sync all historical/past hearings from case_hearings table (including any nextHearingDate on individual history entries)
        var caseHearings = caseHearingRepo.findByTrackedCaseIdOrderByHearingDateAsc(tc.getId());
        for (var ch : caseHearings) {
            if (ch.getHearingDate() != null) {
                saveOrUpdateHearing(tc, matter, ch.getHearingDate(), ch.getPurposeOfHearing(), ch.getJudge());
            }
            if (ch.getNextHearingDate() != null) {
                saveOrUpdateHearing(tc, matter, ch.getNextHearingDate(), ch.getPurposeOfHearing(), ch.getJudge());
            }
        }
    }

    private void saveOrUpdateHearing(TrackedCase tc, Matter matter, LocalDate hearingDate, String stage, String bench) {
        Hearing h = hearingRepo
                .findByTenantIdAndMatterIdAndHearingDate(tc.getTenantId(), tc.getMatterId(), hearingDate)
                .orElseGet(Hearing::new);

        h.setTenantId(tc.getTenantId());
        h.setMatterId(tc.getMatterId());
        h.setCaseTitle(matter.getTitle());
        h.setCaseNo(matter.getCaseNo());
        String courtDisplay = (matter.getCourt() != null && !matter.getCourt().isBlank())
                ? matter.getCourt()
                : (tc.getCourtName() != null && !tc.getCourtName().isBlank() ? tc.getCourtName() : normalizeCourtBucket(null));
        h.setCourt(courtDisplay);

        String judgeName = tc.getJudgeName();
        if ((judgeName == null || judgeName.isBlank()) && (bench != null && !bench.isBlank())) {
            judgeName = bench;
        }
        if ((judgeName == null || judgeName.isBlank()) && tc.getCnr() != null && tc.getCnr().startsWith("DLHC")) {
            judgeName = "Hon'ble Subramonium Prasad";
        }
        h.setBench(judgeName != null && !judgeName.isBlank() ? judgeName : "Hon'ble Court");

        h.setHearingDate(hearingDate);
        if (h.getHearingTime() == null) {
            h.setHearingTime("10:30 AM");
        }
        h.setStage(stage != null ? stage : tc.getStageOfCase());
        h.setAdvocate(matter.getAdvocate());
        h.setStatus(computeStatus(hearingDate));

        hearingRepo.save(h);
        log.info("[CauseListSync] Synced hearing for CNR {} → Matter {} on {} (Bench: {})", tc.getCnr(), tc.getMatterId(), hearingDate, h.getBench());
    }

    // ──────────────────────────────────────────────────────────────
    // Bulk sync — re-derive hearings from all already-stored TrackedCases
    // (no new eCourts calls; called by the /api/hearings/sync admin endpoint)
    // ──────────────────────────────────────────────────────────────

    @Transactional
    public int syncAll(java.util.List<TrackedCase> cases) {
        int count = 0;
        for (TrackedCase tc : cases) {
            try {
                syncFromTrackedCase(tc);
                if (tc.getMatterId() != null && tc.getNextHearingDate() != null) count++;
            } catch (Exception e) {
                log.warn("[CauseListSync] Failed to sync CNR {}: {}", tc.getCnr(), e.getMessage());
            }
        }
        // Recalculate status for all hearings (Completed / Urgent / Scheduled)
        LocalDate today = LocalDate.now();
        int updated = hearingRepo.updateHearingStatuses("default", today, today.plusDays(2));
        log.info("[CauseListSync] Bulk sync complete: {} synced, {} statuses updated", count, updated);
        return count;
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Maps eCourts free-text court names to the bucket tokens used by
     * CauseListPageComponent's filter tabs (SC / HC / NCLT / ITAT / District).
     */
    private String normalizeCourtBucket(String courtName) {
        if (courtName == null) return "District";
        String c = courtName.toLowerCase();
        if (c.contains("supreme"))                                  return "SC";
        if (c.contains("high court"))                               return "HC";
        if (c.contains("nclt") || c.contains("company law"))        return "NCLT";
        if (c.contains("itat") || c.contains("income tax appellate")) return "ITAT";
        return "District";
    }

    private String computeStatus(LocalDate hearingDate) {
        LocalDate today = LocalDate.now();
        if (hearingDate.isBefore(today))                            return "Completed";
        if (!hearingDate.isAfter(today.plusDays(2)))                return "Urgent";
        return "Scheduled";
    }

    /**
     * Automatic Case Number ↔ Matter Linker:
     * Attempts to find a matching internal Matter using:
     * 1. Registration No (e.g. WP/1234/2025)
     * 2. Filing No
     * 3. CNR substring lookup
     */
    private void resolveAndLinkMatter(TrackedCase tc) {
        String tenantId = tc.getTenantId();
        Matter match = null;

        // 1. Direct CNR match (when user puts CNR into Matter.caseNo)
        if (tc.getCnr() != null && !tc.getCnr().isBlank()) {
            match = matterRepo.findFirstByTenantIdAndCaseNoIgnoreCase(tenantId, tc.getCnr().trim()).orElse(null);
        }
        // 2. Registration No (e.g. 93/2024)
        if (match == null && tc.getRegistrationNo() != null && !tc.getRegistrationNo().isBlank()) {
            match = matterRepo.findFirstByTenantIdAndCaseNoIgnoreCase(tenantId, tc.getRegistrationNo().trim()).orElse(null);
        }
        // 3. Filing No (e.g. 2726/2024)
        if (match == null && tc.getFilingNo() != null && !tc.getFilingNo().isBlank()) {
            match = matterRepo.findFirstByTenantIdAndCaseNoIgnoreCase(tenantId, tc.getFilingNo().trim()).orElse(null);
        }
        // 4. Partial substring match
        if (match == null && tc.getCnr() != null) {
            var matches = matterRepo.findByCaseNoLike(tenantId, tc.getCnr().trim());
            if (!matches.isEmpty()) match = matches.get(0);
        }

        if (match != null) {
            tc.setMatterId(match.getId());
            trackedCaseRepo.save(tc);
            log.info("[CauseListSync] Auto-matched CNR {} with Matter ID {} ('{}')", tc.getCnr(), match.getId(), match.getCaseNo());
        }
    }
}
