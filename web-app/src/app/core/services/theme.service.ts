import { Injectable, signal } from '@angular/core';
import { BrowserStorageService } from './browser-storage.service';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _theme = signal<Theme>('light');
  readonly theme = this._theme.asReadonly();

  constructor(private readonly storage: BrowserStorageService) {
    this._theme.set(this.storage.getTheme() ?? 'light');
  }

  setTheme(theme: Theme): void {
    this._theme.set(theme);
    this.storage.setTheme(theme);
    document.documentElement.classList.toggle('dark', theme === 'dark');
  }

  toggle(): void {
    this.setTheme(this._theme() === 'light' ? 'dark' : 'light');
  }

  isDark(): boolean {
    return this._theme() === 'dark';
  }
}
