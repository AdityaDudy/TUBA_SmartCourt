package in.tubalaw.courtos.modules.clients.repository;

import in.tubalaw.courtos.modules.clients.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByTenantId(String tenantId);

    @Query("SELECT c FROM Client c WHERE c.tenantId = :tenantId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(c.mobile) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Client> search(String tenantId, String q);

    List<Client> findAllByTenantIdAndType(String tenantId, String type);
}
