package in.tubalaw.courtos.modules.tracker.repository;

import in.tubalaw.courtos.modules.tracker.entity.CaseAlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseAlertSubscriptionRepository extends JpaRepository<CaseAlertSubscription, Long> {

    Optional<CaseAlertSubscription> findByTrackedCaseIdAndUserId(Long trackedCaseId, Long userId);

    List<CaseAlertSubscription> findByTrackedCaseIdAndActiveTrue(Long trackedCaseId);
}
