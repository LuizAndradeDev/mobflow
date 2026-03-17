import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UserStateService } from '../../../../core/services/user-state.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  currentTime = new Date();

  constructor(
    private authService: AuthService,
    private router: Router,
    public userState: UserStateService,
  ) {}

  ngOnInit() {
    if (!this.userState.user()) {
      this.authService.getProfile().subscribe({
        error: () => this.router.navigate(['/login']),
      });
    }

    setInterval(() => (this.currentTime = new Date()), 1000);
  }

  get greeting(): string {
    const hour = this.currentTime.getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }
}
