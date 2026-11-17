package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.SyntheticPortfolio.Customer;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.Installment;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.LoanAccount;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.LoanProduct;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentAllocation;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentTransaction;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class LoanInvestigationService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SyntheticPortfolio portfolio;
    private final Map<String, Customer> customersById;
    private final Map<String, LoanProduct> productsById;
    private final Map<String, Installment> installmentsById;

    public LoanInvestigationService(DemoPortfolioService portfolioService) {
        this.portfolio = portfolioService.portfolio();
        this.customersById = portfolio.customers().stream()
                .collect(Collectors.toUnmodifiableMap(Customer::customerId, Function.identity()));
        this.productsById = portfolio.products().stream()
                .collect(Collectors.toUnmodifiableMap(LoanProduct::productId, Function.identity()));
        this.installmentsById = portfolio.installments().stream()
                .collect(Collectors.toUnmodifiableMap(Installment::installmentId, Function.identity()));
    }

    public List<LoanSummary> loans() {
        return portfolio.loans().stream()
                .sorted(Comparator.comparing(LoanAccount::loanId))
                .map(this::summary)
                .toList();
    }

    public LoanDetail loan(String loanId) {
        LoanAccount loan = portfolio.loans().stream()
                .filter(candidate -> candidate.loanId().equals(loanId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Loan not found: " + loanId));

        List<PaymentTransaction> payments = paymentsFor(loanId);
        List<InstallmentDetail> schedule = portfolio.installments().stream()
                .filter(installment -> installment.loanId().equals(loanId))
                .sorted(Comparator.comparing(Installment::dueDate).thenComparing(Installment::installmentId))
                .map(installment -> installmentDetail(installment, payments))
                .toList();
        LoanSummary summary = summary(loan);

        return new LoanDetail(
                loan,
                customersById.get(loan.customerId()),
                productsById.get(loan.productId()),
                portfolio.currency(),
                portfolio.asOfDate(),
                summary.dpd(),
                summary.overdueAmount(),
                summary.outstandingPrincipal(),
                schedule,
                payments);
    }

    private LoanSummary summary(LoanAccount loan) {
        List<PaymentTransaction> payments = paymentsFor(loan.loanId());
        List<InstallmentDetail> schedule = portfolio.installments().stream()
                .filter(installment -> installment.loanId().equals(loan.loanId()))
                .map(installment -> installmentDetail(installment, payments))
                .toList();
        Customer customer = customersById.get(loan.customerId());
        LoanProduct product = productsById.get(loan.productId());

        return new LoanSummary(
                loan.loanId(),
                loan.customerId(),
                customer.segment(),
                customer.region(),
                loan.productId(),
                product.name(),
                loan.principal(),
                outstandingPrincipal(loan, payments),
                portfolio.currency(),
                loan.status(),
                dpd(schedule),
                overdueAmount(schedule),
                portfolio.asOfDate());
    }

    private List<PaymentTransaction> paymentsFor(String loanId) {
        return portfolio.payments().stream()
                .filter(payment -> payment.loanId().equals(loanId)
                        && !payment.postingDate().isAfter(portfolio.asOfDate()))
                .sorted(Comparator.comparing(PaymentTransaction::postingDate)
                        .thenComparing(PaymentTransaction::paymentId))
                .toList();
    }

    private InstallmentDetail installmentDetail(
            Installment installment,
            List<PaymentTransaction> payments) {
        BigDecimal allocated = payments.stream()
                .flatMap(payment -> payment.allocations().stream())
                .filter(allocation -> allocation.installmentId().equals(installment.installmentId()))
                .map(PaymentAllocation::amount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal scheduled = installment.scheduledAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = scheduled.subtract(allocated).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        int installmentDpd = installment.dueDate().isAfter(portfolio.asOfDate())
                ? 0
                : Math.toIntExact(ChronoUnit.DAYS.between(installment.dueDate(), portfolio.asOfDate()));
        String status;
        if (installment.dueDate().isAfter(portfolio.asOfDate())) {
            status = "SCHEDULED";
        } else if (remaining.signum() == 0) {
            status = "PAID";
        } else if (remaining.compareTo(scheduled) < 0) {
            status = "PARTIALLY_PAID";
        } else if (installmentDpd == 0) {
            status = "DUE";
        } else {
            status = "OVERDUE";
        }

        return new InstallmentDetail(
                installment.installmentId(),
                installment.dueDate(),
                installment.principalDue(),
                installment.interestDue(),
                installment.feesDue(),
                scheduled,
                allocated,
                remaining,
                installmentDpd,
                status);
    }

    private BigDecimal outstandingPrincipal(LoanAccount loan, List<PaymentTransaction> payments) {
        BigDecimal outstanding = portfolio.installments().stream()
                .filter(installment -> installment.loanId().equals(loan.loanId()))
                .map(installment -> installment.principalDue().subtract(
                        payments.stream()
                                .flatMap(payment -> payment.allocations().stream())
                                .filter(allocation -> allocation.installmentId().equals(installment.installmentId()))
                                .map(PaymentAllocation::principalAmount)
                                .reduce(ZERO, BigDecimal::add)))
                .reduce(ZERO, BigDecimal::add);
        return outstanding.max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private int dpd(List<InstallmentDetail> schedule) {
        return schedule.stream()
                .filter(installment -> !installment.dueDate().isAfter(portfolio.asOfDate())
                        && installment.remainingAmount().signum() > 0)
                .min(Comparator.comparing(InstallmentDetail::dueDate))
                .map(InstallmentDetail::dpd)
                .orElse(0);
    }

    private BigDecimal overdueAmount(List<InstallmentDetail> schedule) {
        return schedule.stream()
                .filter(installment -> installment.dpd() > 0)
                .map(InstallmentDetail::remainingAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
