import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, of, switchMap, timer } from 'rxjs';

interface ApiHealth {
  status: string;
  service: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly apiStatus = signal<'checking' | 'online' | 'offline'>('checking');

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
}
