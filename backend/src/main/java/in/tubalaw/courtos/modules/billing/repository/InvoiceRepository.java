package in.tubalaw.courtos.modules.billing.repository;

import in.tubalaw.courtos.modules.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>,
        JpaSpecificationExecutor<Invoice> {

    List<Invoice> findAllByTenantId(String tenantId);

    List<Invoice> findAllByTenantIdAndStatus(String tenantId, String status);

    /** Used by getMatterRollup() to avoid streaming the entire tenant table */
    List<Invoice> findAllByTenantIdAndMatterId(String tenantId, Long matterId);

    /** Used by getClientRollup() to avoid streaming the entire tenant table */
    List<Invoice> findAllByTenantIdAndClientId(String tenantId, Long clientId);

    long countByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT COALESCE(SUM(i.amount - COALESCE(i.paidAmount, 0)), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status != 'Paid' AND i.status != 'Void'")
    double sumOutstanding(String tenantId);

    @Query("SELECT COALESCE(SUM(COALESCE(i.paidAmount, 0)), 0) FROM Invoice i WHERE i.tenantId = :tenantId")
    double sumCollected(String tenantId);

    @Query("SELECT COALESCE(SUM(i.amount - COALESCE(i.paidAmount, 0)), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = 'Overdue'")
    double sumOverdue(String tenantId);
}

