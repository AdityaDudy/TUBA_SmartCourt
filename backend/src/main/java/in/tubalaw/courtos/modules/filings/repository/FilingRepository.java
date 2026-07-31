package in.tubalaw.courtos.modules.filings.repository;
import in.tubalaw.courtos.modules.filings.entity.Filing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface FilingRepository extends JpaRepository<Filing, Long> {
    List<Filing> findAllByTenantId(String tenantId);
    List<Filing> findAllByTenantIdAndStatus(String tenantId, String status);
    List<Filing> findAllByTenantIdAndMatterId(String tenantId, Long matterId);
    long countByTenantIdAndStatusNot(String tenantId, String status);

    @Query("SELECT f FROM Filing f WHERE f.tenantId = :tenantId AND LOWER(f.advocate) LIKE LOWER(CONCAT('%', :userName, '%'))")
    List<Filing> findAllByTenantIdAndAdvocateContaining(String tenantId, String userName);

    @Query("SELECT f FROM Filing f WHERE f.tenantId = :tenantId AND f.status = :status AND LOWER(f.advocate) LIKE LOWER(CONCAT('%', :userName, '%'))")
    List<Filing> findAllByTenantIdAndStatusAndAdvocateContaining(String tenantId, String status, String userName);
}
