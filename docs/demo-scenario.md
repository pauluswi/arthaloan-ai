# ArthaLoan AI — Demo Scenario

This is the initial, repeatable showcase journey. It is designed to make
the executive dashboard the entry point while showing a real analytics
path through loan investigation and collection workflow. Predictions
and AI responses are illustrative and must be clearly labeled when
provided by mocks.

## Demo objective

Show how an authorized user can move from a portfolio-level trend to an
individual loan investigation, review transparent risk and collection
signals, record a simulated outcome, and ask a grounded portfolio
question.

## Walkthrough

1. Start the locally documented demo and sign in as a seeded
   Credit/Portfolio Manager.
2. Open the **Executive Dashboard**. Review outstanding principal,
   scheduled amount, amount collected, repayment rate, PAR, and
   delinquency buckets for the selected period.
3. Filter by product, segment, region, or loan status and inspect the
    six-month repayment trend. Confirm the period, USD currency, metric
    definitions, as-of date, and synthetic-data label are visible.
4. Open **Loan Portfolio** and select a delinquent loan. Review its
   schedule, posted payment allocations, reversals, outstanding
   principal, overdue amount, and current DPD. The risk view and
   prediction are planned for a later step.
5. Open the **Collection Workbench** and locate the loan. Review the
   overdue amount, DPD, risk signal, priority factors, and policy
   version.
6. As a seeded Collection Officer, record a simulated activity and
   outcome. Verify that the activity is persisted and audited; no
   borrower is contacted and no adverse action is triggered.
7. Ask the assistant: “Which segment contributed most to the increase
   in PAR 30 during this period?” Verify the response uses the analytics
   interface and shows period, filters, metric definition, and source.
   The initial assistant may use a deterministic fixture/template; it
   must not invent numeric results.
8. Review the relevant audit history as an authorized read-only user or
   administrator.

## Initial demo conventions

- **Currency:** USD. Currency is explicit in monetary records so other
  currencies and locale-specific formatting can be supported later.
- **DPD:** Calendar days, no grace period, anchored to the oldest unpaid
  installment. A partial payment does not reset DPD while that
  installment remains unpaid.
- **Repayment rate:** Amount-based. Numerator is payment allocated to
  installments due in the selected period and posted in that period,
  net of reversals. Payments posted before the period, including
  prepayments, are excluded. Denominator is scheduled principal,
  interest, and applicable fees due in the period.
- **PAR 1/7/30/90:** Outstanding principal for loans at or above the
  respective DPD threshold divided by total outstanding principal in
  the selected portfolio, as of the reporting date.
- **Risk and language-model integrations:** Mocked behind replaceable
  interfaces initially. Mark mock outputs as illustrative. The
  underlying DPD, portfolio metrics, worklist rules, authorization,
  and audit behavior should use real application logic.
- **Data:** Synthetic, seeded, and repeatable; no real customer or bank
  data.

## Completion checks

- Re-running the demo produces the same portfolio and fixture-backed
  risk/assistant results.
- Dashboard totals reconcile with the synthetic source records using
  the conventions above; implemented metrics are served by
  `GET /api/demo/analytics`.
- The demo user roles have different server-enforced permissions.
- Loan prioritization exposes its factors and policy version.
- Collection activity and material AI interactions are auditable.
- Mock providers can be disabled or replaced without disabling
  deterministic analytics.
