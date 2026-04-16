import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { UserProfileStateService } from '../../core/services/user-profile-state.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { TaskService } from '../../core/services/task.service';
import { Workspace } from '../../core/models/workspace.model';
import { TaskAnalytics } from '../../core/models/task.model';
import { calculateCompletionPercentage, EMPTY_TASK_ANALYTICS } from '../../core/utils/task-analytics.util';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.component.html',
})
export class HomeComponent implements OnInit {
  isLoading = true;
  workspaces: Workspace[] = [];
  analytics: TaskAnalytics | null = null;

  constructor(
    public userProfileState: UserProfileStateService,
    private readonly workspaceService: WorkspaceService,
    private readonly taskService: TaskService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get profile() {
    return this.userProfileState.profile();
  }

  get completionPct(): number {
    if (!this.analytics) {
      return 0;
    }

    return calculateCompletionPercentage(
      this.analytics.completedTasks,
      this.analytics.assignedTasks,
    );
  }

  ngOnInit(): void {
    const profile = this.userProfileState.profile();
    if (!profile) {
      this.finishLoading();
      return;
    }

    const authId = profile.authId;

    forkJoin({
      workspaces: this.workspaceService.listMine().pipe(catchError(() => of([] as Workspace[]))),
      analytics: this.taskService.getUserAnalytics(authId).pipe(
        catchError(() => of(EMPTY_TASK_ANALYTICS)),
      ),
    }).subscribe({
      next: ({ workspaces, analytics }) => {
        this.workspaces = workspaces;
        this.analytics = analytics;
        this.finishLoading();
      },
      error: () => {
        this.finishLoading();
      },
    });
  }

  private finishLoading(): void {
    this.isLoading = false;
    this.cdr.markForCheck();
  }
}
