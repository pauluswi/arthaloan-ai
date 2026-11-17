# ArthaLoan AI

ArthaLoan AI is a decision-support showcase for synthetic loan portfolio
analytics, repayment-risk insights, collection prioritization, and
evidence-grounded AI. It does not make credit decisions or contact
borrowers.

## Run the demo (no Docker)

Requirements: Java 25 and Node.js 22.22.3+, 24.15.0+, or 26+ with npm.
The script uses an installed Maven if available; otherwise it downloads
Maven 3.9.9 into the user cache.

```sh
./demo.sh
```

Open [http://localhost:8080](http://localhost:8080). The script installs
the frontend's locked dependencies, builds Angular, packages its static
files into the Spring Boot application, and starts the single executable
JAR. The backend health endpoint is available at
[http://localhost:8080/api/health](http://localhost:8080/api/health).
Set `SERVER_PORT` to use another port, for example
`SERVER_PORT=8090 ./demo.sh`.

Stop the demo with `Ctrl+C`. The dashboard shows deterministic portfolio
analytics backed by the synthetic snapshot; no external model or database
is required.

For separate frontend/backend development, start Spring Boot on port 8080,
then run `npm --prefix frontend start` and open
[http://localhost:4200](http://localhost:4200). Angular's development-server
proxy forwards `/api` requests to the backend.

## Optional Docker setup

Docker Compose remains available for trying the containerized setup:

```sh
docker compose up --build
```

Open [http://localhost:4200](http://localhost:4200) for that setup.

All demo data will be synthetic. Do not add real customer or confidential
bank data to this repository or local showcase.

## Current foundation

- Angular dashboard with filterable portfolio metrics, delinquency buckets,
  PAR, repayment trends, portfolio search, and loan investigation.
- Java 25 / Spring Boot API with a health endpoint.
- Repeatable synthetic demo portfolio available at
  `GET /api/demo/portfolio` (12 customers, 12 loans, 144 installments).
- Loan list and investigation APIs at `GET /api/demo/loans` and
  `GET /api/demo/loans/{loanId}`, including payment-aware DPD, schedule
  balances, and reversal history.
- Interactive portfolio search and loan detail screens in the dashboard.
- Deterministic analytics at `GET /api/demo/analytics`, including period and
  dimension filters, repayment rate, delinquency distribution, and PAR 1/7/30/90.
- Explainable 30-day loan risk scores at `GET /api/demo/loans/{loanId}/risk`,
  using documented deterministic demo rules rather than a trained model.
- Ranked collection worklist at `GET /api/demo/collections/worklist`, with
  policy version, priority factors, and links to loan investigation.
- Deterministic analytics assistant at `POST /api/demo/assistant/ask`,
  answering an allowlisted set of portfolio questions with metric
  definitions, reporting dates, filters, and evidence.
- One-command local startup without Docker; Docker Compose is optional.

The portfolio is generated in memory with seed `20261117` and as-of date
`2026-10-31`. Override them with `ARTHALOAN_DEMO_SEED` and
`ARTHALOAN_DEMO_AS_OF_DATE` when launching the backend. The generator
includes performing, overdue, partial-payment, and reversal scenarios.

The analytics assistant is a deterministic demo, not an LLM; authentication,
interaction persistence, and trained-model predictions are not implemented.
Collection activity entries are kept in memory for the current server run
and are cleared on restart. Analytics, demo risk-score and worklist
conventions, and generated-data limitations are described in
[`docs/data-dictionary.md`](docs/data-dictionary.md).
