package com.pswied.core.arthaloan.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoPortfolioController {

    private final DemoPortfolioService portfolioService;
    private final LoanInvestigationService loanInvestigationService;
    private final LoanRiskPredictionService riskPredictionService;
    private final CollectionWorklistService collectionWorklistService;
    private final CollectionActivityService collectionActivityService;
    private final PortfolioAnalyticsService analyticsService;
    private final DemoAssistantService assistantService;

    public DemoPortfolioController(
            DemoPortfolioService portfolioService,
            LoanInvestigationService loanInvestigationService,
            LoanRiskPredictionService riskPredictionService,
            CollectionWorklistService collectionWorklistService,
            CollectionActivityService collectionActivityService,
            PortfolioAnalyticsService analyticsService,
            DemoAssistantService assistantService) {
        this.portfolioService = portfolioService;
        this.loanInvestigationService = loanInvestigationService;
        this.riskPredictionService = riskPredictionService;
        this.collectionWorklistService = collectionWorklistService;
        this.collectionActivityService = collectionActivityService;
        this.analyticsService = analyticsService;
        this.assistantService = assistantService;
    }

    @GetMapping("/portfolio")
    public SyntheticPortfolio portfolio() {
        return portfolioService.portfolio();
    }

    @GetMapping("/loans")
    public List<LoanSummary> loans() {
        return loanInvestigationService.loans();
    }

    @GetMapping("/loans/{loanId}")
    public LoanDetail loan(@PathVariable String loanId) {
        return loanInvestigationService.loan(loanId);
    }

    @GetMapping("/loans/{loanId}/risk")
    public LoanRiskPrediction riskPrediction(@PathVariable String loanId) {
        return riskPredictionService.predict(loanId);
    }

    @GetMapping("/loans/{loanId}/collection-activities")
    public List<CollectionActivity> collectionActivities(@PathVariable String loanId) {
        return collectionActivityService.activities(loanId);
    }

    @PostMapping("/loans/{loanId}/collection-activities")
    public ResponseEntity<CollectionActivity> recordCollectionActivity(
            @PathVariable String loanId,
            @RequestBody CollectionActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionActivityService.record(loanId, request));
    }

    @GetMapping("/collections/worklist")
    public CollectionWorklist collectionWorklist() {
        return collectionWorklistService.worklist();
    }

    @GetMapping("/analytics")
    public PortfolioAnalytics analytics(
            @RequestParam(required = false) java.time.LocalDate periodStart,
            @RequestParam(required = false) java.time.LocalDate periodEnd,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String loanStatus) {
        return analyticsService.calculate(periodStart, periodEnd, productId, segment, region, loanStatus);
    }

    @PostMapping("/assistant/ask")
    public AssistantAnswer askAssistant(@RequestBody AssistantQuestion request) {
        return assistantService.answer(request);
    }
}
