package com.pswied.core.arthaloan.demo;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MockAssistantModel {

    private static final Pattern PAR_THRESHOLD = Pattern.compile("\\bpar\\s*(1|7|30|90)\\b");
    private static final String MODEL_NAME = "ArthaLoan mock model v1";

    public Plan interpret(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        Matcher par = PAR_THRESHOLD.matcher(normalized);
        boolean hasParThreshold = par.find();
        int threshold = hasParThreshold ? Integer.parseInt(par.group(1)) : 30;

        if (containsChangeLanguage(normalized)) {
            return new Plan(Intent.HISTORICAL_CHANGE, threshold);
        }
        if (asksForCollectionWorklist(normalized)) {
            return new Plan(Intent.COLLECTION_WORKLIST, threshold);
        }
        if (asksForSegmentPar(normalized)) {
            return new Plan(Intent.SEGMENT_PAR, threshold);
        }
        if (hasParThreshold || normalized.contains("portfolio at risk")) {
            return new Plan(Intent.PAR_METRIC, threshold);
        }
        if (asksForSummary(normalized)) {
            return new Plan(Intent.PORTFOLIO_SUMMARY, threshold);
        }
        if (asksForDelinquency(normalized)) {
            return new Plan(Intent.DELINQUENCY, threshold);
        }
        return new Plan(Intent.UNSUPPORTED, threshold);
    }

    public String compose(String groundedAnswer) {
        return "Simulated AI response: " + groundedAnswer;
    }

    public String modelName() {
        return MODEL_NAME;
    }

    private boolean asksForCollectionWorklist(String question) {
        return question.contains("collection")
                || question.contains("worklist")
                || question.contains("review first")
                || question.contains("prioriti")
                || question.contains("accounts to review");
    }

    private boolean asksForSegmentPar(String question) {
        return question.contains("segment")
                && question.contains("par")
                && (question.contains("which")
                || question.contains("most")
                || question.contains("contribut")
                || question.contains("highest"));
    }

    private boolean asksForSummary(String question) {
        return question.contains("summary")
                || question.contains("repayment rate")
                || question.contains("portfolio performance")
                || question.contains("how is the portfolio");
    }

    private boolean asksForDelinquency(String question) {
        return question.contains("delinquen")
                || question.contains("overdue accounts")
                || question.contains("dpd distribution");
    }

    private boolean containsChangeLanguage(String question) {
        return question.contains("increase")
                || question.contains("decrease")
                || question.contains("change")
                || question.contains("grew")
                || question.contains("rise");
    }

    public enum Intent {
        PORTFOLIO_SUMMARY,
        PAR_METRIC,
        SEGMENT_PAR,
        DELINQUENCY,
        COLLECTION_WORKLIST,
        HISTORICAL_CHANGE,
        UNSUPPORTED
    }

    public record Plan(Intent intent, int parThreshold) {
    }
}
