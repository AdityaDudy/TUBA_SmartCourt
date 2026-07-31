package in.tubalaw.courtos.modules.billing.service;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.modules.billing.dto.PendingBillableDto;
import in.tubalaw.courtos.modules.billing.entity.Invoice;
import in.tubalaw.courtos.modules.billing.entity.InvoiceLineItem;
import in.tubalaw.courtos.modules.billing.entity.Payment;
import in.tubalaw.courtos.modules.billing.entity.Expense;
import in.tubalaw.courtos.modules.billing.repository.InvoiceRepository;
import in.tubalaw.courtos.modules.billing.repository.InvoiceLineItemRepository;
import in.tubalaw.courtos.modules.billing.repository.PaymentRepository;
import in.tubalaw.courtos.modules.billing.repository.ExpenseRepository;
import in.tubalaw.courtos.modules.filings.entity.Filing;
import in.tubalaw.courtos.modules.filings.repository.FilingRepository;
import in.tubalaw.courtos.modules.causelist.entity.Hearing;
import in.tubalaw.courtos.modules.causelist.repository.HearingRepository;
import in.tubalaw.courtos.modules.tasks.entity.Task;
import in.tubalaw.courtos.modules.tasks.repository.TaskRepository;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final InvoiceRepository repo;
    private final InvoiceLineItemRepository lineItemRepo;
    private final PaymentRepository paymentRepo;
    private final ExpenseRepository expenseRepo;
    private final FilingRepository filingRepo;
    private final HearingRepository hearingRepo;
    private final TaskRepository taskRepo;
    private final MatterRepository matterRepo;

    private static final String TENANT = "default";

    public List<Invoice> list(String status) {
        // Run overdue status checker check first to ensure status list is fresh
        checkOverdueInvoices();
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All")) {
            return repo.findAllByTenantIdAndStatus(TENANT, status);
        }
        return repo.findAllByTenantId(TENANT);
    }

    public Invoice getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    @Transactional
    public Invoice create(Invoice invoice) {
        invoice.setTenantId(TENANT);
        long count = repo.count();
        invoice.setInvoiceNo("INV-2026-" + String.format("%03d", count + 1));
        
        // Compute total amount from line items if present
        BigDecimal total = BigDecimal.ZERO;
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            for (InvoiceLineItem item : invoice.getLineItems()) {
                item.setTenantId(TENANT);
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
            }
        }
        invoice.setAmount(total);
        
        if (invoice.getStatus() == null || invoice.getStatus().isBlank()) {
            invoice.setStatus("Unpaid");
        }

        Invoice saved = repo.save(invoice);

        // Update source references to 'invoiced = true'
        if (saved.getLineItems() != null) {
            for (InvoiceLineItem item : saved.getLineItems()) {
                String ref = item.getSourceReference();
                if (ref != null && ref.startsWith("expense_")) {
                    try {
                        Long expId = Long.parseLong(ref.replace("expense_", ""));
                        expenseRepo.findById(expId).ifPresent(exp -> {
                            exp.setInvoiced(true);
                            exp.setInvoiceId(saved.getId());
                            expenseRepo.save(exp);
                        });
                    } catch (Exception e) {
                        log.error("Failed to parse expense reference id: {}", ref, e);
                    }
                }
            }
        }

        return saved;
    }

    @Transactional
    public Invoice update(Long id, Invoice updates) {
        Invoice existing = getById(id);
        if (updates.getStatus()      != null) existing.setStatus(updates.getStatus());
        if (updates.getAmount()      != null) existing.setAmount(updates.getAmount());
        if (updates.getPaidAmount()  != null) existing.setPaidAmount(updates.getPaidAmount());
        if (updates.getDueDate()     != null) existing.setDueDate(updates.getDueDate());
        if (updates.getPaidDate()    != null) existing.setPaidDate(updates.getPaidDate());
        
        // Check status recalculations
        rederiveStatus(existing);
        
        return repo.save(existing);
    }

    public Map<String, Object> summary() {
        checkOverdueInvoices();
        Map<String, Object> s = new HashMap<>();
        
        double outstanding = repo.sumOutstanding(TENANT);
        double collected = repo.sumCollected(TENANT);
        double overdue = repo.sumOverdue(TENANT);
        
        // Sum expenses
        double totalExpenses = expenseRepo.findAllByTenantId(TENANT).stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                .sum();

        s.put("outstanding", outstanding);
        s.put("collected",   collected);
        s.put("overdue",     overdue);
        s.put("total",       outstanding + collected);
        s.put("expenses",    totalExpenses);
        
        return s;
    }

    /**
     * Pending Billables queue retrieval
     */
    public List<PendingBillableDto> getPendingBillables() {
        List<PendingBillableDto> list = new ArrayList<>();

        // Get all source references currently in invoice line items to exclude them
        Set<String> invoicedRefs = lineItemRepo.findAll().stream()
                .map(InvoiceLineItem::getSourceReference)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 1. Filings in stage "Filed"
        List<Filing> filings = filingRepo.findAll().stream()
                .filter(f -> f.getStage() != null && f.getStage().equalsIgnoreCase("Filed"))
                .collect(Collectors.toList());

        for (Filing f : filings) {
            String refKey = "filing_" + f.getId();
            if (!invoicedRefs.contains(refKey)) {
                // Fetch matter/client details
                Long clientId = null;
                String clientName = "No Client";
                if (f.getMatterId() != null) {
                    Optional<Matter> m = matterRepo.findById(f.getMatterId());
                    if (m.isPresent()) {
                        clientId = m.get().getClientId();
                        clientName = m.get().getClientName();
                    }
                }
                list.add(PendingBillableDto.builder()
                        .id(refKey)
                        .type("Filing")
                        .title("Filing Charges — " + f.getTitle())
                        .description("Filing fee for document '" + f.getTitle() + "' filed in " + (f.getCourt() != null ? f.getCourt() : "court"))
                        .suggestedAmount(new BigDecimal("2500.00")) // standard filing fee suggestion
                        .date(f.getFiledDate() != null ? f.getFiledDate() : LocalDate.now())
                        .matterId(f.getMatterId())
                        .matterTitle(f.getMatterTitle())
                        .clientId(clientId)
                        .clientName(clientName)
                        .build());
            }
        }

        // 2. Hearings completed
        List<Hearing> hearings = hearingRepo.findAll().stream()
                .filter(h -> h.getStatus() != null && h.getStatus().equalsIgnoreCase("Completed"))
                .collect(Collectors.toList());

        for (Hearing h : hearings) {
            String refKey = "hearing_" + h.getId();
            if (!invoicedRefs.contains(refKey)) {
                Long clientId = null;
                String clientName = "No Client";
                if (h.getMatterId() != null) {
                    Optional<Matter> m = matterRepo.findById(h.getMatterId());
                    if (m.isPresent()) {
                        clientId = m.get().getClientId();
                        clientName = m.get().getClientName();
                    }
                }
                list.add(PendingBillableDto.builder()
                        .id(refKey)
                        .type("Hearing")
                        .title("Appearance Fees — " + (h.getCourt() != null ? h.getCourt() : "Court"))
                        .description("Court appearance for hearing on " + h.getHearingDate())
                        .suggestedAmount(new BigDecimal("10000.00")) // standard hearing suggestion
                        .date(h.getHearingDate())
                        .matterId(h.getMatterId())
                        .matterTitle(h.getCaseTitle())
                        .clientId(clientId)
                        .clientName(clientName)
                        .build());
            }
        }

        // 3. Drafting tasks completed
        List<Task> tasks = taskRepo.findAll().stream()
                .filter(t -> t.isDone() && t.getType() != null && t.getType().equalsIgnoreCase("Drafting"))
                .collect(Collectors.toList());

        for (Task t : tasks) {
            String refKey = "task_" + t.getId();
            if (!invoicedRefs.contains(refKey)) {
                Long clientId = null;
                String clientName = "No Client";
                if (t.getMatterId() != null) {
                    Optional<Matter> m = matterRepo.findById(t.getMatterId());
                    if (m.isPresent()) {
                        clientId = m.get().getClientId();
                        clientName = m.get().getClientName();
                    }
                }
                list.add(PendingBillableDto.builder()
                        .id(refKey)
                        .type("Task")
                        .title("Drafting Fees — " + t.getTitle())
                        .description("Drafting charges for task: " + t.getTitle())
                        .suggestedAmount(new BigDecimal("5000.00")) // standard drafting fee suggestion
                        .date(t.getDueDate() != null ? t.getDueDate() : LocalDate.now())
                        .matterId(t.getMatterId())
                        .matterTitle(t.getMatterTitle())
                        .clientId(clientId)
                        .clientName(clientName)
                        .build());
            }
        }

        // 4. Billable expenses not yet invoiced
        List<Expense> expenses = expenseRepo.findAllByTenantIdAndBillableAndInvoiced(TENANT, true, false);
        for (Expense e : expenses) {
            String refKey = "expense_" + e.getId();
            list.add(PendingBillableDto.builder()
                    .id(refKey)
                    .type("Expense")
                    .title("Reimbursement — " + e.getCategory())
                    .description("Pass-through expense item: " + e.getCategory() + " incurred on " + e.getDate())
                    .suggestedAmount(e.getAmount())
                    .date(e.getDate())
                    .matterId(e.getMatterId())
                    .matterTitle(e.getMatterTitle())
                    .clientId(e.getClientId())
                    .clientName(e.getClientName())
                    .build());
        }

        return list;
    }

    /**
     * Record a payment
     */
    @Transactional
    public Payment recordPayment(Long invoiceId, Payment payment) {
        Invoice invoice = getById(invoiceId);
        payment.setTenantId(TENANT);
        payment.setInvoiceId(invoiceId);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        Payment saved = paymentRepo.save(payment);

        // Compute new paidAmount
        List<Payment> payments = paymentRepo.findAllByInvoiceId(invoiceId);
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (p.getAmount() != null) {
                totalPaid = totalPaid.add(p.getAmount());
            }
        }
        invoice.setPaidAmount(totalPaid);
        rederiveStatus(invoice);
        repo.save(invoice);

        // Simulation receipt generation & alert logs
        log.info("Recalculated paid amount for Invoice #{}: Paid={}/{}", invoice.getInvoiceNo(), totalPaid, invoice.getAmount());
        if (invoice.getStatus().equals("Paid")) {
            log.info("[RECEIPT AUTO-GENERATOR] Invoice #{} is fully Paid. Auto-generated PDF Receipt. Sent via WhatsApp/Email simulation to client: {}", invoice.getInvoiceNo(), invoice.getClientName());
        }

        return saved;
    }

    /**
     * Retrieve all payments for an invoice
     */
    public List<Payment> getPayments(Long invoiceId) {
        return paymentRepo.findAllByInvoiceId(invoiceId);
    }

    /**
     * Rollups
     */
    public Map<String, Object> getMatterRollup(Long matterId) {
        Map<String, Object> r = new HashMap<>();
        List<Invoice> invoices = repo.findAllByTenantId(TENANT).stream()
                .filter(i -> matterId.equals(i.getMatterId()))
                .collect(Collectors.toList());

        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        for (Invoice i : invoices) {
            billed = billed.add(i.getAmount());
            received = received.add(i.getPaidAmount());
        }

        // Sum expenses
        BigDecimal totalExpenses = BigDecimal.ZERO;
        List<Expense> expenses = expenseRepo.findAllByMatterId(matterId);
        for (Expense e : expenses) {
            if (e.getAmount() != null) {
                totalExpenses = totalExpenses.add(e.getAmount());
            }
        }

        r.put("billed", billed);
        r.put("received", received);
        r.put("outstanding", billed.subtract(received));
        r.put("expenses", totalExpenses);
        r.put("invoices", invoices);
        r.put("expensesList", expenses);
        return r;
    }

    public Map<String, Object> getClientRollup(Long clientId) {
        Map<String, Object> r = new HashMap<>();
        List<Invoice> invoices = repo.findAllByTenantId(TENANT).stream()
                .filter(i -> clientId.equals(i.getClientId()))
                .collect(Collectors.toList());

        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        for (Invoice i : invoices) {
            billed = billed.add(i.getAmount());
            received = received.add(i.getPaidAmount());
        }

        BigDecimal totalExpenses = BigDecimal.ZERO;
        List<Expense> expenses = expenseRepo.findAllByClientId(clientId);
        for (Expense e : expenses) {
            if (e.getAmount() != null) {
                totalExpenses = totalExpenses.add(e.getAmount());
            }
        }

        r.put("billed", billed);
        r.put("received", received);
        r.put("outstanding", billed.subtract(received));
        r.put("expenses", totalExpenses);
        r.put("invoices", invoices);
        return r;
    }

    /**
     * Cron checking past-due invoices daily
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1:00 AM
    @Transactional
    public void dailyOverdueCheck() {
        checkOverdueInvoices();
    }

    @Transactional
    public void checkOverdueInvoices() {
        List<Invoice> all = repo.findAllByTenantId(TENANT);
        LocalDate today = LocalDate.now();
        boolean updatedAny = false;
        for (Invoice i : all) {
            if (!"Paid".equalsIgnoreCase(i.getStatus()) && !"Void".equalsIgnoreCase(i.getStatus())) {
                if (i.getDueDate() != null && i.getDueDate().isBefore(today)) {
                    if (!"Overdue".equalsIgnoreCase(i.getStatus())) {
                        i.setStatus("Overdue");
                        repo.save(i);
                        updatedAny = true;
                        log.info("[CRON] Invoice #{} marked OVERDUE (Due date {} was before today {})", i.getInvoiceNo(), i.getDueDate(), today);
                    }
                }
            }
        }
        if (updatedAny) {
            log.info("[CRON] Daily check for overdue invoices finished.");
        }
    }

    private void rederiveStatus(Invoice invoice) {
        if (invoice.getPaidAmount() == null) {
            invoice.setPaidAmount(BigDecimal.ZERO);
        }
        if (invoice.getAmount() == null) {
            invoice.setAmount(BigDecimal.ZERO);
        }

        if (invoice.getPaidAmount().compareTo(invoice.getAmount()) >= 0) {
            invoice.setStatus("Paid");
            invoice.setPaidDate(LocalDate.now());
        } else if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("Partially Paid");
            invoice.setPaidDate(null);
        } else {
            LocalDate today = LocalDate.now();
            if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(today)) {
                invoice.setStatus("Overdue");
            } else {
                invoice.setStatus("Unpaid");
            }
            invoice.setPaidDate(null);
        }
    }
}
