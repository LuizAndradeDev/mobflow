import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { WorkspaceService } from '../../core/services/workspace.service';
import { TaskService } from '../../core/services/task.service';
import { UserProfileStateService } from '../../core/services/user-profile-state.service';
import { Workspace, WorkspaceMember } from '../../core/models/workspace.model';
import { BoardSummary, TaskAnalytics, WorkspaceSummary } from '../../core/models/task.model';
import {
  AnalyticsSummary,
  buildAnalyticsSummary,
  calculateCompletionPercentage,
  calculateInProgressTasks,
  EMPTY_TASK_ANALYTICS,
} from '../../core/utils/task-analytics.util';
import { getBoardTaskCount, getPriorityDotClass } from '../../core/utils/task-board.util';

interface WorkspaceStats {
  workspace: Workspace;
  summary: WorkspaceSummary | null;
  analytics: TaskAnalytics;
  inProgressTasks: number;
  completionPct: number;
  members: number;
}

type AnalyticsScope = 'personal' | 'team' | string;

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './analytics.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnalyticsComponent implements OnInit {
  scope: AnalyticsScope = 'personal';

  workspaces: Workspace[] = [];
  workspaceStats: WorkspaceStats[] = [];
  personalAnalytics: TaskAnalytics | null = null;
  crossWorkspaceAnalytics: TaskAnalytics | null = null;
  isLoading = true;

  constructor(
    private readonly route: ActivatedRoute,
    public router: Router,
    private readonly workspaceService: WorkspaceService,
    private readonly taskService: TaskService,
    public userProfileState: UserProfileStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((p) => {
      this.scope = p['scope'] ?? 'personal';
      this.cdr.markForCheck();
    });

    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    const profile = this.userProfileState.profile();
    if (!profile) {
      this.finishLoading();
      return;
    }

    const authId = profile.authId;

    this.workspaceService.listMine().subscribe({
      next: (workspaces) => {
        this.workspaces = workspaces;

        if (workspaces.length === 0) {
          this.finishLoading();
          return;
        }

        const ids = workspaces.map((w) => w.id);

        forkJoin({
          summaries: this.taskService.getBatchSummaries(ids).pipe(
            catchError(() => of([] as WorkspaceSummary[])),
          ),
          workspaceAnalytics: forkJoin(
            workspaces.map((w) =>
              this.taskService.getWorkspaceAnalytics(w.id, authId).pipe(
                catchError(() => of(EMPTY_TASK_ANALYTICS)),
              ),
            ),
          ),
          memberCounts: forkJoin(
            workspaces.map((w) =>
              this.workspaceService.listMembers(w.id).pipe(catchError(() => of([] as WorkspaceMember[]))),
            ),
          ),
          personalAnalytics: this.taskService.getUserAnalytics(authId).pipe(
            catchError(() => of(EMPTY_TASK_ANALYTICS)),
          ),
          crossWorkspaceAnalytics: this.taskService.getUserAnalyticsAcrossWorkspaces(authId, ids).pipe(
            catchError(() => of(EMPTY_TASK_ANALYTICS)),
          ),
        }).subscribe({
          next: ({ summaries, workspaceAnalytics, memberCounts, personalAnalytics, crossWorkspaceAnalytics }) => {
            const summaryMap = new Map(summaries.map((s) => [s.workspaceId, s]));

            this.personalAnalytics = personalAnalytics;
            this.crossWorkspaceAnalytics = crossWorkspaceAnalytics;

            this.workspaceStats = workspaces.map((ws, i) => {
              const summary = summaryMap.get(ws.id) ?? null;
              const analytics = workspaceAnalytics[i];
              const members = memberCounts[i].length;
              const inProgressTasks = calculateInProgressTasks(
                analytics.totalTasks,
                analytics.completedTasks,
                analytics.overdueTasks,
              );
              const completionPct = calculateCompletionPercentage(
                analytics.completedTasks,
                analytics.totalTasks,
              );

              return {
                workspace: ws,
                summary,
                analytics,
                inProgressTasks,
                completionPct,
                members,
              };
            });

            this.finishLoading();
          },
          error: () => {
            this.finishLoading();
          },
        });
      },
      error: () => {
        this.finishLoading();
      },
    });
  }

  get selectedWorkspaceStats(): WorkspaceStats | null {
    if (this.scope === 'personal' || this.scope === 'team') return null;
    return this.workspaceStats.find((s) => s.workspace.id === this.scope) ?? null;
  }

  get globalStats(): AnalyticsSummary {
    if (this.crossWorkspaceAnalytics) {
      const a = this.crossWorkspaceAnalytics;
      return buildAnalyticsSummary(a.totalTasks, a.completedTasks, a.overdueTasks);
    }

    const total = this.workspaceStats.reduce((a, s) => a + s.analytics.totalTasks, 0);
    const done = this.workspaceStats.reduce((a, s) => a + s.analytics.completedTasks, 0);
    const overdue = this.workspaceStats.reduce((a, s) => a + s.analytics.overdueTasks, 0);
    return buildAnalyticsSummary(total, done, overdue);
  }

  get personalStats(): AnalyticsSummary {
    if (this.personalAnalytics) {
      const a = this.personalAnalytics;
      return buildAnalyticsSummary(a.assignedTasks, a.completedTasks, a.overdueTasks);
    }
    return buildAnalyticsSummary(0, 0, 0);
  }

  boardTotalTasks(board: BoardSummary): number {
    return getBoardTaskCount(board);
  }

  priorityColor(priority: BoardSummary['lists'][number]['previewTasks'][number]['priority']): string {
    return getPriorityDotClass(priority);
  }

  setScope(scope: AnalyticsScope): void {
    this.scope = scope;
    this.router.navigate(['/analytics'], { queryParams: { scope } });
  }

  private finishLoading(): void {
    this.isLoading = false;
    this.cdr.markForCheck();
  }
}
