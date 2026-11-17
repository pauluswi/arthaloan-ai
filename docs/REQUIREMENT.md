# ArthaLoan AI --- Requirements Specification

**Product:** ArthaLoan AI\
**Positioning:** End-to-end AI-powered loan intelligence platform\
**Architecture:** Modular monolith, designed for future service
extraction\
**AI approach:** Hybrid Machine Learning + Explainable AI + Generative
AI

------------------------------------------------------------------------

## 1. Executive Summary

ArthaLoan AI is a portfolio-grade banking technology showcase that
demonstrates how a financial institution can use analytics, machine
learning, and generative AI across the loan lifecycle, with an initial
emphasis on repayment performance, delinquency prediction, and
collection intelligence.

The platform will ingest simulated loan, customer, repayment, and
collection data; calculate portfolio and operational metrics; identify
repayment risk; explain model predictions; and provide grounded
natural-language insights to authorized users.

The project will be built as a modular monolith first. Modules will have
explicit boundaries and contracts so that selected capabilities can be
extracted into independent services if scale, deployment independence,
or team ownership later justify doing so.

ArthaLoan AI is a decision-support platform. It must not autonomously
approve or reject credit, change loan terms, contact borrowers, or
initiate adverse collection actions.

## 2. Product Vision and Goals

### 2.1 Vision

Help lending teams understand loan portfolio performance earlier,
identify emerging repayment risks, and make better-informed collection
and portfolio decisions through trustworthy analytics and AI.

### 2.2 Business goals

1.  Provide a consolidated view of loan portfolio health and repayment
    performance.
2.  Detect borrowers and segments showing elevated risk of future
    delinquency.
3.  Explain the main factors contributing to a risk prediction.
4.  Help collection teams prioritize accounts using transparent,
    configurable rules and model outputs.
5.  Enable managers to ask questions about portfolio performance in
    natural language and receive evidence-grounded answers.
6.  Demonstrate an enterprise-minded architecture, responsible AI
    practices, and operational observability.

### 2.3 Success measures

The project should report measurable outcomes rather than claim
unverified business impact. Candidate measures include:

-   Repayment rate and collection rate by period and segment.
-   Portfolio at Risk (PAR) at configurable DPD thresholds.
-   Delinquency roll rates and cure rates.
-   Prediction precision, recall, PR-AUC, and calibration on a
    time-based holdout set.
-   Percentage of AI-generated answers with traceable supporting
    metrics.
-   API latency, error rate, batch-processing duration, and system
    availability in the demo environment.

Targets will be established after the baseline dataset and model
evaluation are available. No performance or financial improvement should
be claimed without measured evidence.

## 3. Scope

### 3.1 In scope

**Loan lifecycle visibility** - Loan application and origination records
as source context. - Loan account and repayment schedule management for
demonstration purposes. - Disbursement, installment, payment, reversal,
and adjustment records. - Delinquency tracking and collection activity
records. - Portfolio and customer-level analytics.

**Risk intelligence** - Repayment behavior features. - Delinquency
prediction for a defined future horizon, initially 30 days. - Risk
segmentation and configurable risk bands. - Explainability for
individual predictions and portfolio-level drivers. - Model evaluation
and version tracking.

**Collection intelligence** - Worklist prioritization based on
configurable policy rules and risk signals. - Suggested next-best
collection action for human review. - Tracking of collection attempts
and outcomes in the simulated environment. - Monitoring of cure and
roll-rate outcomes.

**Generative AI** - Natural-language questions about approved portfolio
metrics. - Retrieval of approved product, policy, and metric
definitions. - Tool-based access to authorized analytics functions. -
Responses with metric definitions, reporting periods, filters, and
source references. - Clear handling of unsupported questions and
insufficient data.

**Platform** - Role-based access control. - Audit logging for material
user and AI actions. - Synthetic data generation and repeatable demo
datasets. - API documentation, automated tests, containerized
deployment, and monitoring.

### 3.2 Out of scope for the initial release

-   Production integration with a real core banking system, loan
    management system, credit bureau, payment switch, or collection
    vendor.
-   Use of real customer personal data or confidential bank data.
-   Automated credit approval, rejection, pricing, limit changes,
    restructuring, or write-off decisions.
-   Autonomous borrower communications, payment collection, or legal
    escalation.
-   Real-time streaming from production banking channels.
-   A fully autonomous AI agent with unrestricted database or system
    access.
-   Regulatory certification or a claim of compliance with any specific
    jurisdiction.
-   Training a foundation model from scratch.

### 3.3 Future expansion candidates

-   External and internal source-system adapters.
-   Near-real-time event ingestion.
-   Champion/challenger model deployment and automated drift workflows.
-   Scenario simulation and portfolio stress testing.
-   Additional products such as SME, mortgage, vehicle, and revolving
    credit.
-   Multi-tenant deployment and institution-specific configuration.

## 4. Users and Roles

  -----------------------------------------------------------------------
  Role                    Main responsibilities   Typical capabilities
  ----------------------- ----------------------- -----------------------
  System Administrator    Configure users, roles, User and role
                          and platform settings   administration,
                                                  configuration

  Risk Analyst            Monitor risk and        Portfolio analytics,
                          investigate model       risk cohorts,
                          outputs                 explanations

  Collection Officer      Work assigned accounts  Prioritized worklist,
                          and record outcomes     account detail,
                                                  activity logging

  Collection Manager      Manage collection       Team dashboards,
                          performance and         worklist oversight,
                          priorities              strategy review

  Credit / Portfolio      Review portfolio        Portfolio dashboards,
  Manager                 quality and trends      segment analysis, AI
                                                  assistant

  Auditor / Read-only     Review evidence and     Read-only reports,
  User                    activity                prediction and audit
                                                  history
  -----------------------------------------------------------------------

Authorization must be enforced on the server for every protected
operation. Hiding a UI control is not sufficient authorization.

## 5. Business Requirements

  -----------------------------------------------------------------------
  ID                      Requirement             Priority
  ----------------------- ----------------------- -----------------------
  BR-001                  The platform shall      Must
                          provide a consolidated
                          view of loan portfolio
                          quality and repayment
                          performance.

  BR-002                  The platform shall      Must
                          identify delinquency
                          trends by product,
                          segment, period, and
                          DPD bucket.

  BR-003                  The platform shall help Must
                          users identify accounts
                          with elevated future
                          repayment risk.

  BR-004                  The platform shall      Must
                          provide understandable
                          reasons and evidence
                          for risk predictions.

  BR-005                  The platform shall help Must
                          collection managers
                          prioritize accounts
                          using configurable
                          policies and risk
                          signals.

  BR-006                  The platform shall      Should
                          provide
                          natural-language access
                          to approved portfolio
                          analytics.

  BR-007                  The platform shall      Must
                          preserve traceability
                          for important user
                          actions, model
                          predictions, and
                          AI-assisted
                          recommendations.

  BR-008                  The platform shall      Must
                          support repeatable
                          demonstrations using
                          documented synthetic
                          datasets.

  BR-009                  The platform shall      Should
                          support future
                          integration with
                          external banking
                          systems without
                          coupling the domain
                          model to a specific
                          vendor.
  -----------------------------------------------------------------------

## 6. Functional Requirements

### 6.1 Identity, access, and administration

-   **FR-001:** Users shall authenticate before accessing protected
    functions.
-   **FR-002:** Administrators shall assign users one or more roles.
-   **FR-003:** The backend shall enforce role-based permissions for
    APIs and business operations.
-   **FR-004:** The platform shall record authentication events and
    security-relevant administrative changes.
-   **FR-005:** Demo users and credentials shall be documented
    separately from production-grade secrets; default credentials must
    not be used in a public deployment.

### 6.2 Loan and repayment data

-   **FR-010:** The platform shall maintain loan account records with a
    unique loan identifier, customer reference, product, principal,
    interest terms, tenor, origination date, maturity date, and status.
-   **FR-011:** The platform shall maintain an installment schedule
    including due date, principal due, interest due, fees where
    applicable, and installment status.
-   **FR-012:** The platform shall record payment transactions with
    transaction identifier, loan identifier, posting date, value date
    where available, amount, allocation, status, and source reference.
-   **FR-013:** The platform shall support payment reversals and
    adjustments without deleting the original transaction record.
-   **FR-014:** The platform shall calculate days past due (DPD) using a
    documented and consistently applied rule.
-   **FR-015:** The platform shall expose loan, schedule, repayment, and
    delinquency information through documented APIs.
-   **FR-016:** The platform shall validate imported records and report
    rejected or quarantined rows with actionable error reasons.

### 6.3 Portfolio analytics

-   **FR-020:** The dashboard shall display total active loans,
    outstanding principal, scheduled amount, amount collected, and
    repayment rate for a selected reporting period.
-   **FR-021:** The dashboard shall display delinquency distribution by
    configurable DPD buckets, including current, 1--30, 31--60, 61--90,
    and over 90 DPD.
-   **FR-022:** The platform shall calculate PAR 1, PAR 7, PAR 30, and
    PAR 90 using documented numerator and denominator definitions.
-   **FR-023:** Users shall filter analytics by reporting period,
    product, branch or region, customer segment, and loan status where
    data is available.
-   **FR-024:** Users shall view repayment trends, delinquency trends,
    roll rates, and cure rates over time.
-   **FR-025:** Analytics shall display the reporting period, last
    refresh time, currency, metric definition, and relevant filters.
-   **FR-026:** The platform shall distinguish zero values from missing
    or unavailable data.
-   **FR-027:** Aggregated metrics shall be calculated by deterministic
    backend queries or validated analytical services, not generated by
    an LLM.

### 6.4 Risk prediction and explainability

-   **FR-030:** The platform shall generate a repayment-risk prediction
    for eligible loans or customers using a documented prediction
    horizon, initially 30 days.
-   **FR-031:** Each prediction shall include a probability or score,
    risk band, prediction timestamp, model version, prediction horizon,
    and status.
-   **FR-032:** Risk bands shall be configurable and their thresholds
    documented. Initial thresholds may be provisional until calibration
    is completed.
-   **FR-033:** The platform shall display the leading contributing
    factors for an individual prediction.
-   **FR-034:** Users shall be able to inspect prediction history and
    compare predictions across model versions where available.
-   **FR-035:** The platform shall allow authorized users to flag a
    prediction as useful, incorrect, or requiring review; feedback shall
    not silently alter model parameters.
-   **FR-036:** The platform shall record the actual outcome after the
    prediction horizon so model performance can be evaluated.
-   **FR-037:** The system shall identify ineligible or
    insufficient-data cases and avoid presenting fabricated scores.

### 6.5 Collection intelligence

-   **FR-040:** The platform shall generate a collection worklist based
    on overdue status, exposure, risk signals, and configurable policy
    rules.
-   **FR-041:** Worklist prioritization shall expose the factors and
    rule version used to determine priority.
-   **FR-042:** The platform shall show loan balance, overdue amount,
    DPD, recent payment behavior, risk prediction, and relevant
    contact/activity history subject to authorization.
-   **FR-043:** Authorized users shall record collection attempts,
    outcomes, promises to pay, and follow-up dates in the simulated
    environment.
-   **FR-044:** The platform may suggest a next-best action, but a human
    user must review and decide whether to act.
-   **FR-045:** The platform shall report worklist status and collection
    outcomes by team, period, and segment where data is available.
-   **FR-046:** Collection policies and prioritization rules shall be
    configurable and versioned.
-   **FR-047:** The platform shall not automatically contact borrowers
    or initiate legal, adverse, or coercive actions.

### 6.6 Generative AI assistant

-   **FR-050:** Authorized users shall ask natural-language questions
    about portfolio metrics and approved loan information.
-   **FR-051:** The assistant shall use a controlled set of backend
    tools for metric queries and account-level lookups.
-   **FR-052:** The assistant shall retrieve approved definitions and
    documentation for metrics, products, and collection policies when
    needed.
-   **FR-053:** Answers containing quantitative claims shall identify
    the reporting period, applied filters, metric definition, and source
    or tool result.
-   **FR-054:** The assistant shall state when data is unavailable,
    ambiguous, stale, or insufficient to answer.
-   **FR-055:** The assistant shall not invent records, financial
    values, model scores, policy clauses, or citations.
-   **FR-056:** The assistant shall respect the requesting user's
    permissions and must not reveal data outside that user's
    authorization scope.
-   **FR-057:** The assistant shall distinguish observed facts, model
    estimates, and recommendations.
-   **FR-058:** The assistant shall not execute payments, change loan
    terms, approve or reject applications, or initiate collection
    actions.
-   **FR-059:** AI prompts, tool calls, response metadata, and feedback
    shall be logged according to the privacy and retention policy, with
    sensitive values minimized or redacted.

### 6.7 Reporting and audit

-   **FR-060:** Users with permission shall export approved portfolio
    reports in CSV format.
-   **FR-061:** The platform shall maintain audit records for sensitive
    data access, administrative changes, prediction generation, and
    AI-assisted operations.
-   **FR-062:** Audit records shall include actor, action, timestamp,
    relevant object reference, outcome, and correlation identifier.
-   **FR-063:** Audit records shall be protected against ordinary
    application-user modification.
-   **FR-064:** Reports shall display whether data is synthetic, the
    reporting period, generation time, and applicable caveats.

## 7. AI and Machine Learning Requirements

### 7.1 Predictive ML

-   **AI-001:** The initial predictive task shall be defined as
    predicting whether an eligible loan will enter a specified
    delinquency state within the next 30 days.
-   **AI-002:** The target label, observation window, performance
    window, and treatment of partial or censored outcomes shall be
    documented before training.
-   **AI-003:** Candidate models may include a logistic-regression
    baseline and gradient-boosted trees such as XGBoost or LightGBM.
-   **AI-004:** Features shall be computed using only information
    available at the prediction timestamp to prevent target leakage.
-   **AI-005:** Training, validation, and test datasets shall be split
    chronologically where feasible. Random splitting alone is
    insufficient for the primary time-based evaluation.
-   **AI-006:** Evaluation shall include precision, recall, F1 where
    useful, PR-AUC, ROC-AUC as a secondary metric, confusion matrix, and
    probability calibration.
-   **AI-007:** Evaluation shall consider class imbalance and the
    operational cost of false positives and false negatives.
-   **AI-008:** The model shall be compared with a simple baseline
    before being presented as useful.
-   **AI-009:** The model shall output a probability only if the model
    is appropriately calibrated; otherwise the output must be clearly
    labeled as a score.
-   **AI-010:** Model artifacts, feature definitions, training data
    version, code version, parameters, and evaluation results shall be
    versioned.

### 7.2 Explainable AI

-   **AI-020:** SHAP or a comparable explainability method shall provide
    global and local feature contribution analysis where technically
    appropriate.
-   **AI-021:** Explanations shall describe associations contributing to
    a model output, not assert that a feature caused a borrower's
    behavior.
-   **AI-022:** The UI shall show the model version and prediction
    timestamp alongside explanations.
-   **AI-023:** Explanations shall be checked for stability and
    plausibility; misleading or unavailable explanations shall be
    labeled accordingly.
-   **AI-024:** Sensitive or protected attributes shall not be used as
    model features by default. Any use in fairness testing must be
    separately governed and access-controlled.

### 7.3 Generative AI and retrieval

-   **AI-030:** The LLM shall use tool calling for live numeric
    analytics rather than relying on model memory for current portfolio
    values.
-   **AI-031:** RAG shall be limited to approved knowledge sources such
    as metric definitions, product documentation, and collection policy
    documents created for the demo.
-   **AI-032:** Retrieved content shall be treated as data, not as
    instructions that can override system policy or access controls.
-   **AI-033:** The assistant shall use structured tool outputs and a
    defined response schema for quantitative answers.
-   **AI-034:** The assistant shall provide a concise explanation of how
    a result was derived and identify the underlying metric or query
    where feasible.
-   **AI-035:** The system shall defend against prompt injection,
    unauthorized tool use, data exfiltration, and cross-user retrieval
    leakage.
-   **AI-036:** LLM output shall be treated as untrusted until
    validated; the backend shall validate tool arguments and enforce
    permissions independently.
-   **AI-037:** The application shall support a configurable model
    provider and avoid hard-coding provider-specific assumptions into
    core domain modules.

### 7.4 AI governance and monitoring

-   **AI-040:** Each model shall have a model card describing purpose,
    intended use, prohibited use, data, limitations, evaluation, and
    owner.
-   **AI-041:** The platform shall monitor prediction distributions,
    input feature drift, outcome performance, and missing-feature rates
    when sufficient data is available.
-   **AI-042:** The platform shall support a manual model-disable or
    fallback-to-rules mechanism.
-   **AI-043:** Users shall be informed that predictions are estimates
    and require human judgment.
-   **AI-044:** AI performance and fairness results shall be reported
    with limitations; synthetic-data results must not be presented as
    evidence of real-world production performance.

## 8. Data Requirements

### 8.1 Core data entities

  -----------------------------------------------------------------------
  Entity                  Description             Key fields
  ----------------------- ----------------------- -----------------------
  Customer                Synthetic borrower      customer_id, segment,
                          reference               region, permitted
                                                  demographic fields

  Loan Account            Loan facility           loan_id, customer_id,
                                                  product_id, principal,
                                                  rate, tenor, dates,
                                                  status

  Loan Product            Product configuration   product_id, name,
                                                  repayment frequency,
                                                  policy parameters

  Repayment Schedule      Contractual             schedule_id, loan_id,
                          installments            due_date,
                                                  principal_due,
                                                  interest_due, fees_due

  Payment Transaction     Payment and adjustment  payment_id, loan_id,
                          ledger                  posting_date, amount,
                                                  status, allocation

  Delinquency Snapshot    Point-in-time           loan_id, snapshot_date,
                          delinquency state       dpd, overdue_amount

  Collection Activity     Collection interaction  activity_id, loan_id,
                          record                  activity_date, channel,
                                                  outcome,
                                                  next_action_date

  Risk Prediction         Model output            prediction_id, loan_id,
                                                  score, band, horizon,
                                                  model_version

  Model Version           Model metadata          model_version,
                                                  artifact_reference,
                                                  feature_version,
                                                  evaluation

  Audit Event             Security and business   event_id, actor,
                          audit                   action, timestamp,
                                                  object_reference

  AI Interaction          AI request and response interaction_id,
                          metadata                user_id, tool_calls,
                                                  timestamp, outcome
  -----------------------------------------------------------------------

### 8.2 Data quality and integrity

-   **DR-001:** Every business entity shall have a stable unique
    identifier.
-   **DR-002:** Monetary values shall use fixed-precision decimal types,
    not binary floating-point types.
-   **DR-003:** Currency shall be explicit on monetary records and
    reports.
-   **DR-004:** Dates and timestamps shall have documented timezone and
    business-date semantics.
-   **DR-005:** Payment reversals and corrections shall preserve
    transaction history.
-   **DR-006:** Duplicate ingestion shall be detected using stable
    source identifiers or idempotency keys.
-   **DR-007:** Referential integrity shall be enforced for loan,
    customer, schedule, payment, and collection records.
-   **DR-008:** Data imports shall produce reconciliation counts and
    validation summaries.
-   **DR-009:** Missing, invalid, and late-arriving data shall be
    visible and handled through documented rules.
-   **DR-010:** Analytical snapshots shall be reproducible for a given
    dataset version and as-of date.

### 8.3 Synthetic data

-   All initial datasets shall be synthetic and clearly labeled.
-   Synthetic data shall include realistic distributions across
    products, loan sizes, tenors, repayment behavior, and delinquency
    states.
-   The dataset shall include both performing and delinquent accounts
    and enough positive outcomes to evaluate an imbalanced
    classification problem.
-   Synthetic generation logic and random seeds shall be versioned for
    reproducibility.
-   Generated records shall not imitate identifiable real customers or
    contain real bank data.
-   The project shall document limitations caused by synthetic data,
    including the possibility that model performance will not generalize
    to real lending portfolios.

### 8.4 Metric definitions

Metric definitions shall be documented and implemented consistently. At
minimum:

-   **Repayment rate:** Define the numerator and denominator explicitly,
    for example amount collected against amount contractually due for
    the selected period. Treatment of prepayments, partial payments,
    reversals, fees, and rescheduled installments must be specified.
-   **DPD:** Number of calendar days between the applicable unpaid due
    date and the as-of business date, following the configured
    allocation and grace-period rules.
-   **PAR N:** Outstanding principal balance of loans with DPD greater
    than or equal to the defined threshold, divided by total outstanding
    principal in the selected portfolio, as of a specified date. The
    project must document whether the threshold is `DPD >= N` or another
    convention.
-   **Roll rate:** Share of accounts or balance moving from one
    delinquency bucket to another between defined observation dates.
-   **Cure rate:** Share of delinquent accounts or balance returning to
    the defined current state during the observation period.

Every metric must state whether it is account-count based or balance
based.

## 9. Non-Functional Requirements

  -----------------------------------------------------------------------
  ID                      Area                    Requirement
  ----------------------- ----------------------- -----------------------
  NFR-001                 Architecture            The application shall
                                                  be a modular monolith
                                                  with explicit module
                                                  boundaries, dependency
                                                  rules, and documented
                                                  interfaces.

  NFR-002                 Maintainability         Business rules shall
                                                  reside in
                                                  domain/application
                                                  modules, not in
                                                  controllers or UI
                                                  components.

  NFR-003                 Performance             Initial demo target:
                                                  common dashboard and
                                                  read APIs should
                                                  achieve p95 response
                                                  time below 2 seconds
                                                  under a documented
                                                  representative test
                                                  load, excluding
                                                  external LLM latency.

  NFR-004                 Performance             Batch prediction and
                                                  analytics jobs shall
                                                  report duration,
                                                  throughput, and failure
                                                  counts.

  NFR-005                 Availability            The local/demo
                                                  deployment shall
                                                  support health checks,
                                                  graceful error
                                                  handling, and
                                                  documented restart
                                                  procedures.

  NFR-006                 Scalability             Stateless application
                                                  components and module
                                                  boundaries shall allow
                                                  future horizontal
                                                  scaling or service
                                                  extraction.

  NFR-007                 Security                All protected APIs
                                                  shall require
                                                  authentication and
                                                  server-side
                                                  authorization.

  NFR-008                 Security                Secrets shall be
                                                  supplied through
                                                  environment
                                                  configuration or a
                                                  secret manager; they
                                                  shall not be committed
                                                  to source control.

  NFR-009                 Security                Data in transit shall
                                                  use TLS in deployed
                                                  environments.

  NFR-010                 Security                Inputs shall be
                                                  validated; database
                                                  access shall use
                                                  parameterized queries
                                                  or safe ORM practices.

  NFR-011                 Privacy                 Only synthetic data
                                                  shall be used in the
                                                  public showcase.
                                                  Sensitive fields shall
                                                  be minimized and masked
                                                  in logs.

  NFR-012                 Auditability            Important operations
                                                  shall include
                                                  correlation IDs and
                                                  structured audit
                                                  events.

  NFR-013                 Observability           The platform shall
                                                  expose structured logs,
                                                  application metrics,
                                                  traces where practical,
                                                  and health/readiness
                                                  endpoints.

  NFR-014                 Reliability             Background jobs and
                                                  event handlers shall
                                                  support retry policies,
                                                  idempotency, and
                                                  dead-letter or failure
                                                  handling where
                                                  applicable.

  NFR-015                 Testability             Domain rules, metric
                                                  calculations,
                                                  authorization, AI tool
                                                  contracts, and critical
                                                  workflows shall have
                                                  automated tests.

  NFR-016                 Portability             The application shall
                                                  run locally using
                                                  documented
                                                    configuration without
                                                    requiring containers;
                                                    containerized
                                                    deployment may be
                                                    provided as an option.

  NFR-017                 Documentation           The repository shall
                                                  include setup
                                                  instructions,
                                                  architecture diagrams,
                                                  API documentation, data
                                                  dictionary, model
                                                  cards, and demo
                                                  scenarios.

  NFR-018                 Accessibility           The web interface shall
                                                  support keyboard
                                                  navigation, readable
                                                  contrast, and clear
                                                  labels for key
                                                  dashboard information.

  NFR-019                 Interoperability        APIs shall use
                                                  documented, versioned
                                                  contracts and
                                                  consistent error
                                                  responses.

  NFR-020                 Cost control            External LLM calls
                                                  shall be configurable,
                                                  rate-limited,
                                                  observable, and bounded
                                                  by token or usage
                                                  limits.
  -----------------------------------------------------------------------

Performance targets are initial engineering objectives, not measured
claims. They must be validated against a documented test environment and
workload.

## 10. Architecture Requirements

### 10.1 Architectural style

The initial implementation shall use a modular monolith. It should
deliver the operational simplicity of a single deployable backend while
maintaining boundaries that make future extraction possible.

### 10.2 Proposed modules

1.  **Identity & Access:** authentication, roles, permissions.
2.  **Customer:** synthetic customer profile and segmentation.
3.  **Loan Management:** loan accounts, products, and lifecycle status.
4.  **Repayment:** schedules, payment transactions, allocations,
    reversals.
5.  **Delinquency:** DPD calculation, snapshots, delinquency buckets.
6.  **Analytics:** portfolio KPIs, trends, roll rates, cure rates.
7.  **Risk & ML:** feature generation, inference integration,
    predictions, explanations, model metadata.
8.  **Collections:** worklists, prioritization policies, activities,
    outcomes.
9.  **AI Assistant:** LLM orchestration, RAG, tool registry, response
    validation.
10. **Audit & Reporting:** audit events, report generation, exports.
11. **Platform & Observability:** configuration, health, metrics,
    correlation, background jobs.

### 10.3 Dependency rules

-   Modules shall expose use cases or application interfaces rather than
    allowing arbitrary access to another module's internals.
-   A module shall own its domain model and persistence mappings.
-   Cross-module interactions shall use explicit interfaces, application
    events, or well-defined queries.
-   Circular module dependencies are prohibited.
-   The AI Assistant shall access business capabilities through approved
    application interfaces, never unrestricted database access.
-   The initial deployment may use one relational database, but schema
    ownership and migration responsibility shall be clear.
-   External integrations shall be behind adapters and ports to avoid
    vendor lock-in.

### 10.4 Technology baseline

  -----------------------------------------------------------------------
  Concern                             Preferred technology
  ----------------------------------- -----------------------------------
  Frontend                            Angular

  Backend                             Java 25 and Spring Boot, subject to
                                      dependency compatibility

  Persistence                         PostgreSQL

  Cache                               Redis where justified

  ML                                  Python, FastAPI, scikit-learn,
                                      XGBoost or LightGBM

  Explainability                      SHAP

  GenAI                               Provider-neutral LLM integration,
                                      tool calling, and RAG

  Messaging                           In-process events initially; Kafka
                                      when asynchronous integration is
                                      justified

  API contract                        REST and OpenAPI

  Packaging                           Docker Compose for local demo

  Orchestration                       Kubernetes as an advanced
                                      deployment target

  Observability                       OpenTelemetry where practical,
                                      Prometheus, Grafana, structured
                                      logs

  CI/CD                               GitHub Actions
  -----------------------------------------------------------------------

Technology versions shall be pinned and checked for compatibility during
implementation. Kafka and Kubernetes are not mandatory prerequisites for
the first working vertical slice.

## 11. Security, Privacy, and Responsible Use

-   Use synthetic data only in the public repository and demo
    environment.
-   Apply least-privilege access and deny-by-default authorization.
-   Do not expose customer-level information in aggregate dashboards
    unless the user's role permits it.
-   Do not place secrets, access tokens, or unnecessary personal data in
    prompts or logs.
-   Treat uploaded and retrieved documents as untrusted content.
-   Validate and authorize every AI tool call on the server.
-   Maintain an audit trail for sensitive access and material
    AI-assisted operations.
-   Clearly label predictions as estimates, explain their limitations,
    and retain human oversight.
-   Do not use protected or sensitive attributes to make adverse
    decisions.
-   Provide a mechanism to disable AI functionality while keeping
    deterministic analytics available.
-   Document retention and deletion behavior for AI interaction metadata
    and audit records.

This is an engineering showcase, not a legal or regulatory compliance
certification. Any future real-bank deployment would require
institution-specific legal, compliance, security, model-risk, and
privacy review.

## 12. User Experience Requirements

The web application shall provide the following primary screens:

1.  **Executive Dashboard:** portfolio summary, repayment trends, PAR,
    delinquency distribution.
2.  **Loan Portfolio:** searchable and filterable loan list with loan
    details.
3.  **Loan Detail:** schedule, payments, DPD history, prediction
    history, collection activities.
4.  **Risk Monitoring:** risk distribution, high-risk accounts, model
    performance indicators.
5.  **Collection Workbench:** prioritized worklist, account context,
    activity recording, suggested actions.
6.  **AI Assistant:** natural-language questions, grounded answers,
    visible filters and data provenance.
7.  **Model Insights:** model versions, evaluation results, feature
    importance, drift indicators.
8.  **Administration & Audit:** user roles, configuration, and audit
    history.

All screens shall display suitable empty, loading, error, and
no-permission states. Important values shall include definitions or
accessible explanations.

## 13. Acceptance Criteria

The initial release is accepted when the following criteria are met.

### 13.1 Data and loan lifecycle

-   [ ] A documented synthetic dataset can be generated reproducibly.
-   [ ] Loan accounts, schedules, payments, reversals, and collection
    activities can be loaded and queried.
-   [ ] Invalid records are rejected or quarantined with reasons.
-   [ ] Payment corrections preserve the original transaction history.
-   [ ] DPD calculations pass tests for due dates, partial payments,
    reversals, and as-of dates.

### 13.2 Analytics

-   [ ] Dashboard totals reconcile to the underlying synthetic source
    records for the same reporting period.
-   [ ] Repayment rate and PAR definitions are documented and covered by
    automated tests.
-   [ ] Users can filter analytics by the supported dimensions.
-   [ ] DPD buckets, trends, and at least one roll-rate and cure-rate
    view are available.
-   [ ] Reports display currency, as-of date or period, refresh time,
    and synthetic-data labeling.

### 13.3 Predictive AI

-   [ ] A baseline model and at least one candidate model are trained
    and evaluated.
-   [ ] The target definition, prediction horizon, feature definitions,
    and time split are documented.
-   [ ] Leakage checks are performed and documented.
-   [ ] Evaluation includes PR-AUC, precision, recall, confusion matrix,
    and calibration assessment.
-   [ ] Each prediction records model version, timestamp, horizon, and
    score or probability.
-   [ ] Individual predictions include understandable feature
    contributions or a clear explanation when unavailable.
-   [ ] The UI identifies insufficient-data cases rather than
    fabricating a prediction.
-   [ ] A model card and limitations statement are included in the
    repository.

### 13.4 Collection intelligence

-   [ ] A collection worklist can be generated from delinquency and
    configured priority rules.
-   [ ] Users can see why an account was prioritized.
-   [ ] Authorized users can record a collection activity and outcome.
-   [ ] Suggested actions are visibly advisory and require human review.
-   [ ] No automated borrower contact or adverse action is performed.

### 13.5 Generative AI

-   [ ] An authorized user can ask a natural-language question about at
    least three supported portfolio metrics.
-   [ ] Numeric answers are obtained from approved analytics tools, not
    invented by the LLM.
-   [ ] Answers identify reporting period and filters and provide
    traceable metric/source information.
-   [ ] Unsupported or ambiguous questions produce a clarification or
    limitation response.
-   [ ] Access controls apply equally to AI tool calls and ordinary
    APIs.
-   [ ] Prompt-injection and unauthorized-tool-use test cases are
    included.
-   [ ] AI functionality can be disabled without disabling deterministic
    portfolio analytics.

### 13.6 Security and operations

-   [ ] Role-based authorization is tested at the API layer.
-   [ ] Secrets are not committed to the repository.
-   [ ] Sensitive values are not unnecessarily written to application
    logs.
-   [ ] Health checks and structured logs are available.
-   [ ] Critical domain, API, and AI tool contracts have automated
    tests.
-   [ ] The application can be started locally using documented
    instructions.
-   [ ] API documentation and an architecture diagram are included.

### 13.7 Demo readiness

-   [ ] A reviewer can follow a documented end-to-end demo scenario.
-   [ ] The demo shows a portfolio trend, a high-risk loan, its
    explanation, a collection worklist, and a grounded AI question.
-   [ ] The repository clearly distinguishes implemented features,
    planned features, and illustrative results.
-   [ ] No real customer or confidential banking data is included.

## 14. Suggested End-to-End Demonstration Scenario

1.  Generate and load a synthetic portfolio containing multiple loan
    products and repayment histories.
2.  Open the executive dashboard and inspect repayment rate, outstanding
    balance, PAR, and delinquency distribution.
3.  Filter to a loan product or customer segment where delinquency is
    increasing.
4.  Open a loan with a high predicted delinquency risk.
5.  Review the prediction horizon, model version, and SHAP-based
    contributing factors.
6.  Open the collection workbench and find the loan in the prioritized
    worklist.
7.  Review the suggested next action and record a simulated collection
    outcome as an authorized user.
8.  Ask the AI assistant: "Which segment contributed most to the
    increase in PAR 30 during this period?"
9.  Inspect the answer's reporting period, filters, metric definition,
    and supporting analytics result.
10. Review the audit trail and model metadata.

## 15. Delivery Milestones

  -----------------------------------------------------------------------
  Milestone                           Outcome
  ----------------------------------- -----------------------------------
  M0 --- Product foundation           Scope, domain glossary,
                                      architecture decisions, repository
                                      structure

  M1 --- Data foundation              Synthetic data generator, schema,
                                      import validation, loan and
                                      repayment APIs

  M2 --- Analytics MVP                DPD, repayment metrics, PAR,
                                      dashboard and reconciliation tests

  M3 --- Predictive intelligence      Feature pipeline, baseline and
                                      candidate model, evaluation,
                                      inference API

  M4 --- Explainability               SHAP explanations, model metadata,
                                      risk monitoring screens

  M5 --- Collection workbench         Priority policies, worklist,
                                      activity tracking, outcome
                                      reporting

  M6 --- GenAI assistant              RAG knowledge base, analytics
                                      tools, grounded answers, safety
                                      tests

  M7 --- Showcase hardening           Security review, observability,
                                      Docker deployment, documentation,
                                      demo script
  -----------------------------------------------------------------------

Milestones should be completed in sequence where dependencies require
it. The first demonstrable vertical slice should prioritize synthetic
data, repayment metrics, and one end-to-end loan investigation before
advanced AI features.

## 16. Repository Deliverables

The repository should contain, at minimum:

-   `README.md` --- product overview, screenshots, setup, demo,
    limitations.
-   `REQUIREMENTS.md` --- this requirements baseline.
-   `ARCHITECTURE.md` --- module boundaries, dependencies, deployment
    view, key decisions.
-   `docs/domain-glossary.md` --- loan, repayment, DPD, PAR, roll-rate,
    and cure-rate definitions.
-   `docs/data-dictionary.md` --- entities, fields, constraints, and
    synthetic-data notes.
-   `docs/ai/model-card.md` --- model purpose, data, evaluation,
    limitations, and intended use.
-   `docs/ai/evaluation.md` --- reproducible evaluation results and
    methodology.
-   `docs/security/threat-model.md` --- assets, trust boundaries,
    threats, and mitigations.
-   `docs/demo-scenario.md` --- repeatable end-to-end walkthrough.
-   `openapi/` --- API specifications.
-   `infra/` --- local container and deployment configuration.
-   Automated tests for domain logic, APIs, authorization, analytics,
    and AI tools.

## 17. Assumptions and Open Decisions

The following items must be resolved during design and implementation:

1.  Initial loan products: personal loans only, or personal loans plus
    one additional product for comparison.
2.  Currency and locale: default demo currency is USD; keep currency
    explicit on monetary records and make locale-specific formatting
    configurable for future internationalization.
3.  DPD policy: calendar-day calculation, treatment of grace periods,
    holidays, and payment allocation.
4.  Repayment-rate definition: amount-based, installment-count-based, or
    both.
5.  PAR convention: threshold inclusivity and balance denominator.
6.  Prediction label: whether the target is any missed installment,
    reaching 1+ DPD, or reaching a defined DPD threshold within 30 days.
7.  Model risk bands: thresholds based on calibration and operational
    review rather than arbitrary labels.
8.  LLM provider and deployment mode: external API or locally hosted
    model.
9.  User identity provider: demo authentication first, with an adapter
    for future enterprise identity integration.
10. Data retention periods for audit events and AI interaction metadata.

These decisions shall be recorded in architecture decision records or
the relevant domain documentation before the corresponding feature is
considered complete.

------------------------------------------------------------------------

## 18. Definition of Done

A feature is complete only when:

-   Its acceptance criteria are met.
-   Domain and API tests are passing.
-   Authorization and validation are implemented server-side.
-   Logs and errors are actionable without exposing secrets or
    unnecessary sensitive data.
-   API and user-facing documentation are updated.
-   The feature works with the documented synthetic dataset.
-   AI outputs, where applicable, are traceable and clearly labeled as
    predictions or recommendations.
-   The feature is demonstrable using the documented local environment.

**Product principle:** ArthaLoan AI must prioritize trustworthy banking
analytics, explainable predictions, controlled AI capabilities, and
human accountability over AI novelty.
