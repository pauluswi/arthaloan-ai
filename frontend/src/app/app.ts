import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, exhaustMap, map, of, switchMap, timeout, timer } from 'rxjs';

interface ApiHealth {
  status: string;
  service: string;
}

interface LoanSummary {
  loanId: string;
  customerId: string;
  segment: string;
  region: string;
  productId: string;
  productName: string;
  principal: number;
  outstandingPrincipal: number;
  currency: string;
  status: string;
  dpd: number;
  overdueAmount: number;
  asOfDate: string;
}

interface InstallmentDetail {
  installmentId: string;
  dueDate: string;
  principalDue: number;
  interestDue: number;
  feesDue: number;
  scheduledAmount: number;
  allocatedAmount: number;
  remainingAmount: number;
  dpd: number;
  status: string;
}

interface PaymentAllocation {
  installmentId: string;
  principalAmount: number;
  interestAmount: number;
  feesAmount: number;
}

interface PaymentTransaction {
  paymentId: string;
  loanId: string;
  postingDate: string;
  amount: number;
  currency: string;
  type: 'PAYMENT' | 'REVERSAL';
  reversesPaymentId: string | null;
  allocations: PaymentAllocation[];
}

interface LoanDetail {
  loan: {
    loanId: string;
    customerId: string;
    productId: string;
    principal: number;
    annualInterestRatePercent: number;
    tenorMonths: number;
    originationDate: string;
    maturityDate: string;
    status: string;
  };
  customer: {
    customerId: string;
    segment: string;
    region: string;
  };
  product: {
    productId: string;
    name: string;
    repaymentFrequency: string;
  };
  currency: string;
  asOfDate: string;
  dpd: number;
  overdueAmount: number;
  outstandingPrincipal: number;
  installments: InstallmentDetail[];
  payments: PaymentTransaction[];
}

interface LoanRiskPrediction {
  loanId: string;
  predictedAt: string;
  horizonDays: number;
  modelVersion: string;
  status: 'AVAILABLE' | 'INELIGIBLE';
  score: number | null;
  riskBand: 'LOW' | 'MODERATE' | 'HIGH' | 'UNAVAILABLE';
  scoreDescription: string;
  eligibilityReason: string | null;
  factors: {
    name: string;
    observedValue: string;
    points: number;
    explanation: string;
  }[];
}

interface PortfolioAnalytics {
  periodStart: string;
  periodEnd: string;
  asOfDate: string;
  currency: string;
  filters: {
    productId: string | null;
    segment: string | null;
    region: string | null;
    loanStatus: string | null;
  };
  summary: {
    activeLoans: number;
    outstandingPrincipal: number;
    scheduledAmount: number;
    amountCollected: number;
    repaymentRatePercent: number | null;
  };
  portfolioAtRisk: {
    thresholdDays: number;
    qualifyingLoans: number;
    atRiskPrincipal: number;
    portfolioPrincipal: number;
    ratioPercent: number | null;
  }[];
  delinquencyDistribution: {
    bucket: string;
    loanCount: number;
    outstandingPrincipal: number;
  }[];
  repaymentTrend: {
    periodStart: string;
    periodEnd: string;
    scheduledAmount: number;
    amountCollected: number;
    repaymentRatePercent: number | null;
  }[];
  definitions: {
    repaymentRate: string;
    dpd: string;
    portfolioAtRisk: string;
    outstandingPrincipal: string;
  };
  filterOptions: {
    products: { productId: string; name: string }[];
    segments: string[];
    regions: string[];
    loanStatuses: string[];
  };
}

type View = 'overview' | 'portfolio' | 'loan-detail';
type LoadState = 'idle' | 'loading' | 'ready' | 'error';

@Component({
  imports: [CurrencyPipe, DatePipe, DecimalPipe],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private static readonly API_TIMEOUT_MS = 8_000;
  protected readonly apiStatus = signal<'checking' | 'online' | 'offline'>('checking');
  protected readonly view = signal<View>('overview');
  protected readonly loans = signal<LoanSummary[]>([]);
  protected readonly selectedLoan = signal<LoanDetail | null>(null);
  protected readonly riskPrediction = signal<LoanRiskPrediction | null>(null);
  protected readonly riskState = signal<LoadState>('idle');
  protected readonly riskError = signal('');
  protected readonly riskRequestLoanId = signal('');
  protected readonly listState = signal<LoadState>('idle');
  protected readonly detailState = signal<LoadState>('idle');
  protected readonly analytics = signal<PortfolioAnalytics | null>(null);
  protected readonly analyticsState = signal<LoadState>('idle');
  protected readonly analyticsError = signal('');
  protected readonly selectedMonth = signal('');
  protected readonly selectedProduct = signal('');
  protected readonly selectedSegment = signal('');
  protected readonly selectedRegion = signal('');
  protected readonly selectedLoanStatus = signal('');
  protected readonly errorMessage = signal('');
  protected readonly searchTerm = signal('');
  protected readonly filteredLoans = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();
    if (!query) {
      return this.loans();
    }

    return this.loans().filter((loan) =>
      [loan.loanId, loan.customerId, loan.productName, loan.segment, loan.region]
        .join(' ')
        .toLowerCase()
        .includes(query),
    );
  });
  protected readonly maxTrendAmount = computed(() =>
    Math.max(
      1,
      ...(this.analytics()?.repaymentTrend ?? []).flatMap((month) => [
        month.scheduledAmount,
        month.amountCollected,
      ]),
    ),
  );
  protected readonly maxBucketCount = computed(() =>
    Math.max(1, ...(this.analytics()?.delinquencyDistribution ?? []).map((bucket) => bucket.loanCount)),
  );

  ngOnInit(): void {
    this.loadAnalytics();
    timer(0, 10_000)
      .pipe(
        exhaustMap(() =>
          this.http.get<ApiHealth>('/api/health').pipe(
            timeout({ first: 4_000 }),
            map((health): 'online' | 'offline' =>
              health.status === 'UP' ? 'online' : 'offline',
            ),
            catchError(() => of('offline' as const)),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((status) => this.apiStatus.set(status));
  }

  protected showOverview(): void {
    this.view.set('overview');
    this.selectedLoan.set(null);
  }

  protected showPortfolio(): void {
    this.view.set('portfolio');
    this.selectedLoan.set(null);
    if (this.listState() === 'idle' || this.listState() === 'error') {
      this.loadLoans();
    }
  }

  protected openLoan(loanId: string): void {
    this.view.set('loan-detail');
    this.selectedLoan.set(null);
    this.riskPrediction.set(null);
    this.riskState.set('loading');
    this.riskError.set('');
    this.riskRequestLoanId.set(loanId);
    this.detailState.set('loading');
    this.errorMessage.set('');
    this.http
      .get<LoanDetail>(`/api/demo/loans/${encodeURIComponent(loanId)}`)
      .pipe(
        timeout({ first: App.API_TIMEOUT_MS }),
        catchError(() => {
          this.detailState.set('error');
          this.errorMessage.set('We could not load this loan. Check the API connection and try again.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((detail) => {
        if (detail) {
          this.selectedLoan.set(detail);
          this.detailState.set('ready');
        }
      });
    this.http
      .get<LoanRiskPrediction>(`/api/demo/loans/${encodeURIComponent(loanId)}/risk`)
      .pipe(
        timeout({ first: App.API_TIMEOUT_MS }),
        catchError(() => {
          if (this.riskRequestLoanId() === loanId) {
            this.riskState.set('error');
            this.riskError.set('The demo risk score could not be loaded. Check the API connection and try again.');
          }
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((prediction) => {
        if (prediction && this.riskRequestLoanId() === loanId) {
          this.riskPrediction.set(prediction);
          this.riskState.set('ready');
        }
      });
  }

  protected backToPortfolio(): void {
    this.view.set('portfolio');
    this.selectedLoan.set(null);
    this.riskRequestLoanId.set('');
  }

  protected retryLoans(): void {
    this.loadLoans();
  }

  protected updateSearch(event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.searchTerm.set(event.target.value);
    }
  }

  protected updateMonth(event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.selectedMonth.set(event.target.value);
    }
  }

  protected updateProduct(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.selectedProduct.set(event.target.value);
    }
  }

  protected updateSegment(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.selectedSegment.set(event.target.value);
    }
  }

  protected updateRegion(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.selectedRegion.set(event.target.value);
    }
  }

  protected updateLoanStatus(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.selectedLoanStatus.set(event.target.value);
    }
  }

  protected applyAnalyticsFilters(): void {
    const month = this.selectedMonth();
    const params = new URLSearchParams();
    if (month) {
      const [yearText, monthText] = month.split('-');
      const year = Number(yearText);
      const monthNumber = Number(monthText);
      const lastDay = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate();
      params.set('periodStart', `${month}-01`);
      params.set('periodEnd', `${month}-${String(lastDay).padStart(2, '0')}`);
    }
    this.addFilter(params, 'productId', this.selectedProduct());
    this.addFilter(params, 'segment', this.selectedSegment());
    this.addFilter(params, 'region', this.selectedRegion());
    this.addFilter(params, 'loanStatus', this.selectedLoanStatus());
    this.loadAnalytics(params);
  }

  protected trendWidth(value: number): number {
    return value === 0 ? 0 : Math.max(5, (value / this.maxTrendAmount()) * 100);
  }

  protected bucketWidth(value: number): number {
    return value === 0 ? 0 : Math.max(5, (value / this.maxBucketCount()) * 100);
  }

  protected retryAnalytics(): void {
    this.applyAnalyticsFilters();
  }

  private loadLoans(): void {
    this.listState.set('loading');
    this.errorMessage.set('');
    this.http
      .get<LoanSummary[]>('/api/demo/loans')
      .pipe(
        timeout({ first: App.API_TIMEOUT_MS }),
        catchError(() => {
          this.listState.set('error');
          this.errorMessage.set('We could not load the loan portfolio. Check the API connection and try again.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((loans) => {
        if (loans) {
          this.loans.set(loans);
          this.listState.set('ready');
        }
      });
  }

  private loadAnalytics(params = new URLSearchParams()): void {
    let httpParams = new HttpParams();
    params.forEach((value, key) => {
      httpParams = httpParams.set(key, value);
    });

    this.analyticsState.set('loading');
    this.analyticsError.set('');
    this.http
      .get<PortfolioAnalytics>('/api/demo/analytics', { params: httpParams })
      .pipe(
        timeout({ first: App.API_TIMEOUT_MS }),
        catchError((error: unknown) => {
          this.analyticsState.set('error');
          const reason = error instanceof HttpErrorResponse
            ? error.status === 0
              ? 'The API did not respond. Keep ./demo.sh running and check http://localhost:8080/api/health.'
              : `The API returned HTTP ${error.status}.`
            : 'The API did not respond within 8 seconds. Keep ./demo.sh running and check http://localhost:8080/api/health.';
          this.analyticsError.set(`Portfolio analytics failed. ${reason}`);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((analytics) => {
        if (analytics) {
          this.analytics.set(analytics);
          if (!this.selectedMonth()) {
            this.selectedMonth.set(analytics.periodStart.slice(0, 7));
          }
          this.analyticsState.set('ready');
        }
      });
  }

  private addFilter(params: URLSearchParams, name: string, value: string): void {
    if (value) {
      params.set(name, value);
    }
  }
}
