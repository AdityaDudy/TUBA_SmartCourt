package in.tubalaw.courtos.modules.tracker.repository;

import in.tubalaw.courtos.modules.tracker.entity.TrackedCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrackedCaseRepository extends JpaRepository<TrackedCase, Long> {

    Optional<TrackedCase> findByCnrAndTenantId(String cnr, String tenantId);

    /** All cases with active alert subscriptions that haven't been fetched recently */
    @Query("""
        SELECT DISTINCT tc FROM TrackedCase tc
        JOIN CaseAlertSubscription cas ON cas.trackedCaseId = tc.id
        WHERE cas.active = true
          AND tc.tenantId = :tenantId
          AND (tc.lastFetchedAt IS NULL OR tc.lastFetchedAt < :cutoff)
    """)
    List<TrackedCase> findStaleWithActiveAlerts(String tenantId, Instant cutoff);

    /** All matter-linked cases — used by DailyCauseListRefreshJob */
    List<TrackedCase> findAllByMatterIdIsNotNullAndTenantId(String tenantId);
}

