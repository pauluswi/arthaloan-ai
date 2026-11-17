package com.pswied.core.arthaloan.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoPortfolioController {

    private final SyntheticPortfolioGenerator generator;

    public DemoPortfolioController(SyntheticPortfolioGenerator generator) {
        this.generator = generator;
    }

    @GetMapping("/portfolio")
    public SyntheticPortfolio portfolio() {
        return generator.generate();
    }
}
