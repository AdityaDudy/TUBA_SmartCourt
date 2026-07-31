package in.tubalaw.courtos.modules.tracker.repository;

import in.tubalaw.courtos.modules.tracker.entity.ScrapeJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, Long> {

    /** Pick the oldest PENDING job — ScrapeWorker uses this to process one at a time */
    @Query("SELECT j FROM ScrapeJob j WHERE j.status = 'PENDING' ORDER BY j.createdAt ASC LIMIT 1")
    Optional<ScrapeJob> findNextPending();

    /** Most recent N searches by a user, for the "Recent Searches" chips on the frontend */
    List<ScrapeJob> findTop20ByInitiatedByUserIdAndTenantIdOrderByCreatedAtDesc(Long userId, String tenantId);

    /** Most recent job for a given CNR (used to check if already queued) */
    Optional<ScrapeJob> findTopByCnrAndTenantIdOrderByCreatedAtDesc(String cnr, String tenantId);
}
