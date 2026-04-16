import { Injectable } from '@angular/core';
import { Theme } from './theme.service';

@Injectable({ providedIn: 'root' })
export class BrowserStorageService {
  private readonly tokenKey = 'token';
  private readonly themeKey = 'mobflow-theme';

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  clearToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  hasToken(): boolean {
    return this.getToken() !== null;
  }

  getTheme(): Theme | null {
    const theme = localStorage.getItem(this.themeKey);
    return theme === 'light' || theme === 'dark' ? theme : null;
  }

  setTheme(theme: Theme): void {
    localStorage.setItem(this.themeKey, theme);
  }
}
