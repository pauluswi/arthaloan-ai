package com.pswied.core.arthaloan.demo;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanInvestigationServiceTest {

    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 10, 31);

    private LoanInvestigationService service() {
        SyntheticPortfolioGenerator generator = new SyntheticPortfolioGenerator(20261117L, AS_OF_DATE);
        return new LoanInvestigationService(new DemoPortfolioService(generator));
    }

    @Test
    void listsAllLoansWithComputedPrincipalAndDelinquency() {
        LoanInvestigationService service = service();

        assertEquals(12, service.loans().size());

        LoanSummary performing = service.loans().stream()
                .filter(loan -> loan.loanId().equals("LN-001"))
                .findFirst()
                .orElseThrow();
        assertEquals(0, performing.dpd());
        assertTrue(performing.outstandingPrincipal().compareTo(performing.principal()) < 0);

        LoanSummary delinquent = service.loans().stream()
                .filter(loan -> loan.loanId().equals("LN-002"))
                .findFirst()
                .orElseThrow();
        assertEquals(16, delinquent.dpd());
        assertTrue(delinquent.overdueAmount().signum() > 0);
    }

    @Test
    void computesSchedulePaymentAndReversalEffectsForLoanDetail() {
        LoanDetail partialPaymentLoan = service().loan("LN-006");
        InstallmentDetail partialInstallment = partialPaymentLoan.installments().stream()
                .filter(installment -> installment.status().equals("PARTIALLY_PAID"))
                .findFirst()
                .orElseThrow();

        assertEquals(16, partialPaymentLoan.dpd());
        assertTrue(partialInstallment.allocatedAmount().signum() > 0);
        assertTrue(partialInstallment.remainingAmount().signum() > 0);

        LoanDetail reversedPaymentLoan = service().loan("LN-007");
        InstallmentDetail reversedInstallment = reversedPaymentLoan.installments().stream()
                .filter(installment -> installment.dueDate().equals(LocalDate.of(2026, 6, 15)))
                .findFirst()
                .orElseThrow();
        int expectedDpd = Math.toIntExact(ChronoUnit.DAYS.between(reversedInstallment.dueDate(), AS_OF_DATE));

        assertEquals("OVERDUE", reversedInstallment.status());
        assertEquals(0, reversedInstallment.allocatedAmount().signum());
        assertEquals(expectedDpd, reversedPaymentLoan.dpd());
        assertTrue(reversedPaymentLoan.payments().stream()
                .anyMatch(payment -> payment.type() == SyntheticPortfolio.TransactionType.REVERSAL));
    }

    @Test
    void reportsUnknownLoanAsNotFound() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service().loan("UNKNOWN"));

        assertEquals(404, exception.getStatusCode().value());
    }
}
