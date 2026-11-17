package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.AssistantAnswer.Evidence;
import com.pswied.core.arthaloan.demo.AssistantAnswer.Provenance;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.ParMetric;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.Summary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DemoAssistantService {

    private static final Pattern PAR_THRESHOLD = Pattern.compile("\\bpar\\s*(1|7|30|90)\\b");
    private static final String SOURCE = "Deterministic portfolio analytics API; no language model used.";

    private final PortfolioAnalyticsService analyticsService;

    public DemoAssistantService(PortfolioAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public AssistantAnswer answer(AssistantQuestion request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required.");
        }

        String question = request.question().strip();
        String normalized = question.toLowerCase(Locale.ROOT);
        if (normalized.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not exceed 500 characters.");
        }
        PortfolioAnalytics analytics = calculate(request, request.segment());

        if (asksForSegmentPar(normalized)) {
            return segmentParAnswer(request, question, normalized);
        }
        if (containsChangeLanguage(normalized)) {
            return answer(
                    "INSUFFICIENT_DATA",
                    request,
                    question,
                    "This demo assistant does not calculate historical changes. It can report current PAR snapshots "
                            + "and repayment metrics for the selected reporting period.",
                    analytics,
                    normalized.contains("par")
                            ? analytics.definitions().portfolioAtRisk()
                            : analytics.definitions().repaymentRate(),
                    List.of());
        }
        Matcher parMatcher = PAR_THRESHOLD.matcher(normalized);
        if (parMatcher.find()) {
            return parAnswer(request, question, Integer.parseInt(parMatcher.group(1)));
        }
        if (asksForSummary(normalized)) {
            return summaryAnswer(request, question);
        }
        if (asksForDelinquency(normalized)) {
            return delinquencyAnswer(request, question);
        }
        return answer(
                "UNSUPPORTED",
                request,
                question,
                "I can answer portfolio summary, repayment rate, delinquency distribution, PAR 1/7/30/90, "
                        + "and current segment contribution to PAR. I cannot infer an increase without historical snapshots.",
                analytics,
                null,
                List.of());
    }

    private AssistantAnswer summaryAnswer(AssistantQuestion request, String question) {
        PortfolioAnalytics analytics = calculate(request, request.segment());
        Summary summary = analytics.summary();
        String rate = summary.repaymentRatePercent() == null
                ? "unavailable because no scheduled amount is due"
                : number(summary.repaymentRatePercent()) + "%";
        String text = ("For %s through %s, the selected portfolio has %d loans, %s outstanding principal, "
                + "%s scheduled, and %s collected. The repayment rate is %s."
                ).formatted(
                        analytics.periodStart(),
                        analytics.periodEnd(),
                        summary.activeLoans(),
                        money(summary.outstandingPrincipal(), analytics.currency()),
                        money(summary.scheduledAmount(), analytics.currency()),
                        money(summary.amountCollected(), analytics.currency()),
                        rate);
        List<Evidence> evidence = List.of(
                new Evidence("Active loans", Long.toString(summary.activeLoans())),
                new Evidence("Outstanding principal", money(summary.outstandingPrincipal(), analytics.currency())),
                new Evidence("Scheduled amount", money(summary.scheduledAmount(), analytics.currency())),
                new Evidence("Amount collected", money(summary.amountCollected(), analytics.currency())),
                new Evidence("Repayment rate", rate));
        return answer("ANSWERED", request, question, text, analytics,
                analytics.definitions().repaymentRate(), evidence);
    }

    private AssistantAnswer parAnswer(AssistantQuestion request, String question, int threshold) {
        PortfolioAnalytics analytics = calculate(request, request.segment());
        ParMetric metric = analytics.portfolioAtRisk().stream()
                .filter(item -> item.thresholdDays() == threshold)
                .findFirst()
                .orElseThrow();
        String ratio = metric.ratioPercent() == null
                ? "unavailable because selected portfolio principal is zero"
                : number(metric.ratioPercent()) + "%";
        String text = ("PAR %d is %s (%s at-risk principal of %s outstanding principal), using DPD >= %d "
                + "as of %s."
                ).formatted(
                        threshold,
                        ratio,
                        money(metric.atRiskPrincipal(), analytics.currency()),
                        money(metric.portfolioPrincipal(), analytics.currency()),
                        threshold,
                        analytics.asOfDate());
        return answer(
                "ANSWERED",
                request,
                question,
                text,
                analytics,
                analytics.definitions().portfolioAtRisk(),
                List.of(
                        new Evidence("PAR " + threshold, ratio),
                        new Evidence("At-risk principal", money(metric.atRiskPrincipal(), analytics.currency())),
                        new Evidence("Portfolio principal", money(metric.portfolioPrincipal(), analytics.currency())),
                        new Evidence("Qualifying loans", Long.toString(metric.qualifyingLoans()))));
    }

    private AssistantAnswer segmentParAnswer(
            AssistantQuestion request,
            String question,
            String normalizedQuestion) {
        PortfolioAnalytics allSegments = calculate(request, null);
        Matcher matcher = PAR_THRESHOLD.matcher(normalizedQuestion);
        int threshold = matcher.find() ? Integer.parseInt(matcher.group(1)) : 30;

        if (containsChangeLanguage(normalizedQuestion)) {
            return answer(
                    "INSUFFICIENT_DATA",
                    request,
                    question,
                    "I can compare current segment contributions to PAR %d, but this demo has only one as-of snapshot "
                            .formatted(threshold)
                            + "and cannot establish an increase or decrease over time.",
                    allSegments,
                    allSegments.definitions().portfolioAtRisk(),
                    List.of());
        }

        List<PortfolioAnalytics> segments = allSegments.filterOptions().segments().stream()
                .map(segment -> analyticsService.calculate(
                        request.periodStart(),
                        request.periodEnd(),
                        request.productId(),
                        segment,
                        request.region(),
                        request.loanStatus()))
                .toList();
        List<SegmentContribution> contributions = segments.stream()
                .map(analytics -> {
                    ParMetric metric = analytics.portfolioAtRisk().stream()
                            .filter(item -> item.thresholdDays() == threshold)
                            .findFirst()
                            .orElseThrow();
                    return new SegmentContribution(
                            analytics.filters().segment(),
                            metric.atRiskPrincipal(),
                            metric.ratioPercent(),
                            metric.qualifyingLoans());
                })
                .sorted(Comparator.comparing(SegmentContribution::atRiskPrincipal).reversed()
                        .thenComparing(SegmentContribution::segment))
                .toList();
        if (contributions.isEmpty() || contributions.getFirst().atRiskPrincipal().signum() == 0) {
            return answer(
                    "INSUFFICIENT_DATA",
                    request,
                    question,
                    "No segment has outstanding principal meeting the PAR %d threshold in this snapshot."
                            .formatted(threshold),
                    allSegments,
                    allSegments.definitions().portfolioAtRisk(),
                    List.of());
        }

        SegmentContribution top = contributions.getFirst();
        String text = ("%s contributes the largest amount to current PAR %d: %s at-risk principal (%s of that "
                + "segment's outstanding principal), across %d qualifying loans. This is a current snapshot "
                + "comparison, not a change over time."
                ).formatted(
                        top.segment(),
                        threshold,
                        money(top.atRiskPrincipal(), allSegments.currency()),
                        top.ratioPercent() == null ? "unavailable" : number(top.ratioPercent()) + "%",
                        top.qualifyingLoans());
        List<Evidence> evidence = contributions.stream()
                .map(item -> new Evidence(
                        item.segment() + " PAR " + threshold,
                        money(item.atRiskPrincipal(), allSegments.currency())
                                + " at risk; "
                                + (item.ratioPercent() == null ? "ratio unavailable" : number(item.ratioPercent()) + "%")
                                + "; "
                                + item.qualifyingLoans()
                                + " qualifying loans"))
                .toList();
        return answer(
                "ANSWERED",
                request,
                question,
                text,
                allSegments,
                allSegments.definitions().portfolioAtRisk(),
                evidence);
    }

    private AssistantAnswer delinquencyAnswer(AssistantQuestion request, String question) {
        PortfolioAnalytics analytics = calculate(request, request.segment());
        long delinquentLoans = analytics.delinquencyDistribution().stream()
                .filter(bucket -> !bucket.bucket().equals("Current"))
                .mapToLong(PortfolioAnalytics.DelinquencyBucket::loanCount)
                .sum();
        List<Evidence> evidence = analytics.delinquencyDistribution().stream()
                .map(bucket -> new Evidence(bucket.bucket(), bucket.loanCount() + " loans"))
                .toList();
        String text = "The selected snapshot has %d delinquent loans. The distribution is %s."
                .formatted(
                        delinquentLoans,
                        String.join(", ", evidence.stream()
                                .map(item -> item.label() + ": " + item.value())
                                .toList()));
        return answer("ANSWERED", request, question, text, analytics,
                analytics.definitions().dpd(), evidence);
    }

    private PortfolioAnalytics calculate(AssistantQuestion request, String segment) {
        return analyticsService.calculate(
                request.periodStart(),
                request.periodEnd(),
                request.productId(),
                segment,
                request.region(),
                request.loanStatus());
    }

    private AssistantAnswer answer(
            String status,
            AssistantQuestion request,
            String question,
            String text,
            PortfolioAnalytics analytics,
            String definition,
            List<Evidence> evidence) {
        Provenance provenance = analytics == null
                ? new Provenance(
                        SOURCE,
                        null,
                        request.periodStart(),
                        request.periodEnd(),
                        null,
                        new PortfolioAnalytics.AppliedFilters(
                                request.productId(), request.segment(), request.region(), request.loanStatus()))
                : new Provenance(
                        SOURCE,
                        definition,
                        analytics.periodStart(),
                        analytics.periodEnd(),
                        analytics.asOfDate(),
                        analytics.filters());
        return new AssistantAnswer(status, question, text, OffsetDateTime.now(), provenance, evidence);
    }

    private boolean asksForSegmentPar(String normalizedQuestion) {
        return normalizedQuestion.contains("segment")
                && normalizedQuestion.contains("par")
                && (normalizedQuestion.contains("which")
                || normalizedQuestion.contains("most")
                || normalizedQuestion.contains("contribut")
                || normalizedQuestion.contains("highest"));
    }

    private boolean asksForSummary(String normalizedQuestion) {
        return normalizedQuestion.contains("summary")
                || normalizedQuestion.contains("repayment rate")
                || normalizedQuestion.contains("portfolio performance")
                || normalizedQuestion.contains("how is the portfolio");
    }

    private boolean asksForDelinquency(String normalizedQuestion) {
        return normalizedQuestion.contains("delinquen")
                || normalizedQuestion.contains("overdue accounts")
                || normalizedQuestion.contains("dpd distribution");
    }

    private boolean containsChangeLanguage(String normalizedQuestion) {
        return normalizedQuestion.contains("increase")
                || normalizedQuestion.contains("decrease")
                || normalizedQuestion.contains("change")
                || normalizedQuestion.contains("grew")
                || normalizedQuestion.contains("rise");
    }

    private String money(BigDecimal amount, String currency) {
        return NumberFormat.getCurrencyInstance(Locale.US)
                .format(amount)
                .replace("$", currency + " ");
    }

    private String number(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private record SegmentContribution(
            String segment,
            BigDecimal atRiskPrincipal,
            BigDecimal ratioPercent,
            long qualifyingLoans) {
    }
}
