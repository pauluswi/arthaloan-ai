package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.LoanRiskPrediction.RiskFactor;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentTransaction;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.TransactionType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LoanRiskPredictionService {

    private static final String MODEL_VERSION = "demo-rules-v1";
    private static final int PREDICTION_HORIZON_DAYS = 30;
    private static final int MAX_DPD_POINTS = 40;
    private static final int POINTS_PER_UNPAID_INSTALLMENT = 10;
    private static final int MAX_UNPAID_INSTALLMENT_POINTS = 30;
    private static final int RECENT_REVERSAL_POINTS = 15;
    private static final int REVERSAL_LOOKBACK_DAYS = 180;

    private final LoanInvestigationService loanInvestigationService;

    public LoanRiskPredictionService(LoanInvestigationService loanInvestigationService) {
        this.loanInvestigationService = loanInvestigationService;
    }

    public LoanRiskPrediction predict(String loanId) {
        LoanDetail loan = loanInvestigationService.loan(loanId);
        long dueInstallments = loan.installments().stream()
                .filter(installment -> !installment.dueDate().isAfter(loan.asOfDate()))
                .count();

        if (!"ACTIVE".equals(loan.loan().status()) || dueInstallments == 0) {
            return new LoanRiskPrediction(
                    loanId,
                    loan.asOfDate(),
                    PREDICTION_HORIZON_DAYS,
                    MODEL_VERSION,
                    "INELIGIBLE",
                    null,
                    "UNAVAILABLE",
                    "Deterministic demo rule score; not a calibrated probability.",
                    dueInstallments == 0
                            ? "No installments were due by the snapshot date."
                            : "Only active loans are eligible for this demo score.",
                    List.of());
        }

        List<RiskFactor> factors = new ArrayList<>();
        int score = 0;
        if (loan.dpd() > 0) {
            int points = Math.min(MAX_DPD_POINTS, loan.dpd());
            score += points;
            factors.add(new RiskFactor(
                    "Days past due",
                    "%d days".formatted(loan.dpd()),
                    points,
                    "The oldest unpaid installment is %d calendar days past due.".formatted(loan.dpd())));
        }

        List<InstallmentDetail> recentInstallments = loan.installments().stream()
                .filter(installment -> !installment.dueDate().isAfter(loan.asOfDate()))
                .sorted(Comparator.comparing(InstallmentDetail::dueDate).reversed())
                .limit(3)
                .toList();
        long unpaidInstallments = recentInstallments.stream()
                .filter(installment -> installment.remainingAmount().signum() > 0)
                .count();
        if (unpaidInstallments > 0) {
            int points = Math.toIntExact(Math.min(
                    MAX_UNPAID_INSTALLMENT_POINTS,
                    unpaidInstallments * POINTS_PER_UNPAID_INSTALLMENT));
            score += points;
            factors.add(new RiskFactor(
                    "Recent unpaid installments",
                    "%d of the last %d due installments".formatted(unpaidInstallments, recentInstallments.size()),
                    points,
                    "At least one of the three most recent due installments has a remaining balance."));
        }

        long recentReversals = loan.payments().stream()
                .filter(payment -> payment.type() == TransactionType.REVERSAL)
                .filter(payment -> !payment.postingDate().isBefore(loan.asOfDate().minusDays(REVERSAL_LOOKBACK_DAYS)))
                .count();
        if (recentReversals > 0) {
            score += RECENT_REVERSAL_POINTS;
            factors.add(new RiskFactor(
                    "Recent payment reversals",
                    "%d reversal transaction(s) in the last 180 days".formatted(recentReversals),
                    RECENT_REVERSAL_POINTS,
                    "A posted reversal reduced the net effect of a recent payment."));
        }

        factors.sort(Comparator.comparingInt(RiskFactor::points).reversed());
        return new LoanRiskPrediction(
                loanId,
                loan.asOfDate(),
                PREDICTION_HORIZON_DAYS,
                MODEL_VERSION,
                "AVAILABLE",
                score,
                riskBand(score),
                "Deterministic demo rule score; not a calibrated probability.",
                null,
                factors);
    }

    private String riskBand(int score) {
        if (score >= 50) {
            return "HIGH";
        }
        if (score >= 25) {
            return "MODERATE";
        }
        return "LOW";
    }
}
