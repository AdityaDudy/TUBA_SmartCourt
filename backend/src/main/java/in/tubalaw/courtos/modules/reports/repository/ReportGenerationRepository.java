package in.tubalaw.courtos.modules.reports.repository;

import in.tubalaw.courtos.modules.reports.entity.ReportGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportGenerationRepository extends JpaRepository<ReportGeneration, Long> {
}
