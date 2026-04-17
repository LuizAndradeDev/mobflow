import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs';
import { SignupRequest, LoginRequest, AuthResponse } from '../models/auth.model';
import { UserStateService } from './user-state.service';
import { UserProfileStateService } from './user-profile-state.service';
import { UserProfileService } from './user-profile.service';
import { NotificationStateService } from './notification-state.service';
import { BrowserStorageService } from './browser-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiPath = '/api/auth';

  constructor(
    private readonly http: HttpClient,
    private readonly userState: UserStateService,
    private readonly userProfileState: UserProfileStateService,
    private readonly userProfileService: UserProfileService,
    private readonly notificationState: NotificationStateService,
    private readonly storage: BrowserStorageService,
  ) {}

  register(data: SignupRequest) {
    return this.http.post<{ username: string; email: string }>(`${this.apiPath}/signup`, data);
  }

  confirmEmail(token: string) {
    return this.http.get(`${this.apiPath}/confirm-email?token=${encodeURIComponent(token)}`, {
      responseType: 'text',
    });
  }

  login(data: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.apiPath}/login`, data).pipe(
      tap((response) => {
        this.storage.setToken(response.token);
        this.userState.set({ username: response.user.username, email: response.user.email });
        this.notificationState.startPolling();
      }),
    );
  }

  getProfile() {
    return this.http.get<{ username: string; email: string }>(`${this.apiPath}/profile`).pipe(
      tap((profile) => {
        this.userState.set(profile);
        this.userProfileService.getMyProfile().subscribe();
        this.notificationState.startPolling();
      }),
    );
  }

  logout() {
    this.storage.clearToken();
    this.userState.clear();
    this.userProfileState.clear();
    this.notificationState.clear();
  }
}
