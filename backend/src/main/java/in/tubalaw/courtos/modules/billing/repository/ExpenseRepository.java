package in.tubalaw.courtos.modules.billing.repository;

import in.tubalaw.courtos.modules.billing.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>,
        JpaSpecificationExecutor<Expense> {
    List<Expense> findAllByTenantId(String tenantId);
    List<Expense> findAllByMatterId(Long matterId);
    List<Expense> findAllByClientId(Long clientId);
    List<Expense> findAllByTenantIdAndBillableAndInvoiced(String tenantId, boolean billable, boolean invoiced);
    List<Expense> findAllByInvoiceId(Long invoiceId);
}
