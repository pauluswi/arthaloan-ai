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

## Loan investigation calculations

The loan portfolio API (`GET /api/demo/loans`) and loan detail API
(`GET /api/demo/loans/{loanId}`) calculate balances from allocations
posted on or before the snapshot as-of date. Signed reversal allocations
reduce the original payment's effect without deleting its record.

- An installment is paid when its remaining scheduled amount is zero;
  it is partially paid when a positive allocation leaves a positive
  balance; future installments are scheduled.
- DPD is the calendar-day difference between the as-of date and the
  oldest installment with a positive remaining scheduled balance and a
  due date on or before the as-of date. An installment due on the as-of
  date has zero DPD.
- Overdue amount is the remaining scheduled principal, interest, and
  fees for installments with DPD greater than zero.
- Outstanding principal is scheduled principal across the full loan
  tenor less net posted principal allocations, bounded below by zero.

## Portfolio analytics

`GET /api/demo/analytics` returns the fixed snapshot's reporting period,
as-of date, currency, applied filters, metric definitions, and filter
options alongside the metrics. The reporting period defaults to the
as-of month's first day through the as-of date. Filters support product,
customer segment, region, and loan status.

- Scheduled amount is principal, interest, and fees due in the reporting
  period for the selected loans.
- Amount collected is the signed net of allocations posted during the
  reporting period to installments due during that same period.
- Repayment rate is amount collected divided by scheduled amount. A zero
  scheduled amount is unavailable (`null`), not 0%.
- Six-month trend applies the same due-period and posting-period rules to
  each displayed month.
- Delinquency buckets count loans by snapshot DPD; their balance amounts
  use outstanding principal.
- PAR 1/7/30/90 is outstanding principal for loans meeting the inclusive
  DPD threshold divided by selected-portfolio outstanding principal.
  A zero portfolio balance makes the ratio unavailable.

## Limitations

This generator is a deterministic demo fixture, not a financial
simulation or a source of model-training evidence. Interest is
simplified as a flat monthly amount based on original principal and the
annual rate. The six-month behavior history is intentionally concise;
generated records must not be interpreted as real customer or lending
outcomes.
