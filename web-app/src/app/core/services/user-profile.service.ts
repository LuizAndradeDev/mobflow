import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs';
import { UserProfile, UpdateUserProfileRequest } from '../models/user-profile.model';
import { UserProfileStateService } from './user-profile-state.service';

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  private readonly API = 'http://localhost:8081/api/users';

  constructor(
    private http: HttpClient,
    private userProfileState: UserProfileStateService,
  ) {}

  getMyProfile() {
    return this.http.get<UserProfile>(`${this.API}/me`).pipe(
      tap((profile) => this.userProfileState.set(profile)),
    );
  }

  updateMyProfile(data: UpdateUserProfileRequest) {
    return this.http.put<UserProfile>(`${this.API}/me`, data).pipe(
      tap((profile) => this.userProfileState.set(profile)),
    );
  }

  updateAvatar(file: File) {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.patch<UserProfile>(`${this.API}/me/avatar`, formData).pipe(
      tap((profile) => this.userProfileState.set(profile)),
    );
  }

  getProfileByAuthId(authId: string) {
    return this.http.get<UserProfile>(`${this.API}/${authId}`);
  }
}
