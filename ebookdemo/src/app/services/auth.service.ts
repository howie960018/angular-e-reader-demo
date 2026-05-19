import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { User, UserRole, AuthResponse, LoginRequest, RegisterRequest } from '../models';
import { CartService } from './cart.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/auth';
  private currentUser$ = new BehaviorSubject<User | null>(this.loadStoredUser());

  constructor(private http: HttpClient, private cartService: CartService) {}

  get currentUser(): Observable<User | null> {
    return this.currentUser$.asObservable();
  }

  get currentUserValue(): User | null {
    return this.currentUser$.value;
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<any>(`${this.baseUrl}/login`, request).pipe(
      map(res => this.normalizeAuthResponse(res)),
      tap(res => {
        this.storeSession(res);
        this.cartService.loadCart().subscribe();
      }),
      catchError(this.handleError)
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<any>(`${this.baseUrl}/register`, request).pipe(
      map(res => this.normalizeAuthResponse(res)),
      tap(res => {
        this.storeSession(res);
        this.cartService.loadCart().subscribe();
      }),
      catchError(this.handleError)
    );
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('current_user');
    this.currentUser$.next(null);
    this.cartService.resetCart();
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('auth_token');
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  /** Convert numeric id from backend to string so existing components stay compatible */
  private normalizeUser(raw: any): User {
    return {
      id: String(raw.id),
      username: raw.username,
      email: raw.email,
      role: raw.role as UserRole,
      createdAt: new Date(raw.createdAt)
    };
  }

  private normalizeAuthResponse(raw: any): AuthResponse {
    return { token: raw.token, user: this.normalizeUser(raw.user) };
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem('auth_token', res.token);
    localStorage.setItem('current_user', JSON.stringify(res.user));
    this.currentUser$.next(res.user);
  }

  private loadStoredUser(): User | null {
    const stored = localStorage.getItem('current_user');
    return stored ? JSON.parse(stored) : null;
  }

  private handleError(err: HttpErrorResponse) {
    const message = err.error?.message || err.message || 'Unknown error';
    return throwError(() => new Error(message));
  }
}
