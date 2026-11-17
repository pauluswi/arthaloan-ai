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

## Illustrative loan risk score

`GET /api/demo/loans/{loanId}/risk` returns a deterministic rule score
for an active loan with at least one installment due by the snapshot
date. It includes the snapshot date, 30-day horizon, demo rule version,
eligibility status, score band, and the contributing factors. It is not
a trained model, calibrated probability, or a recommendation to take
action.

The score adds up to 100 points from observable snapshot evidence:

| Factor | Points |
|---|---|
| Current DPD | One point per day, capped at 40 |
| Unpaid balance in the three most recent due installments | 10 points per installment, capped at 30 |
| Payment reversal posted within the prior 180 days | 15 points when present |

Bands are **LOW** (0–24), **MODERATE** (25–49), and **HIGH** (50–100).
Loans that are not active or have no due installment are ineligible and
return a null score rather than a fabricated result. Factors are
displayed with their observed values and point contributions. These
provisional demo rules are only for showcasing explainability; they
have no measured predictive performance and must not be used for lending
or collection decisions.

## Collection worklist

`GET /api/demo/collections/worklist` returns active loans with positive
DPD and overdue amount, ranked by a deterministic `demo-collection-v1`
policy. Each item includes the 30-day demo risk signal, component points,
the score band, and the snapshot balances. The endpoint also returns the
as-of date and total overdue amount.

| Priority component | Points |
|---|---|
| Delinquency | One point per DPD, capped at 40 |
| Overdue exposure | 5 points below $500; 10 for $500–$999.99; 15 for $1,000–$1,999.99; 20 from $2,000 |
| 30-day risk signal | 40% of the illustrative risk score, rounded to the nearest point |

Priority bands are **HIGH** (60+), **PRIORITY** (35–59), and **STANDARD**
(below 35). Ties are ordered by DPD descending, overdue amount descending,
then loan ID. The displayed review suggestion is advisory. This worklist
does not assign real collection officers, record activities, or initiate
borrower contact.

## Simulated collection activity

`GET /api/demo/loans/{loanId}/collection-activities` lists activities
recorded during the current server run.
`POST /api/demo/loans/{loanId}/collection-activities` records an account
review, simulated contact attempt, or follow-up with an outcome. Promise
to pay requires a positive promised amount and follow-up date; follow-up
required also requires a date. Notes are limited to 500 characters.

Each record includes a generated activity ID, timestamp, fixed demo
actor (`demo-user`), outcome, and supplied promise/follow-up details.
Records are held in application memory and cleared when the server
restarts; this is not durable storage or a production audit log. Entries
are accepted only for overdue loans. Simulated contact attempts do not
send messages or contact borrowers. Use synthetic notes only.

## Deterministic analytics assistant

`POST /api/demo/assistant/ask` accepts a question and optional analytics
period and filters. This showcase uses a deterministic, allowlisted
question matcher and the existing portfolio analytics service; it does not
call an LLM or expose direct database, account-detail, or action tools.
Supported questions cover a portfolio summary/repayment rate,
delinquency distribution, PAR 1/7/30/90, and the segment with the largest
current at-risk principal for a PAR threshold. Segment comparisons apply
the product, region, and loan-status filters, but compare all segments
regardless of a selected segment filter.

Responses include a status (`ANSWERED`, `UNSUPPORTED`, or
`INSUFFICIENT_DATA`), a concise answer, evidence values, metric definition,
reporting period, snapshot date, applied filters, and source. PAR is a
single snapshot metric; the assistant explicitly declines segment
increase/decrease questions because this fixture has no historical PAR
snapshots. Unsupported prompts do not produce financial claims. The
assistant is not authenticated, does not persist interaction history, and
must not be treated as fulfilling production authorization, audit, or
generative-AI requirements.

## Limitations

This generator is a deterministic demo fixture, not a financial
simulation or a source of model-training evidence. Interest is
simplified as a flat monthly amount based on original principal and the
annual rate. The six-month behavior history is intentionally concise;
generated records must not be interpreted as real customer or lending
outcomes.
