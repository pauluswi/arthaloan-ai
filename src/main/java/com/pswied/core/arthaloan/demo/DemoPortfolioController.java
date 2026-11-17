package com.pswied.core.arthaloan.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoPortfolioController {

    private final DemoPortfolioService portfolioService;
    private final LoanInvestigationService loanInvestigationService;

    public DemoPortfolioController(
            DemoPortfolioService portfolioService,
            LoanInvestigationService loanInvestigationService) {
        this.portfolioService = portfolioService;
        this.loanInvestigationService = loanInvestigationService;
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
}
