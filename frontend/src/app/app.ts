import { HttpClient } from '@angular/common/http';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, of, switchMap, timer } from 'rxjs';

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

type View = 'overview' | 'portfolio' | 'loan-detail';
type LoadState = 'idle' | 'loading' | 'ready' | 'error';

@Component({
  imports: [CurrencyPipe, DatePipe],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly apiStatus = signal<'checking' | 'online' | 'offline'>('checking');
  protected readonly view = signal<View>('overview');
  protected readonly loans = signal<LoanSummary[]>([]);
  protected readonly selectedLoan = signal<LoanDetail | null>(null);
  protected readonly listState = signal<LoadState>('idle');
  protected readonly detailState = signal<LoadState>('idle');
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

  ngOnInit(): void {
    timer(0, 10_000)
      .pipe(
        switchMap(() =>
          this.http.get<ApiHealth>('/api/health').pipe(
            map((health): 'online' | 'offline' =>
              health.status === 'UP' ? 'online' : 'offline',
            ),
            catchError(() => of('offline' as const)),
          ),
        ),
        takeUntilDestroyed(),
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
    this.detailState.set('loading');
    this.errorMessage.set('');
    this.http
      .get<LoanDetail>(`/api/demo/loans/${encodeURIComponent(loanId)}`)
      .pipe(
        catchError(() => {
          this.detailState.set('error');
          this.errorMessage.set('We could not load this loan. Check the API connection and try again.');
          return of(null);
        }),
        takeUntilDestroyed(),
      )
      .subscribe((detail) => {
        if (detail) {
          this.selectedLoan.set(detail);
          this.detailState.set('ready');
        }
      });
  }

  protected backToPortfolio(): void {
    this.view.set('portfolio');
    this.selectedLoan.set(null);
  }

  protected retryLoans(): void {
    this.loadLoans();
  }

  protected updateSearch(event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.searchTerm.set(event.target.value);
    }
  }

  private loadLoans(): void {
    this.listState.set('loading');
    this.errorMessage.set('');
    this.http
      .get<LoanSummary[]>('/api/demo/loans')
      .pipe(
        catchError(() => {
          this.listState.set('error');
          this.errorMessage.set('We could not load the loan portfolio. Check the API connection and try again.');
          return of(null);
        }),
        takeUntilDestroyed(),
      )
      .subscribe((loans) => {
        if (loans) {
          this.loans.set(loans);
          this.listState.set('ready');
        }
      });
  }
}
