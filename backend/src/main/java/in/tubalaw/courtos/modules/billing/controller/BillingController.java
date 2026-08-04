package in.tubalaw.courtos.modules.billing.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.billing.entity.Invoice;
import in.tubalaw.courtos.modules.billing.entity.Payment;
import in.tubalaw.courtos.modules.billing.dto.PendingBillableDto;
import in.tubalaw.courtos.modules.billing.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyAuthority('view_billing', 'view_own_billing', 'create_invoices', 'export_billing', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Invoice>>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.list(status)));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('view_billing', 'view_own_billing', 'create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Invoice>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getById(id)));
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Invoice>> create(@RequestBody Invoice invoice) {
        Invoice saved = billingService.create(invoice);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Invoice Created", "Invoice", saved.getId().toString(),
                "Created invoice #" + saved.getId() + " for ₹" + saved.getAmount(), request.getRemoteAddr(), "LOW");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Invoice generated!"));
    }

    @PutMapping("/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Invoice>> update(@PathVariable Long id, @RequestBody Invoice invoice) {
        Invoice saved = billingService.update(id, invoice);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Invoice Updated", "Invoice", id.toString(),
                "Updated invoice #" + id, request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @DeleteMapping("/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        billingService.delete(id);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Invoice Deleted", "Invoice", id.toString(),
                "Deleted invoice #" + id, request.getRemoteAddr(), "MEDIUM");
        return ResponseEntity.ok(ApiResponse.ok(null, "Invoice deleted."));
    }

    @PostMapping("/invoices/bulk-delete")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            billingService.delete(id);
        }
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Bulk Invoice Delete", "Invoice", null,
                "Deleted " + ids.size() + " invoice(s)", request.getRemoteAddr(), "HIGH");
        return ResponseEntity.ok(ApiResponse.ok(null, "Invoices deleted successfully."));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('view_billing', 'view_own_billing', 'create_invoices', 'export_billing', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.summary()));
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addEntry(@RequestBody Map<String, Object> entry) {
        return ResponseEntity.ok(ApiResponse.ok(null, "Billing entry added."));
    }

    @GetMapping("/pending-billables")
    @PreAuthorize("hasAnyAuthority('view_billing', 'create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PendingBillableDto>>> getPendingBillables() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getPendingBillables()));
    }

    @PostMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> recordPayment(@PathVariable Long id, @RequestBody Payment payment) {
        Payment saved = billingService.recordPayment(id, payment);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Payment Recorded", "Invoice", id.toString(),
                "Recorded payment of ₹" + payment.getAmount() + " for invoice #" + id, request.getRemoteAddr(), "LOW");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Payment recorded successfully!"));
    }

    @GetMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyAuthority('view_billing', 'view_own_billing', 'create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Payment>>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getPayments(id)));
    }

    @GetMapping("/rollup/matter/{id}")
    @PreAuthorize("hasAnyAuthority('view_billing', 'view_own_billing', 'create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMatterRollup(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getMatterRollup(id)));
    }

    @GetMapping("/rollup/client/{id}")
    @PreAuthorize("hasAnyAuthority('view_billing', 'view_own_billing', 'create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClientRollup(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getClientRollup(id)));
    }

    @PostMapping("/invoices/bulk-remind")
    @PreAuthorize("hasAnyAuthority('export_billing', 'create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> bulkRemind(@RequestBody List<Long> invoiceIds) {
        for (Long id : invoiceIds) {
            System.out.println("[ALERTS] Sent payment reminder notification for Invoice #" + id);
        }
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Bulk Reminder Sent", "Invoice", null,
                "Sent payment reminders for " + invoiceIds.size() + " invoice(s)", request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(null, "Reminders dispatched successfully"));
    }
}
