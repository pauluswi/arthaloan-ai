package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.SyntheticPortfolio.Customer;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.Installment;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.LoanAccount;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.LoanProduct;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentAllocation;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentTransaction;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class SyntheticPortfolioGenerator {

    private static final String CURRENCY = "USD";
    private static final int TENOR_MONTHS = 12;
    private static final int HISTORY_MONTHS = 6;
    private static final int ACCOUNT_COUNT = 12;
    private static final List<String> SEGMENTS = List.of("PRIME", "MASS_MARKET", "EMERGING");
    private static final List<String> REGIONS = List.of("NORTH_AMERICA", "EUROPE", "ASIA_PACIFIC");

    private final long seed;
    private final LocalDate asOfDate;

    public SyntheticPortfolioGenerator(
            @Value("${arthaloan.demo.seed:20261117}") long seed,
            @Value("${arthaloan.demo.as-of-date:2026-10-31}") LocalDate asOfDate) {
        this.seed = seed;
        this.asOfDate = asOfDate;
    }

    public SyntheticPortfolio generate() {
        Random random = new Random(seed);
        List<LoanProduct> products = List.of(
                new LoanProduct("PL-01", "Personal Loan", "MONTHLY"),
                new LoanProduct("AL-01", "Auto Loan", "MONTHLY"));
        List<Customer> customers = new ArrayList<>();
        List<LoanAccount> loans = new ArrayList<>();
        List<Installment> installments = new ArrayList<>();
        List<PaymentTransaction> payments = new ArrayList<>();

        for (int accountNumber = 1; accountNumber <= ACCOUNT_COUNT; accountNumber++) {
            String customerId = id("CUS", accountNumber);
            String loanId = id("LN", accountNumber);
            String productId = accountNumber % 3 == 0 ? "AL-01" : "PL-01";
            LocalDate originationDate = asOfDate.withDayOfMonth(1)
                    .minusMonths(HISTORY_MONTHS)
                    .plusDays(accountNumber - 1L);
            BigDecimal principal = BigDecimal.valueOf(
                    5_000L + accountNumber * 1_250L + random.nextInt(10) * 100L).setScale(2);
            BigDecimal annualRate = BigDecimal.valueOf(8 + random.nextInt(11)).setScale(2);

            customers.add(new Customer(
                    customerId,
                    SEGMENTS.get((accountNumber - 1) % SEGMENTS.size()),
                    REGIONS.get((accountNumber - 1) % REGIONS.size())));
            loans.add(new LoanAccount(
                    loanId,
                    customerId,
                    productId,
                    principal,
                    annualRate,
                    TENOR_MONTHS,
                    originationDate,
                    originationDate.plusMonths(TENOR_MONTHS),
                    "ACTIVE"));

            generateScheduleAndPayments(
                    accountNumber, loanId, principal, annualRate, originationDate,
                    installments, payments);
        }

        return new SyntheticPortfolio(
                seed, asOfDate, CURRENCY, products, customers, loans, installments, payments);
    }

    private void generateScheduleAndPayments(
            int accountNumber,
            String loanId,
            BigDecimal principal,
            BigDecimal annualRate,
            LocalDate originationDate,
            List<Installment> installments,
            List<PaymentTransaction> payments) {
        BigDecimal monthlyPrincipal = principal.divide(
                BigDecimal.valueOf(TENOR_MONTHS), 2, RoundingMode.DOWN);
        BigDecimal monthlyInterest = principal.multiply(annualRate)
                .divide(BigDecimal.valueOf(1_200), 2, RoundingMode.HALF_UP);
        int dueHistoryMonths = 0;
        for (int month = 1; month <= TENOR_MONTHS; month++) {
            LocalDate dueDate = originationDate.plusMonths(month).withDayOfMonth(15);
            if (!dueDate.isAfter(asOfDate)) {
                dueHistoryMonths++;
            }
        }
        int missedInstallments = Math.min(missedInstallmentCount(accountNumber), dueHistoryMonths);
        int firstMissedMonth = dueHistoryMonths - missedInstallments + 1;

        for (int month = 1; month <= TENOR_MONTHS; month++) {
            String installmentId = id("INS-" + loanId.substring(3), month);
            BigDecimal principalDue = month == TENOR_MONTHS
                    ? principal.subtract(monthlyPrincipal.multiply(BigDecimal.valueOf(TENOR_MONTHS - 1)))
                    : monthlyPrincipal;
            LocalDate dueDate = originationDate.plusMonths(month).withDayOfMonth(15);
            BigDecimal interestDue = monthlyInterest;
            BigDecimal feesDue = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

            if (dueDate.isAfter(asOfDate)) {
                installments.add(new Installment(
                        installmentId, loanId, dueDate, principalDue, interestDue, feesDue, "SCHEDULED"));
                continue;
            }

            boolean reversedPayment = accountNumber == 7 && month == 2;
            boolean missed = month >= firstMissedMonth && missedInstallments > 0;
            boolean partialPayment = accountNumber == 6 && month == dueHistoryMonths;

            if (reversedPayment) {
                addPayment(payments, loanId, installmentId, postingDate(dueDate, 2),
                        principalDue, interestDue, feesDue, null);
                PaymentTransaction original = payments.getLast();
                addPayment(payments, loanId, installmentId, postingDate(dueDate, 4),
                        principalDue.negate(), interestDue.negate(), feesDue.negate(), original.paymentId());
                installments.add(new Installment(
                        installmentId, loanId, dueDate, principalDue, interestDue, feesDue, "OVERDUE"));
            } else if (partialPayment) {
                BigDecimal paidPrincipal = principalDue.divide(BigDecimal.valueOf(2), 2, RoundingMode.DOWN);
                BigDecimal paidInterest = interestDue.divide(BigDecimal.valueOf(2), 2, RoundingMode.DOWN);
                addPayment(payments, loanId, installmentId, postingDate(dueDate, 2),
                        paidPrincipal, paidInterest, BigDecimal.ZERO.setScale(2), null);
                installments.add(new Installment(
                        installmentId, loanId, dueDate, principalDue, interestDue, feesDue, "PARTIALLY_PAID"));
            } else if (missed) {
                installments.add(new Installment(
                        installmentId, loanId, dueDate, principalDue, interestDue, feesDue, "OVERDUE"));
            } else {
                addPayment(payments, loanId, installmentId, postingDate(dueDate, 2),
                        principalDue, interestDue, feesDue, null);
                installments.add(new Installment(
                        installmentId, loanId, dueDate, principalDue, interestDue, feesDue, "PAID"));
            }
        }
    }

    private LocalDate postingDate(LocalDate dueDate, int daysAfterDue) {
        return dueDate.plusDays(daysAfterDue).isAfter(asOfDate)
                ? asOfDate
                : dueDate.plusDays(daysAfterDue);
    }

    private static void addPayment(
            List<PaymentTransaction> payments,
            String loanId,
            String installmentId,
            LocalDate postingDate,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal fees,
            String reversesPaymentId) {
        String paymentId = id("PAY", payments.size() + 1);
        TransactionType type = reversesPaymentId == null ? TransactionType.PAYMENT : TransactionType.REVERSAL;
        payments.add(new PaymentTransaction(
                paymentId,
                loanId,
                postingDate,
                principal.add(interest).add(fees),
                CURRENCY,
                type,
                reversesPaymentId,
                List.of(new PaymentAllocation(installmentId, principal, interest, fees))));
    }

    private static int missedInstallmentCount(int accountNumber) {
        return switch (accountNumber) {
            case 1, 10 -> 0;
            case 2, 6, 12 -> 1;
            case 3, 8 -> 2;
            case 4, 11 -> 3;
            case 5 -> 4;
            case 9 -> HISTORY_MONTHS;
            case 7 -> 0;
            default -> throw new IllegalArgumentException("Unexpected account number: " + accountNumber);
        };
    }

    private static String id(String prefix, int value) {
        return "%s-%03d".formatted(prefix, value);
    }
}
