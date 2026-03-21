import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'mobflow-theme';

  private _theme = signal<Theme>(this.loadTheme());
  readonly theme = this._theme.asReadonly();

  private loadTheme(): Theme {
    return (localStorage.getItem(this.STORAGE_KEY) as Theme) || 'light';
  }

  setTheme(theme: Theme) {
    this._theme.set(theme);
    localStorage.setItem(this.STORAGE_KEY, theme);
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  toggle() {
    this.setTheme(this._theme() === 'light' ? 'dark' : 'light');
  }

  isDark(): boolean {
    return this._theme() === 'dark';
  }
}
