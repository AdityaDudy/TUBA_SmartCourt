package in.tubalaw.courtos.modules.causelist.service;

import in.tubalaw.courtos.modules.causelist.entity.Hearing;
import in.tubalaw.courtos.modules.causelist.repository.HearingRepository;
import in.tubalaw.courtos.modules.tracker.repository.TrackedCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HearingService {

    private final HearingRepository      repo;
    private final CauseListSyncService   causeListSyncService;
    private final TrackedCaseRepository  trackedCaseRepo;

    private static final String TENANT = "default";

    public List<Hearing> today() {
        syncAll();
        return repo.findByDate(TENANT, LocalDate.now());
    }

    public List<Hearing> byDate(String date) {
        syncAll();
        return repo.findByDate(TENANT, LocalDate.parse(date));
    }

    public List<Hearing> filter(String type) {
        syncAll();
        if ("Urgent".equalsIgnoreCase(type)) return repo.findUrgent(TENANT);
        return repo.findAllByTenantId(TENANT);
    }

    public long countUrgent() {
        return repo.countByTenantIdAndStatus(TENANT, "Urgent");
    }

    /**
     * Re-derives hearings from all already-stored matter-linked TrackedCases.
     * No new eCourts calls — useful immediately after bulk matter linking
     * so admins don't wait until the next midnight cron run.
     */
    @Transactional
    public int syncAll() {
        var cases = trackedCaseRepo.findAll();
        return causeListSyncService.syncAll(cases);
    }
}

