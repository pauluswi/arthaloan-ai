package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SyntheticPortfolio(
        long seed,
        LocalDate asOfDate,
        String currency,
        List<LoanProduct> products,
        List<Customer> customers,
        List<LoanAccount> loans,
        List<Installment> installments,
        List<PaymentTransaction> payments) {

    public SyntheticPortfolio {
        products = List.copyOf(products);
        customers = List.copyOf(customers);
        loans = List.copyOf(loans);
        installments = List.copyOf(installments);
        payments = List.copyOf(payments);
    }

    public record LoanProduct(String productId, String name, String repaymentFrequency) {
    }

    public record Customer(String customerId, String segment, String region) {
    }

    public record LoanAccount(
            String loanId,
            String customerId,
            String productId,
            BigDecimal principal,
            BigDecimal annualInterestRatePercent,
            int tenorMonths,
            LocalDate originationDate,
            LocalDate maturityDate,
            String status) {
    }

    public record Installment(
            String installmentId,
            String loanId,
            LocalDate dueDate,
            BigDecimal principalDue,
            BigDecimal interestDue,
            BigDecimal feesDue,
            String status) {

        public BigDecimal scheduledAmount() {
            return principalDue.add(interestDue).add(feesDue);
        }
    }

    public record PaymentTransaction(
            String paymentId,
            String loanId,
            LocalDate postingDate,
            BigDecimal amount,
            String currency,
            TransactionType type,
            String reversesPaymentId,
            List<PaymentAllocation> allocations) {

        public PaymentTransaction {
            allocations = List.copyOf(allocations);
        }
    }

    public record PaymentAllocation(
            String installmentId,
            BigDecimal principalAmount,
            BigDecimal interestAmount,
            BigDecimal feesAmount) {

        public BigDecimal amount() {
            return principalAmount.add(interestAmount).add(feesAmount);
        }
    }

    public enum TransactionType {
        PAYMENT,
        REVERSAL
    }
}
