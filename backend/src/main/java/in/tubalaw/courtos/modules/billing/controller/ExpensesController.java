package in.tubalaw.courtos.modules.billing.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.auth.repository.UserRepository;
import in.tubalaw.courtos.modules.billing.entity.Expense;
import in.tubalaw.courtos.modules.billing.repository.ExpenseRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing/expenses")
@RequiredArgsConstructor
public class ExpensesController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    @GetMapping
    @PreAuthorize("hasAnyAuthority('view_billing', 'view_own_billing', 'create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Expense>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(expenseRepository.findAllByTenantId(TENANT)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Expense>> create(@RequestBody Expense expense) {
        expense.setTenantId(TENANT);
        populateCreator(expense);
        Expense saved = expenseRepository.save(expense);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Expense Created", "Expense", saved.getId().toString(),
                "Created expense: " + saved.getCategory() + " ₹" + saved.getAmount(), request.getRemoteAddr(), "LOW");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Expense logged successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Expense>> update(@PathVariable Long id, @RequestBody Expense updates) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
        if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
        if (updates.getAmount() != null) existing.setAmount(updates.getAmount());
        if (updates.getDate() != null) existing.setDate(updates.getDate());
        existing.setBillable(updates.isBillable());
        existing.setInvoiced(updates.isInvoiced());
        if (updates.getInvoiceId() != null) existing.setInvoiceId(updates.getInvoiceId());
        if (updates.getMatterId() != null) existing.setMatterId(updates.getMatterId());
        if (updates.getMatterTitle() != null) existing.setMatterTitle(updates.getMatterTitle());
        if (updates.getClientId() != null) existing.setClientId(updates.getClientId());
        if (updates.getClientName() != null) existing.setClientName(updates.getClientName());
        if (updates.getReceiptPath() != null) existing.setReceiptPath(updates.getReceiptPath());
        Expense saved = expenseRepository.save(existing);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Expense Updated", "Expense", id.toString(),
                "Updated expense #" + id + " (" + saved.getCategory() + ")", request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(saved, "Expense updated"));
    }

    private void populateCreator(Expense expense) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            if (email != null && !email.isBlank()) {
                userRepository.findByEmailAndTenantId(email, TENANT).ifPresent(user -> {
                    expense.setCreatedById(user.getId());
                    expense.setCreatedByName(user.getName());
                });
            }
        } catch (Exception ignored) {
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('create_invoices', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
        expenseRepository.deleteById(id);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Expense Deleted", "Expense", id.toString(),
                "Deleted expense: " + expense.getCategory() + " ₹" + expense.getAmount(), request.getRemoteAddr(), "MEDIUM");
        return ResponseEntity.ok(ApiResponse.ok(null, "Expense deleted"));
    }
}
