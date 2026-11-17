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

Stop the demo with `Ctrl+C`. The dashboard intentionally shows clearly
marked unavailable placeholders until portfolio data and analytics are
implemented; it does not display invented portfolio figures.

## Optional Docker setup

Docker Compose remains available for trying the containerized setup:

```sh
docker compose up --build
```

Open [http://localhost:4200](http://localhost:4200) for that setup.

All demo data will be synthetic. Do not add real customer or confidential
bank data to this repository or local showcase.

## Current foundation

- Angular dashboard shell with backend connection status.
- Java 25 / Spring Boot API with a health endpoint.
- Repeatable synthetic demo portfolio available at
  `GET /api/demo/portfolio` (12 customers, 12 loans, 144 installments).
- One-command local startup without Docker; Docker Compose is optional.

The portfolio is generated in memory with seed `20261117` and as-of date
`2026-10-31`. Override them with `ARTHALOAN_DEMO_SEED` and
`ARTHALOAN_DEMO_AS_OF_DATE` when launching the backend. The generator
includes performing, overdue, partial-payment, and reversal scenarios.

Authentication, portfolio metrics, predictions, and collection
workflows are not implemented yet.
