import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError, tap, map, catchError } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { LoginRequest, AuthResponse, JwtPayload } from '../interfaces/auth.interface';

const STORAGE_KEY = 'auth_token';
const REFRESH_KEY = 'auth_refresh_token';

const MOCK_USERS: Record<string, { password: string; roles: string[] }> = {
  'supervisor@test.com': { password: '123456', roles: ['SUPERVISOR'] },
  'analista@test.com': { password: '123456', roles: ['ANALISTA'] },
  'admin@test.com': { password: 'admin', roles: ['ADMIN', 'SUPERVISOR'] },
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(this.loadToken());
  private readonly loadingSignal = signal(false);

  readonly token = this.tokenSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal());

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    this.loadingSignal.set(true);

    if (environment.useMockAuth) {
      return this.mockLogin(credentials).pipe(
        tap({
          next: (res) => {
            this.storeToken(res.token);
            this.loadingSignal.set(false);
          },
          error: () => this.loadingSignal.set(false),
        }),
      );
    }

    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, credentials)
      .pipe(
        tap({
          next: (res) => {
            this.storeToken(res.token);
            this.loadingSignal.set(false);
          },
          error: () => this.loadingSignal.set(false),
        }),
      );
  }

  logout(): void {
    this.tokenSignal.set(null);
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  getUserEmail(): string | null {
    const token = this.tokenSignal();
    if (!token) return null;
    try {
      const payload = this.decodeToken(token);
      return payload?.email ?? payload?.sub ?? null;
    } catch {
      return null;
    }
  }

  getUserRoles(): string[] {
    const token = this.tokenSignal();
    if (!token) return [];
    try {
      const payload = this.decodeToken(token);
      return payload?.roles ?? [];
    } catch {
      return [];
    }
  }

  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  isTokenExpired(): boolean {
    const token = this.tokenSignal();
    if (!token) return true;
    try {
      const payload = this.decodeToken(token);
      if (!payload?.exp) return false;
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  private mockLogin(credentials: LoginRequest): Observable<AuthResponse> {
    const user = MOCK_USERS[credentials.email];
    if (!user || user.password !== credentials.password) {
      return throwError(() => ({
        error: {
          timestamp: new Date().toISOString(),
          status: 401,
          error: 'Unauthorized',
          message: 'Invalid email or password',
          path: '/api/v1/auth/login',
        },
      }));
    }

    const now = Date.now();
    const expiration = now + 3600_000;
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(
      JSON.stringify({
        sub: credentials.email,
        email: credentials.email,
        roles: user.roles,
        iat: Math.floor(now / 1000),
        exp: Math.floor(expiration / 1000),
      }),
    );
    const mockToken = `${header}.${payload}.mock-signature`;

    const response: AuthResponse = {
      token: mockToken,
      issuedAt: new Date(now).toISOString(),
      expiration: new Date(expiration).toISOString(),
    };

    return of(response);
  }

  private storeToken(token: string): void {
    this.tokenSignal.set(token);
    localStorage.setItem(STORAGE_KEY, token);
  }

  private loadToken(): string | null {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return null;
    try {
      const payload = this.decodeToken(stored);
      if (payload?.exp && Date.now() >= payload.exp * 1000) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return stored;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private decodeToken(token: string): JwtPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = parts[1];
      const decoded = atob(payload);
      return JSON.parse(decoded) as JwtPayload;
    } catch {
      return null;
    }
  }
}
