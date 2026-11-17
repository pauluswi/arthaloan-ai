package com.pswied.core.arthaloan.demo;

import org.springframework.stereotype.Service;

@Service
public class DemoPortfolioService {

    private final SyntheticPortfolio portfolio;

    public DemoPortfolioService(SyntheticPortfolioGenerator generator) {
        this.portfolio = generator.generate();
    }

    public SyntheticPortfolio portfolio() {
        return portfolio;
    }
}
