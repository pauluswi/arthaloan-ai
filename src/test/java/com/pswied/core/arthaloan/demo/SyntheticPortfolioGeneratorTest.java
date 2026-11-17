package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentTransaction;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticPortfolioGeneratorTest {

    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 10, 31);

    @Test
    void producesIdenticalPortfolioForSameSeedAndAsOfDate() {
        SyntheticPortfolio first = new SyntheticPortfolioGenerator(1234L, AS_OF_DATE).generate();
        SyntheticPortfolio second = new SyntheticPortfolioGenerator(1234L, AS_OF_DATE).generate();

        assertEquals(first, second);
    }

    @Test
    void usesSeedToVaryGeneratedLoanAmounts() {
        SyntheticPortfolio first = new SyntheticPortfolioGenerator(1234L, AS_OF_DATE).generate();
        SyntheticPortfolio second = new SyntheticPortfolioGenerator(5678L, AS_OF_DATE).generate();

        assertNotEquals(first.loans().getFirst().principal(), second.loans().getFirst().principal());
    }

    @Test
    void respectsConfiguredAsOfDateForScheduleAndPayments() {
        LocalDate asOfDate = LocalDate.of(2026, 10, 10);
        SyntheticPortfolio portfolio = new SyntheticPortfolioGenerator(1234L, asOfDate).generate();

        assertTrue(portfolio.installments().stream()
                .anyMatch(installment -> installment.status().equals("SCHEDULED")
                        && installment.dueDate().isAfter(asOfDate)));
        assertTrue(portfolio.payments().stream()
                .allMatch(payment -> !payment.postingDate().isAfter(asOfDate)));
    }

    @Test
    void createsLinkedSyntheticRecordsAndRepaymentEdgeCases() {
        SyntheticPortfolio portfolio = new SyntheticPortfolioGenerator(20261117L, AS_OF_DATE).generate();

        assertEquals("USD", portfolio.currency());
        assertEquals(12, portfolio.customers().size());
        assertEquals(12, portfolio.loans().size());
        assertEquals(2, portfolio.products().size());
        assertEquals(144, portfolio.installments().size());
        assertEquals(12, new HashSet<>(portfolio.loans().stream().map(SyntheticPortfolio.LoanAccount::loanId).toList()).size());
        assertTrue(portfolio.installments().stream().anyMatch(installment -> installment.status().equals("OVERDUE")));
        assertTrue(portfolio.installments().stream().anyMatch(installment -> installment.status().equals("PARTIALLY_PAID")));
        assertTrue(portfolio.installments().stream().anyMatch(installment -> installment.status().equals("SCHEDULED")));
        assertTrue(portfolio.payments().stream().anyMatch(payment -> payment.type() == TransactionType.REVERSAL));
        assertTrue(portfolio.customers().stream().map(SyntheticPortfolio.Customer::segment).distinct().count() > 1);
        assertTrue(portfolio.customers().stream().map(SyntheticPortfolio.Customer::region).distinct().count() > 1);

        Map<String, SyntheticPortfolio.LoanAccount> loansById = portfolio.loans().stream()
                .collect(Collectors.toMap(SyntheticPortfolio.LoanAccount::loanId, Function.identity()));
        for (SyntheticPortfolio.LoanAccount loan : portfolio.loans()) {
            assertTrue(loan.principal().compareTo(new BigDecimal("5000.00")) >= 0);
            assertTrue(loan.annualInterestRatePercent().compareTo(new BigDecimal("8.00")) >= 0);
            assertTrue(loan.annualInterestRatePercent().compareTo(new BigDecimal("18.00")) <= 0);
            assertEquals(loan.principal(), portfolio.installments().stream()
                    .filter(installment -> installment.loanId().equals(loan.loanId()))
                    .map(SyntheticPortfolio.Installment::principalDue)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add));
        }

        for (PaymentTransaction payment : portfolio.payments()) {
            assertTrue(loansById.containsKey(payment.loanId()));
            assertTrue(payment.allocations().stream().allMatch(allocation ->
                    portfolio.installments().stream()
                            .anyMatch(installment -> installment.installmentId().equals(allocation.installmentId()))));
            if (payment.type() == TransactionType.REVERSAL) {
                assertFalse(payment.reversesPaymentId().isBlank());
                PaymentTransaction original = portfolio.payments().stream()
                        .filter(candidate -> candidate.paymentId().equals(payment.reversesPaymentId()))
                        .findFirst()
                        .orElseThrow();
                assertEquals(TransactionType.PAYMENT, original.type());
                assertEquals(BigDecimal.ZERO.setScale(2), original.amount().add(payment.amount()));
            }
        }
    }
}
