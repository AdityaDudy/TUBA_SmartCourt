package in.tubalaw.courtos.modules.billing.repository;

import in.tubalaw.courtos.modules.billing.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByTenantId(String tenantId);
    List<Expense> findAllByMatterId(Long matterId);
    List<Expense> findAllByClientId(Long clientId);
    List<Expense> findAllByTenantIdAndBillableAndInvoiced(String tenantId, boolean billable, boolean invoiced);
    List<Expense> findAllByInvoiceId(Long invoiceId);
}
