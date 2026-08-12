package in.tubalaw.courtos.modules.tracker.repository;

import in.tubalaw.courtos.modules.tracker.entity.CaseHearing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CaseHearingRepository extends JpaRepository<CaseHearing, Long> {

    List<CaseHearing> findByTrackedCaseIdOrderByHearingDateAsc(Long trackedCaseId);

    long countByTrackedCaseId(Long trackedCaseId);

    boolean existsByTrackedCaseIdAndHearingDate(Long trackedCaseId, LocalDate hearingDate);
}
