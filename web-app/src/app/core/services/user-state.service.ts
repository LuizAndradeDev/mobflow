import { Injectable, signal } from '@angular/core';

export interface UserState {
  username: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class UserStateService {
  private _user = signal<UserState | null>(null);

  readonly user = this._user.asReadonly();

  set(user: UserState) {
    this._user.set(user);
  }

  clear() {
    this._user.set(null);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }
}
