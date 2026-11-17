package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.SyntheticPortfolio.Customer;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.LoanAccount;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.LoanProduct;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoanDetail(
        LoanAccount loan,
        Customer customer,
        LoanProduct product,
        String currency,
        LocalDate asOfDate,
        int dpd,
        BigDecimal overdueAmount,
        BigDecimal outstandingPrincipal,
        List<InstallmentDetail> installments,
        List<PaymentTransaction> payments) {

    public LoanDetail {
        installments = List.copyOf(installments);
        payments = List.copyOf(payments);
    }
}
