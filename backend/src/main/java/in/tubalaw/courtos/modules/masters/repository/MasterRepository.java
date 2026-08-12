package in.tubalaw.courtos.modules.masters.repository;

import in.tubalaw.courtos.modules.masters.entity.Master;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterRepository extends JpaRepository<Master, Long> {
    List<Master> findAllByTenantId(String tenantId);
    Optional<Master> findByTenantIdAndCategory(String tenantId, String category);
}
