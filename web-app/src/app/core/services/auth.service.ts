import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs';
import { SignupRequest, LoginRequest, AuthResponse } from '../models/auth.model';
import { UserStateService } from './user-state.service';
import { UserProfileStateService } from './user-profile-state.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';

  constructor(
    private http: HttpClient,
    private userState: UserStateService,
    private userProfileState: UserProfileStateService,
  ) {}

  register(data: SignupRequest) {
    return this.http.post<{ username: string; email: string }>(`${this.API}/signup`, data);
  }

  login(data: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.API}/login`, data).pipe(
      tap((response) => {
        localStorage.setItem('token', response.token);
        this.userState.set({
          username: response.user.username,
          email: response.user.email,
        });
      }),
    );
  }

  getProfile() {
    return this.http.get<{ username: string; email: string }>(`${this.API}/profile`).pipe(
      tap((profile) => {
        this.userState.set(profile);
      }),
    );
  }

  logout() {
    localStorage.removeItem('token');
    this.userState.clear();
    this.userProfileState.clear();
  }
}
