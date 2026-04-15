import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserProfileStateService } from '../../../core/services/user-profile-state.service';
import { UserStateService } from '../../../core/services/user-state.service';
import { WorkspaceService } from '../../../core/services/workspace.service';
import { Workspace } from '../../../core/models/workspace.model';
import { NotificationStateService } from '../../../core/services/notification-state.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent implements OnInit {
  expanded = signal(false);
  analyticsOpen = signal(false);
  workspaces: Workspace[] = [];

  private leaveTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    public userState: UserStateService,
    public userProfileState: UserProfileStateService,
    public notificationState: NotificationStateService,
    private authService: AuthService,
    private workspaceService: WorkspaceService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.notificationState.startPolling();
    this.workspaceService.listMine().subscribe({
      next: (workspaces) => (this.workspaces = workspaces),
    });
  }

  onContainerEnter() {
    if (this.leaveTimer) {
      clearTimeout(this.leaveTimer);
      this.leaveTimer = null;
    }
    this.expanded.set(true);
  }

  onContainerLeave() {
    this.leaveTimer = setTimeout(() => {
      this.expanded.set(false);
      this.analyticsOpen.set(false);
    }, 120);
  }

  toggleAnalytics() {
    this.analyticsOpen.update((v) => !v);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  goToProfile() {
    this.router.navigate(['/profile/edit']);
  }
}
