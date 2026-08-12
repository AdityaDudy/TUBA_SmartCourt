package in.tubalaw.courtos.modules.reports.repository;

import in.tubalaw.courtos.modules.reports.entity.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {
}
