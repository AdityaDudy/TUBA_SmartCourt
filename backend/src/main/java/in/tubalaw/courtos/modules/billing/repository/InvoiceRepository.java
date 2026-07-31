package in.tubalaw.courtos.modules.billing.repository;

import in.tubalaw.courtos.modules.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByTenantId(String tenantId);

    List<Invoice> findAllByTenantIdAndStatus(String tenantId, String status);

    long countByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT COALESCE(SUM(i.amount - i.paidAmount), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status != 'Paid'")
    double sumOutstanding(String tenantId);

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = 'Paid'")
    double sumCollected(String tenantId);

    @Query("SELECT COALESCE(SUM(i.amount - i.paidAmount), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = 'Overdue'")
    double sumOverdue(String tenantId);
}
