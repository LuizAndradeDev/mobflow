import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  currentTime = new Date();

  constructor(
    private authService: AuthService,
    private router: Router,
    public userProfileState: UserProfileStateService,
  ) {}

  ngOnInit() {
    setInterval(() => (this.currentTime = new Date()), 1000);
  }

  get greeting(): string {
    const hour = this.currentTime.getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
