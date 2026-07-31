package in.tubalaw.courtos.modules.billing.repository;

import in.tubalaw.courtos.modules.billing.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {
    List<InvoiceLineItem> findAllByInvoiceId(Long invoiceId);
}
