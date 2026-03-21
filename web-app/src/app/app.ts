import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AlertComponent } from './shared/components/alert/alert.component';
import { AuthService } from './core/services/auth.service';
import { UserStateService } from './core/services/user-state.service';
import { CommonModule } from '@angular/common';

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
    private authService: AuthService,
    private userState: UserStateService,
  ) {}

  ngOnInit() {
    const token = localStorage.getItem('token');
    if (token && !this.userState.user()) {
      this.authService.getProfile().subscribe({
        error: () => {
          this.authService.logout();
        },
      });
    }
  }
}
