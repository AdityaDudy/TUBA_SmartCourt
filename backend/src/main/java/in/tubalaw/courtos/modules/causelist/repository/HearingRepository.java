package in.tubalaw.courtos.modules.causelist.repository;

import in.tubalaw.courtos.modules.causelist.entity.Hearing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HearingRepository extends JpaRepository<Hearing, Long> {

    @Query("SELECT h FROM Hearing h WHERE h.tenantId = :tenantId AND h.hearingDate = :date ORDER BY h.hearingTime ASC")
    List<Hearing> findByDate(String tenantId, LocalDate date);

    @Query("SELECT h FROM Hearing h WHERE h.tenantId = :tenantId ORDER BY h.hearingDate ASC, h.hearingTime ASC")
    List<Hearing> findAllByTenantId(String tenantId);

    @Query("SELECT h FROM Hearing h WHERE h.tenantId = :tenantId AND h.status = 'Urgent' ORDER BY h.hearingDate ASC")
    List<Hearing> findUrgent(String tenantId);

    /** Upsert key — used by CauseListSyncService to prevent duplicate rows on repeated syncs */
    Optional<Hearing> findByTenantIdAndMatterIdAndHearingDate(String tenantId, Long matterId, LocalDate hearingDate);

    /** Dynamically recalculates hearing status (Completed for past dates, Urgent for next 48h, Scheduled for future) */
    @Modifying
    @Query("""
        UPDATE Hearing h SET h.status = 
        CASE 
            WHEN h.hearingDate < :today THEN 'Completed'
            WHEN h.hearingDate <= :urgentCutoff THEN 'Urgent'
            ELSE 'Scheduled'
        END
        WHERE h.tenantId = :tenantId
    """)
    int updateHearingStatuses(String tenantId, LocalDate today, LocalDate urgentCutoff);

    long countByTenantIdAndHearingDate(String tenantId, LocalDate date);

    long countByTenantIdAndHearingDateBetween(String tenantId, LocalDate startDate, LocalDate endDate);

    long countByTenantIdAndStatus(String tenantId, String status);

    /** Used by BillingService.getPendingBillables() \u2014 replaces the unsafe findAll() */
    List<Hearing> findAllByTenantIdAndStatus(String tenantId, String status);
}
