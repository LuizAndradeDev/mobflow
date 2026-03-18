import { Injectable, signal } from '@angular/core';
import { UserProfile } from '../models/user-profile.model';

@Injectable({ providedIn: 'root' })
export class UserProfileStateService {
  private _profile = signal<UserProfile | null>(null);

  readonly profile = this._profile.asReadonly();

  set(profile: UserProfile) {
    this._profile.set(profile);
  }

  clear() {
    this._profile.set(null);
  }

  hasProfile(): boolean {
    return this._profile() !== null;
  }
}
