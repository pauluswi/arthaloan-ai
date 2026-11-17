package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.AssistantAnswer.Evidence;
import com.pswied.core.arthaloan.demo.AssistantAnswer.Provenance;
import com.pswied.core.arthaloan.demo.AssistantAnswer.ToolCall;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.ParMetric;
import com.pswied.core.arthaloan.demo.PortfolioAnalytics.Summary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DemoAssistantService {

    private static final String SOURCE = "Synthetic demo analytics and collection worklist tools.";

    private final PortfolioAnalyticsService analyticsService;
    private final CollectionWorklistService collectionWorklistService;
    private final MockAssistantModel mockModel;

    public DemoAssistantService(
            PortfolioAnalyticsService analyticsService,
            CollectionWorklistService collectionWorklistService,
            MockAssistantModel mockModel) {
        this.analyticsService = analyticsService;
        this.collectionWorklistService = collectionWorklistService;
        this.mockModel = mockModel;
    }

    public AssistantAnswer answer(AssistantQuestion request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required.");
        }

        String question = request.question().strip();
        if (question.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not exceed 500 characters.");
        }
        MockAssistantModel.Plan plan = mockModel.interpret(question);
        List<ToolCall> toolCalls = new ArrayList<>();
        AssistantAnswer result = switch (plan.intent()) {
            case PORTFOLIO_SUMMARY -> summaryAnswer(request, question, toolCalls);
            case PAR_METRIC -> parAnswer(request, question, plan.parThreshold(), toolCalls);
            case SEGMENT_PAR -> segmentParAnswer(request, question, plan.parThreshold(), toolCalls);
            case DELINQUENCY -> delinquencyAnswer(request, question, toolCalls);
            case COLLECTION_WORKLIST -> collectionWorklistAnswer(request, question, toolCalls);
            case HISTORICAL_CHANGE -> insufficientHistoryAnswer(request, question);
            case UNSUPPORTED -> unsupportedAnswer(request, question);
        };
        return withModelResponse(result, toolCalls);
    }

    private AssistantAnswer summaryAnswer(
            AssistantQuestion request,
            String question,
            List<ToolCall> toolCalls) {
        PortfolioAnalytics analytics = calculate(request, request.segment());
        toolCalls.add(analyticsToolCall(request, "Portfolio summary and metrics returned."));
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
        return answer(
                "ANSWERED", request, question, text,
                analyticsProvenance(analytics, analytics.definitions().repaymentRate()), evidence);
    }

    private AssistantAnswer parAnswer(
            AssistantQuestion request,
            String question,
            int threshold,
            List<ToolCall> toolCalls) {
        PortfolioAnalytics analytics = calculate(request, request.segment());
        toolCalls.add(analyticsToolCall(request, "PAR metrics returned for thresholds 1, 7, 30 and 90."));
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
                analyticsProvenance(analytics, analytics.definitions().portfolioAtRisk()),
                List.of(
                        new Evidence("PAR " + threshold, ratio),
                        new Evidence("At-risk principal", money(metric.atRiskPrincipal(), analytics.currency())),
                        new Evidence("Portfolio principal", money(metric.portfolioPrincipal(), analytics.currency())),
                        new Evidence("Qualifying loans", Long.toString(metric.qualifyingLoans()))));
    }

    private AssistantAnswer segmentParAnswer(
            AssistantQuestion request,
            String question,
            int threshold,
            List<ToolCall> toolCalls) {
        PortfolioAnalytics allSegments = calculate(request, null);
        toolCalls.add(analyticsToolCall(request, "Available segment options returned."));

        List<PortfolioAnalytics> segments = allSegments.filterOptions().segments().stream()
                .map(segment -> {
                    PortfolioAnalytics segmentAnalytics = analyticsService.calculate(
                        request.periodStart(),
                        request.periodEnd(),
                        request.productId(),
                        segment,
                        request.region(),
                        request.loanStatus());
                    toolCalls.add(new ToolCall(
                            "portfolioAnalytics",
                            analyticsArguments(
                                    request.periodStart(),
                                    request.periodEnd(),
                                    request.productId(),
                                    segment,
                                    request.region(),
                                    request.loanStatus()),
                            "PAR metrics returned for segment " + segment + "."));
                    return segmentAnalytics;
                })
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
                    analyticsProvenance(allSegments, allSegments.definitions().portfolioAtRisk()),
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
                analyticsProvenance(allSegments, allSegments.definitions().portfolioAtRisk()),
                evidence);
    }

    private AssistantAnswer delinquencyAnswer(
            AssistantQuestion request,
            String question,
            List<ToolCall> toolCalls) {
        PortfolioAnalytics analytics = calculate(request, request.segment());
        toolCalls.add(analyticsToolCall(request, "Delinquency distribution returned."));
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
        return answer(
                "ANSWERED", request, question, text,
                analyticsProvenance(analytics, analytics.definitions().dpd()), evidence);
    }

    private AssistantAnswer collectionWorklistAnswer(
            AssistantQuestion request,
            String question,
            List<ToolCall> toolCalls) {
        CollectionWorklist worklist = collectionWorklistService.worklist();
        toolCalls.add(new ToolCall(
                "collectionWorklist",
                Map.of(),
                "%d overdue accounts sorted by the %s policy."
                        .formatted(worklist.totalAccounts(), worklist.policyVersion())));
        Provenance provenance = new Provenance(
                SOURCE + " Policy " + worklist.policyVersion() + ".",
                "Priority combines DPD, overdue exposure, and the illustrative demo risk score.",
                null,
                null,
                worklist.asOfDate(),
                new PortfolioAnalytics.AppliedFilters(null, null, null, null));
        if (worklist.items().isEmpty()) {
            return answer(
                    "INSUFFICIENT_DATA",
                    request,
                    question,
                    "There are no overdue accounts in this synthetic worklist snapshot.",
                    provenance,
                    List.of());
        }

        CollectionWorklist.WorklistItem first = worklist.items().getFirst();
        List<Evidence> evidence = new ArrayList<>(List.of(
                new Evidence("First account to review", first.loanId()),
                new Evidence("Priority", first.priorityBand() + " · " + first.priorityScore()),
                new Evidence("Days past due", Integer.toString(first.dpd())),
                new Evidence("Overdue amount", money(first.overdueAmount(), worklist.currency()))));
        first.factors().forEach(factor ->
                evidence.add(new Evidence(factor.name(), factor.points() + " points — " + factor.explanation())));

        return answer(
                "ANSWERED",
                request,
                question,
                "For a human review, start with synthetic account %s: it has %s priority (%d points), %d days "
                        .formatted(first.loanId(), first.priorityBand(), first.priorityScore(), first.dpd())
                        + "past due and "
                        + money(first.overdueAmount(), worklist.currency())
                        + " overdue. Review its payment history and the listed priority factors. This is advisory "
                        + "demo output; no borrower is contacted and no action is taken.",
                provenance,
                evidence);
    }

    private AssistantAnswer insufficientHistoryAnswer(AssistantQuestion request, String question) {
        return answer(
                "INSUFFICIENT_DATA",
                request,
                question,
                "This demo has only one as-of snapshot, so I cannot establish historical increases or decreases. "
                        + "I can report current PAR and repayment metrics.",
                new Provenance(
                        SOURCE,
                        null,
                        request.periodStart(),
                        request.periodEnd(),
                        null,
                        new PortfolioAnalytics.AppliedFilters(
                                request.productId(), request.segment(), request.region(), request.loanStatus())),
                List.of());
    }

    private AssistantAnswer unsupportedAnswer(AssistantQuestion request, String question) {
        return answer(
                "UNSUPPORTED",
                request,
                question,
                "I can summarize the portfolio, explain PAR or delinquency, compare current segment PAR, or suggest "
                        + "the first account for human collection review. I cannot approve borrowers or take actions.",
                new Provenance(
                        SOURCE,
                        null,
                        request.periodStart(),
                        request.periodEnd(),
                        null,
                        new PortfolioAnalytics.AppliedFilters(
                                request.productId(), request.segment(), request.region(), request.loanStatus())),
                List.of());
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

    private Provenance analyticsProvenance(PortfolioAnalytics analytics, String definition) {
        return new Provenance(
                SOURCE,
                definition,
                analytics.periodStart(),
                analytics.periodEnd(),
                analytics.asOfDate(),
                analytics.filters());
    }

    private AssistantAnswer answer(
            String status,
            AssistantQuestion request,
            String question,
            String text,
            Provenance provenance,
            List<Evidence> evidence) {
        return new AssistantAnswer(
                status,
                question,
                text,
                "SIMULATED",
                mockModel.modelName(),
                OffsetDateTime.now(),
                provenance,
                List.of(),
                evidence);
    }

    private AssistantAnswer withModelResponse(AssistantAnswer result, List<ToolCall> toolCalls) {
        return new AssistantAnswer(
                result.status(),
                result.question(),
                mockModel.compose(result.answer()),
                "SIMULATED",
                mockModel.modelName(),
                OffsetDateTime.now(),
                result.provenance(),
                toolCalls,
                result.evidence());
    }

    private ToolCall analyticsToolCall(AssistantQuestion request, String resultSummary) {
        return new ToolCall(
                "portfolioAnalytics",
                analyticsArguments(
                        request.periodStart(),
                        request.periodEnd(),
                        request.productId(),
                        request.segment(),
                        request.region(),
                        request.loanStatus()),
                resultSummary);
    }

    private Map<String, String> analyticsArguments(
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd,
            String productId,
            String segment,
            String region,
            String loanStatus) {
        Map<String, String> arguments = new LinkedHashMap<>();
        if (periodStart != null) {
            arguments.put("periodStart", periodStart.toString());
        }
        if (periodEnd != null) {
            arguments.put("periodEnd", periodEnd.toString());
        }
        if (productId != null) {
            arguments.put("productId", productId);
        }
        if (segment != null) {
            arguments.put("segment", segment);
        }
        if (region != null) {
            arguments.put("region", region);
        }
        if (loanStatus != null) {
            arguments.put("loanStatus", loanStatus);
        }
        return arguments;
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
