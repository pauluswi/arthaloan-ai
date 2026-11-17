package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.PortfolioAnalytics.AppliedFilters;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.DelinquencyBucket;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.FilterOptions;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.LoanProductOption;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.MonthlyTrend;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.ParMetric;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.Summary;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PortfolioAnalyticsService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final int[] PAR_THRESHOLDS = {1, 7, 30, 90};
    private static final String REPAYMENT_RATE_DEFINITION =
            "Period-posted allocations to installments due in the selected period, net of reversals, "
                    + "divided by scheduled principal, interest, and fees due in the period.";
    private static final String DPD_DEFINITION =
            "Calendar days from the oldest unpaid installment due date to the as-of date; no grace period.";
    private static final String PAR_DEFINITION =
            "Outstanding principal for loans with DPD greater than or equal to the threshold divided by "
                    + "total outstanding principal as of the reporting date.";
    private static final String OUTSTANDING_DEFINITION =
            "Original scheduled principal less net principal allocations posted by the as-of date.";

    private final SyntheticPortfolio portfolio;
    private final List<LoanSummary> loanSummaries;
    private final Map<String, Customer> customersById;
    private final Map<String, LoanProduct> productsById;
    private final Map<String, Installment> installmentsById;
    private final Map<String, List<PaymentTransaction>> paymentsByLoan;

    public PortfolioAnalyticsService(
            DemoPortfolioService portfolioService,
            LoanInvestigationService loanInvestigationService) {
        this.portfolio = portfolioService.portfolio();
        this.loanSummaries = loanInvestigationService.loans();
        this.customersById = portfolio.customers().stream()
                .collect(Collectors.toUnmodifiableMap(Customer::customerId, Function.identity()));
        this.productsById = portfolio.products().stream()
                .collect(Collectors.toUnmodifiableMap(LoanProduct::productId, Function.identity()));
        this.installmentsById = portfolio.installments().stream()
                .collect(Collectors.toUnmodifiableMap(Installment::installmentId, Function.identity()));
        this.paymentsByLoan = portfolio.payments().stream()
                .collect(Collectors.groupingBy(PaymentTransaction::loanId));
    }

    public PortfolioAnalytics calculate(
            LocalDate requestedPeriodStart,
            LocalDate requestedPeriodEnd,
            String productId,
            String segment,
            String region,
            String loanStatus) {
        LocalDate periodEnd = requestedPeriodEnd == null ? portfolio.asOfDate() : requestedPeriodEnd;
        YearMonth reportingMonth = YearMonth.from(periodEnd);
        LocalDate periodStart = requestedPeriodStart == null
                ? reportingMonth.atDay(1)
                : requestedPeriodStart;

        validateFilters(periodStart, periodEnd, productId, segment, region, loanStatus);
        AppliedFilters filters = new AppliedFilters(productId, segment, region, loanStatus);
        List<LoanSummary> selectedLoans = loanSummaries.stream()
                .filter(loan -> matches(loan, filters))
                .toList();

        BigDecimal outstanding = sum(selectedLoans.stream().map(LoanSummary::outstandingPrincipal));
        BigDecimal scheduled = scheduledAmount(selectedLoans, periodStart, periodEnd);
        BigDecimal collected = collectedAmount(selectedLoans, periodStart, periodEnd);
        Summary summary = new Summary(
                selectedLoans.size(),
                outstanding,
                scheduled,
                collected,
                percentage(collected, scheduled));

        return new PortfolioAnalytics(
                periodStart,
                periodEnd,
                portfolio.asOfDate(),
                portfolio.currency(),
                filters,
                summary,
                parMetrics(selectedLoans, outstanding),
                delinquencyBuckets(selectedLoans),
                repaymentTrend(periodEnd, filters),
                new PortfolioAnalytics.MetricDefinitions(
                        REPAYMENT_RATE_DEFINITION,
                        DPD_DEFINITION,
                        PAR_DEFINITION,
                        OUTSTANDING_DEFINITION),
                filterOptions());
    }

    private void validateFilters(
            LocalDate periodStart,
            LocalDate periodEnd,
            String productId,
            String segment,
            String region,
            String loanStatus) {
        if (periodStart.isAfter(periodEnd)) {
            throw new ResponseStatusException(BAD_REQUEST, "periodStart must not be after periodEnd.");
        }
        if (periodEnd.isAfter(portfolio.asOfDate())) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "periodEnd must not be after the portfolio as-of date " + portfolio.asOfDate() + ".");
        }
        if (productId != null && productsById.values().stream().noneMatch(product -> product.productId().equals(productId))) {
            throw new ResponseStatusException(BAD_REQUEST, "Unknown productId: " + productId);
        }
        if (segment != null && portfolio.customers().stream().noneMatch(customer -> customer.segment().equals(segment))) {
            throw new ResponseStatusException(BAD_REQUEST, "Unknown segment: " + segment);
        }
        if (region != null && portfolio.customers().stream().noneMatch(customer -> customer.region().equals(region))) {
            throw new ResponseStatusException(BAD_REQUEST, "Unknown region: " + region);
        }
        if (loanStatus != null && portfolio.loans().stream().noneMatch(loan -> loan.status().equals(loanStatus))) {
            throw new ResponseStatusException(BAD_REQUEST, "Unknown loanStatus: " + loanStatus);
        }
    }

    private boolean matches(LoanSummary loan, AppliedFilters filters) {
        return (filters.productId() == null || filters.productId().equals(loan.productId()))
                && (filters.segment() == null || filters.segment().equals(loan.segment()))
                && (filters.region() == null || filters.region().equals(loan.region()))
                && (filters.loanStatus() == null || filters.loanStatus().equals(loan.status()));
    }

    private BigDecimal scheduledAmount(List<LoanSummary> loans, LocalDate start, LocalDate end) {
        return sum(loans.stream()
                .flatMap(loan -> installmentsFor(loan.loanId()).stream())
                .filter(installment -> inPeriod(installment.dueDate(), start, end))
                .map(Installment::scheduledAmount));
    }

    private BigDecimal collectedAmount(List<LoanSummary> loans, LocalDate start, LocalDate end) {
        return sum(loans.stream()
                .map(LoanSummary::loanId)
                .flatMap(loanId -> paymentsByLoan.getOrDefault(loanId, List.of()).stream())
                .filter(payment -> inPeriod(payment.postingDate(), start, end))
                .flatMap(payment -> payment.allocations().stream())
                .filter(allocation -> {
                    Installment installment = installmentsById.get(allocation.installmentId());
                    return installment != null && inPeriod(installment.dueDate(), start, end);
                })
                .map(PaymentAllocation::amount));
    }

    private List<ParMetric> parMetrics(List<LoanSummary> loans, BigDecimal portfolioPrincipal) {
        return Arrays.stream(PAR_THRESHOLDS)
                .mapToObj(threshold -> {
                    List<LoanSummary> qualifying = loans.stream()
                            .filter(loan -> loan.dpd() >= threshold)
                            .toList();
                    BigDecimal atRisk = sum(qualifying.stream().map(LoanSummary::outstandingPrincipal));
                    return new ParMetric(
                            threshold,
                            qualifying.size(),
                            atRisk,
                            portfolioPrincipal,
                            percentage(atRisk, portfolioPrincipal));
                })
                .toList();
    }

    private List<DelinquencyBucket> delinquencyBuckets(List<LoanSummary> loans) {
        return List.of(
                bucket("Current", loans, loan -> loan.dpd() == 0),
                bucket("1–30 DPD", loans, loan -> loan.dpd() >= 1 && loan.dpd() <= 30),
                bucket("31–60 DPD", loans, loan -> loan.dpd() >= 31 && loan.dpd() <= 60),
                bucket("61–90 DPD", loans, loan -> loan.dpd() >= 61 && loan.dpd() <= 90),
                bucket(">90 DPD", loans, loan -> loan.dpd() > 90));
    }

    private DelinquencyBucket bucket(
            String name,
            List<LoanSummary> loans,
            java.util.function.Predicate<LoanSummary> predicate) {
        List<LoanSummary> matching = loans.stream().filter(predicate).toList();
        return new DelinquencyBucket(
                name,
                matching.size(),
                sum(matching.stream().map(LoanSummary::outstandingPrincipal)));
    }

    private List<MonthlyTrend> repaymentTrend(LocalDate periodEnd, AppliedFilters filters) {
        YearMonth lastMonth = YearMonth.from(periodEnd);
        return Stream.iterate(lastMonth.minusMonths(5), month -> month.plusMonths(1))
                .limit(6)
                .map(month -> {
                    LocalDate start = month.atDay(1);
                    LocalDate end = month.atEndOfMonth().isAfter(portfolio.asOfDate())
                            ? portfolio.asOfDate()
                            : month.atEndOfMonth();
                    List<LoanSummary> loans = loanSummaries.stream()
                            .filter(loan -> matches(loan, filters))
                            .toList();
                    BigDecimal scheduled = scheduledAmount(loans, start, end);
                    BigDecimal collected = collectedAmount(loans, start, end);
                    return new MonthlyTrend(start, end, scheduled, collected, percentage(collected, scheduled));
                })
                .toList();
    }

    private FilterOptions filterOptions() {
        return new FilterOptions(
                portfolio.products().stream()
                        .sorted(Comparator.comparing(LoanProduct::name))
                        .map(product -> new LoanProductOption(product.productId(), product.name()))
                        .toList(),
                portfolio.customers().stream()
                        .map(Customer::segment)
                        .distinct()
                        .sorted()
                        .toList(),
                portfolio.customers().stream()
                        .map(Customer::region)
                        .distinct()
                        .sorted()
                        .toList(),
                portfolio.loans().stream()
                        .map(LoanAccount::status)
                        .distinct()
                        .sorted()
                        .toList());
    }

    private List<Installment> installmentsFor(String loanId) {
        return portfolio.installments().stream()
                .filter(installment -> installment.loanId().equals(loanId))
                .toList();
    }

    private static boolean inPeriod(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private static BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
        return values.reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
