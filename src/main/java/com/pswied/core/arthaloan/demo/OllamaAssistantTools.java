package com.pswied.core.arthaloan.demo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OllamaAssistantTools {

    private final PortfolioAnalyticsService analyticsService;
    private final CollectionWorklistService collectionWorklistService;

    public OllamaAssistantTools(
            PortfolioAnalyticsService analyticsService,
            CollectionWorklistService collectionWorklistService) {
        this.analyticsService = analyticsService;
        this.collectionWorklistService = collectionWorklistService;
    }

    @Tool(description = "Get synthetic portfolio summary, repayment rate, delinquency buckets, and PAR metrics.")
    public PortfolioAnalytics portfolioAnalytics() {
        return analyticsService.calculate(null, null, null, null, null, null);
    }

    @Tool(description = "Get the ranked synthetic overdue collection worklist and its explainable priority factors. "
            + "For human review only; it does not contact borrowers or take actions.")
    public CollectionWorklist collectionWorklist() {
        return collectionWorklistService.worklist();
    }
}
