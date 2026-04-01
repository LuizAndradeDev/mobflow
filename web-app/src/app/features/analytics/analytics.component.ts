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
import { Workspace } from '../../core/models/workspace.model';
import { WorkspaceSummary, BoardSummary } from '../../core/models/task.model';

interface WorkspaceStats {
  workspace: Workspace;
  summary: WorkspaceSummary | null;
  totalTasks: number;
  doneTasks: number;
  inProgressTasks: number;
  overdueTasks: number;
  completionPct: number;
  members: number;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './analytics.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnalyticsComponent implements OnInit {
  /** 'personal' | 'team' | workspaceId */
  scope = 'personal';

  workspaces: Workspace[] = [];
  workspaceStats: WorkspaceStats[] = [];
  isLoading = true;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private workspaceService: WorkspaceService,
    private taskService: TaskService,
    public userProfileState: UserProfileStateService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe((p) => {
      this.scope = p['scope'] ?? 'personal';
      this.cdr.markForCheck();
    });

    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    this.workspaceService.listMine().subscribe({
      next: (workspaces) => {
        this.workspaces = workspaces;
        if (workspaces.length === 0) {
          this.isLoading = false;
          this.cdr.markForCheck();
          return;
        }

        const ids = workspaces.map((w) => w.id);

        forkJoin({
          summaries: this.taskService.getBatchSummaries(ids).pipe(catchError(() => of([] as WorkspaceSummary[]))),
          memberCounts: forkJoin(
            workspaces.map((w) =>
              this.workspaceService.listMembers(w.id).pipe(catchError(() => of([]))),
            ),
          ),
        }).subscribe({
          next: ({ summaries, memberCounts }) => {
            const summaryMap = new Map(summaries.map((s) => [s.workspaceId, s]));

            this.workspaceStats = workspaces.map((ws, i) => {
              const summary = summaryMap.get(ws.id) ?? null;
              const members = (memberCounts[i] as any[]).length;

              let totalTasks = 0;
              let doneTasks = 0;
              let inProgressTasks = 0;
              let overdueTasks = 0;

              if (summary) {
                summary.boards.forEach((board) => {
                  const lists = board.lists;
                  lists.forEach((list, idx) => {
                    const isDone = idx === lists.length - 1 && lists.length > 1;
                    totalTasks += list.taskCount;
                    if (isDone) {
                      doneTasks += list.taskCount;
                    } else if (idx === 0) {
                      // first list = todo, rest = in progress
                    } else {
                      inProgressTasks += list.taskCount;
                    }
                    // Count overdue from preview tasks
                    list.previewTasks.forEach((t) => {
                      if (t.dueDate && new Date(t.dueDate) < new Date()) overdueTasks++;
                    });
                  });
                });
              }

              const completionPct = totalTasks > 0 ? Math.round((doneTasks / totalTasks) * 100) : 0;

              return { workspace: ws, summary, totalTasks, doneTasks, inProgressTasks, overdueTasks, completionPct, members };
            });

            this.isLoading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.isLoading = false;
            this.cdr.markForCheck();
          },
        });
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get selectedWorkspaceStats(): WorkspaceStats | null {
    if (this.scope === 'personal' || this.scope === 'team') return null;
    return this.workspaceStats.find((s) => s.workspace.id === this.scope) ?? null;
  }

  get globalStats() {
    const total = this.workspaceStats.reduce((a, s) => a + s.totalTasks, 0);
    const done = this.workspaceStats.reduce((a, s) => a + s.doneTasks, 0);
    const inProgress = this.workspaceStats.reduce((a, s) => a + s.inProgressTasks, 0);
    const overdue = this.workspaceStats.reduce((a, s) => a + s.overdueTasks, 0);
    const pct = total > 0 ? Math.round((done / total) * 100) : 0;
    return { total, done, inProgress, overdue, pct };
  }

  get personalStats() {
    const profile = this.userProfileState.profile();
    // Personal = tasks assigned to the current user across all workspaces
    // Since the summary doesn't include assignee info, we show workspace-level overview
    // as personal scope for now (real implementation needs a backend endpoint)
    return this.globalStats;
  }

  boardTotalTasks(board: BoardSummary): number {
    return board.lists.reduce((a, l) => a + l.taskCount, 0);
  }

  priorityColor(priority: string): string {
    const map: Record<string, string> = {
      URGENT: 'bg-red-500', HIGH: 'bg-orange-400', MEDIUM: 'bg-blue-400', LOW: 'bg-gray-300 dark:bg-gray-600',
    };
    return map[priority] ?? 'bg-gray-300';
  }

  setScope(s: string) {
    this.router.navigate(['/analytics'], { queryParams: { scope: s } });
  }
}
