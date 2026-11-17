# ArthaLoan AI — Demo Data Dictionary

The initial demo portfolio is generated in memory and contains synthetic
records only. `GET /api/demo/portfolio` returns the full generated
snapshot, including its seed and as-of date. No database or external
service is required.

## Reproducibility

- Default seed: `20261117`.
- Default as-of business date: `2026-10-31`.
- Default currency: `USD`.
- Override settings with `ARTHALOAN_DEMO_SEED` and
  `ARTHALOAN_DEMO_AS_OF_DATE`.
- The same seed and as-of date produce the same records. The seed affects
  generated principal amounts and interest rates; the account and
  repayment scenarios remain consistent.
- Identifiers are deterministic and stable within a generated dataset.

## Entities and fields

| Entity | Fields | Meaning |
|---|---|---|
| Loan product | `productId`, `name`, `repaymentFrequency` | Two demonstration products: personal loan and auto loan; both have monthly repayment frequency. |
| Customer | `customerId`, `segment`, `region` | Synthetic customer reference, assigned to one of three segments and regions. No names, contact details, or real demographic values are generated. |
| Loan account | `loanId`, `customerId`, `productId`, `principal`, `annualInterestRatePercent`, `tenorMonths`, `originationDate`, `maturityDate`, `status` | Active synthetic loan. Monetary fields use decimal values and the portfolio currency. |
| Installment | `installmentId`, `loanId`, `dueDate`, `principalDue`, `interestDue`, `feesDue`, `status` | One of twelve monthly installments. Status is `PAID`, `PARTIALLY_PAID`, `OVERDUE`, or `SCHEDULED` as of the snapshot date. `scheduledAmount` is principal, interest, and fees due. |
| Payment transaction | `paymentId`, `loanId`, `postingDate`, `amount`, `currency`, `type`, `reversesPaymentId`, `allocations` | Posted payment or signed reversal. Original payment records remain present when reversed. |
| Payment allocation | `installmentId`, `principalAmount`, `interestAmount`, `feesAmount` | Signed allocation of a transaction to an installment; reversal allocations negate the original allocation. |

## Fixture composition

The default dataset has 12 customers, 12 active loans, 2 products, 144
installments, and payment records covering six months of repayment
history. It includes multiple segments and regions; performing accounts;
accounts one or more installments overdue; a partial payment; and a
payment/reversal pair that preserves the original transaction and links
the reversal using `reversesPaymentId`.

## Limitations

This generator is a deterministic demo fixture, not a financial
simulation or a source of model-training evidence. Interest is
simplified as a flat monthly amount based on original principal and the
annual rate. The six-month behavior history is intentionally concise;
generated records must not be interpreted as real customer or lending
outcomes.
