import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  username = '';
  email = '';
  currentTime = new Date();

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.authService.getProfile().subscribe({
      next: (profile) => {
        this.username = profile.username;
        this.email = profile.email;
      },
      error: () => this.router.navigate(['/login']),
    });

    setInterval(() => (this.currentTime = new Date()), 1000);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get greeting(): string {
    const hour = this.currentTime.getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }
}
