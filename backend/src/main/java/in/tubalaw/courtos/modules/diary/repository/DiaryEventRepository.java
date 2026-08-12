package in.tubalaw.courtos.modules.diary.repository;

import in.tubalaw.courtos.modules.diary.entity.DiaryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiaryEventRepository extends JpaRepository<DiaryEvent, Long> {
    @Query("SELECT d FROM DiaryEvent d WHERE d.tenantId = :tenantId AND YEAR(d.eventDate) = :year AND MONTH(d.eventDate) = :month AND (d.ownerId IN :ownerIds OR d.ownerId IS NULL) ORDER BY d.eventDate ASC, d.eventTime ASC")
    List<DiaryEvent> findByMonthAndOwners(String tenantId, int year, int month, List<Long> ownerIds);

    @Query("SELECT d FROM DiaryEvent d WHERE d.tenantId = :tenantId AND (d.ownerId IN :ownerIds OR d.ownerId IS NULL) ORDER BY d.eventDate ASC, d.eventTime ASC")
    List<DiaryEvent> findByOwners(String tenantId, List<Long> ownerIds);

    List<DiaryEvent> findByTenantIdAndEventDate(String tenantId, LocalDate eventDate);

    long countByTenantIdAndEventDateBetween(String tenantId, LocalDate startDate, LocalDate endDate);
}
