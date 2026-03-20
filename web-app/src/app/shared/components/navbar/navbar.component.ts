import { ChangeDetectorRef, Component, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { UserStateService } from '../../../core/services/user-state.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserProfileStateService } from '../../../core/services/user-profile-state.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
})
export class NavbarComponent {
  constructor(
    public userState: UserStateService,
    private authService: AuthService,
    private router: Router,
    public userProfileState: UserProfileStateService,
    private cdr: ChangeDetectorRef,
  ) {
    effect(() => {
      this.userState.user();
      this.userProfileState.profile();
      cdr.detectChanges();
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
