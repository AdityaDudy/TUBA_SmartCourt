package in.tubalaw.courtos.modules.billing.repository;

import in.tubalaw.courtos.modules.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByInvoiceId(Long invoiceId);
    List<Payment> findAllByTenantId(String tenantId);
}
