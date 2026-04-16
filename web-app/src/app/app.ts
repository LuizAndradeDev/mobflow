import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AlertComponent } from './shared/components/alert/alert.component';
import { AuthService } from './core/services/auth.service';
import { UserStateService } from './core/services/user-state.service';
import { CommonModule } from '@angular/common';
import { BrowserStorageService } from './core/services/browser-storage.service';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AlertComponent, CommonModule],
  template: `
    <app-alert></app-alert>
    <router-outlet></router-outlet>
  `,
})
export class App implements OnInit {
  constructor(
    private readonly authService: AuthService,
    private readonly userState: UserStateService,
    private readonly storage: BrowserStorageService,
    private readonly themeService: ThemeService,
  ) {}

  ngOnInit() {
    this.themeService.setTheme(this.themeService.theme());

    if (this.storage.hasToken() && !this.userState.user()) {
      this.authService.getProfile().subscribe({
        error: () => {
          this.authService.logout();
        },
      });
    }
  }
}
