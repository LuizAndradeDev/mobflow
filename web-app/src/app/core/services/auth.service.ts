import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs';
import { SignupRequest, LoginRequest, AuthResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  register(data: SignupRequest) {
    return this.http.post<AuthResponse>(`${this.API}/signup`, data);
  }

  login(data: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.API}/login`, data).pipe(
      tap((response) => {
        localStorage.setItem('token', response.token);
      }),
    );
  }

  getProfile() {
    return this.http.get<{ username: string; email: string }>(`${this.API}/profile`);
  }

  logout() {
    localStorage.removeItem('token');
  }
}
