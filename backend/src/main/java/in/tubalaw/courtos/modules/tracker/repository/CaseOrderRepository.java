package in.tubalaw.courtos.modules.tracker.repository;

import in.tubalaw.courtos.modules.tracker.entity.CaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseOrderRepository extends JpaRepository<CaseOrder, Long> {

    List<CaseOrder> findByTrackedCaseIdOrderByOrderDateDesc(Long trackedCaseId);

    long countByTrackedCaseId(Long trackedCaseId);

    /** Used for SHA-256 dedup — if the hash already exists for this case, skip re-download */
    Optional<CaseOrder> findByTrackedCaseIdAndContentHash(Long trackedCaseId, String contentHash);

    boolean existsByTrackedCaseIdAndExternalUrl(Long trackedCaseId, String externalUrl);
}
