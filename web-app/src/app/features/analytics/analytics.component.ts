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
import { BoardSummary, TaskAnalytics, WorkspaceSummary } from '../../core/models/task.model';

interface WorkspaceStats {
  workspace: Workspace;
  summary: WorkspaceSummary | null;
  analytics: TaskAnalytics;
  /** Número de tasks em andamento (totalTasks - completedTasks - overdueTasks, mínimo 0) */
  inProgressTasks: number;
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

  /** Analytics pessoais do usuário (via endpoint /user/{authId}) */
  personalAnalytics: TaskAnalytics | null = null;

  /** Analytics do usuário em todos os workspaces (via endpoint /user/{authId}/workspaces) */
  crossWorkspaceAnalytics: TaskAnalytics | null = null;

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
    const profile = this.userProfileState.profile();
    if (!profile) {
      this.isLoading = false;
      this.cdr.markForCheck();
      return;
    }

    const authId = profile.authId;

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
          // Summaries ainda são necessárias para o breakdown visual de boards/listas
          summaries: this.taskService.getBatchSummaries(ids).pipe(
            catchError(() => of([] as WorkspaceSummary[])),
          ),
          // Analytics por workspace para o usuário atual
          workspaceAnalytics: forkJoin(
            workspaces.map((w) =>
              this.taskService.getWorkspaceAnalytics(w.id, authId).pipe(
                catchError(() => of<TaskAnalytics>({
                  totalTasks: 0, completedTasks: 0, createdTasks: 0,
                  assignedTasks: 0, overdueTasks: 0,
                })),
              ),
            ),
          ),
          // Contagem de membros por workspace
          memberCounts: forkJoin(
            workspaces.map((w) =>
              this.workspaceService.listMembers(w.id).pipe(catchError(() => of([]))),
            ),
          ),
          // Analytics pessoais globais do usuário
          personalAnalytics: this.taskService.getUserAnalytics(authId).pipe(
            catchError(() => of<TaskAnalytics>({
              totalTasks: 0, completedTasks: 0, createdTasks: 0,
              assignedTasks: 0, overdueTasks: 0,
            })),
          ),
          // Analytics do usuário em todos os workspaces
          crossWorkspaceAnalytics: this.taskService.getUserAnalyticsAcrossWorkspaces(authId, ids).pipe(
            catchError(() => of<TaskAnalytics>({
              totalTasks: 0, completedTasks: 0, createdTasks: 0,
              assignedTasks: 0, overdueTasks: 0,
            })),
          ),
        }).subscribe({
          next: ({ summaries, workspaceAnalytics, memberCounts, personalAnalytics, crossWorkspaceAnalytics }) => {
            const summaryMap = new Map(summaries.map((s) => [s.workspaceId, s]));

            this.personalAnalytics = personalAnalytics;
            this.crossWorkspaceAnalytics = crossWorkspaceAnalytics;

            this.workspaceStats = workspaces.map((ws, i) => {
              const summary = summaryMap.get(ws.id) ?? null;
              const analytics = workspaceAnalytics[i];
              const members = (memberCounts[i] as any[]).length;

              const inProgressTasks = Math.max(
                0,
                analytics.totalTasks - analytics.completedTasks - analytics.overdueTasks,
              );
              const completionPct =
                analytics.totalTasks > 0
                  ? Math.round((analytics.completedTasks / analytics.totalTasks) * 100)
                  : 0;

              return {
                workspace: ws,
                summary,
                analytics,
                inProgressTasks,
                completionPct,
                members,
              };
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

  /**
   * Estatísticas globais (escopo "team"): agrega analytics de todos os workspaces.
   * Usa crossWorkspaceAnalytics quando disponível; caso contrário agrega localmente.
   */
  get globalStats() {
    if (this.crossWorkspaceAnalytics) {
      const a = this.crossWorkspaceAnalytics;
      const pct = a.totalTasks > 0 ? Math.round((a.completedTasks / a.totalTasks) * 100) : 0;
      return {
        total: a.totalTasks,
        done: a.completedTasks,
        inProgress: Math.max(0, a.totalTasks - a.completedTasks - a.overdueTasks),
        overdue: a.overdueTasks,
        pct,
      };
    }
    // Fallback: agrega localmente a partir dos stats por workspace
    const total = this.workspaceStats.reduce((a, s) => a + s.analytics.totalTasks, 0);
    const done = this.workspaceStats.reduce((a, s) => a + s.analytics.completedTasks, 0);
    const overdue = this.workspaceStats.reduce((a, s) => a + s.analytics.overdueTasks, 0);
    const inProgress = Math.max(0, total - done - overdue);
    const pct = total > 0 ? Math.round((done / total) * 100) : 0;
    return { total, done, inProgress, overdue, pct };
  }

  /**
   * Estatísticas pessoais (escopo "personal"): usa o endpoint /user/{authId}
   * que retorna tasks criadas, atribuídas e concluídas pelo usuário.
   */
  get personalStats() {
    if (this.personalAnalytics) {
      const a = this.personalAnalytics;
      const total = a.assignedTasks;
      const done = a.completedTasks;
      const overdue = a.overdueTasks;
      const inProgress = Math.max(0, total - done - overdue);
      const pct = total > 0 ? Math.round((done / total) * 100) : 0;
      return { total, done, inProgress, overdue, pct };
    }
    return { total: 0, done: 0, inProgress: 0, overdue: 0, pct: 0 };
  }

  boardTotalTasks(board: BoardSummary): number {
    return board.lists.reduce((a, l) => a + l.taskCount, 0);
  }

  priorityColor(priority: string): string {
    const map: Record<string, string> = {
      URGENT: 'bg-red-500',
      HIGH: 'bg-orange-400',
      MEDIUM: 'bg-blue-400',
      LOW: 'bg-gray-300 dark:bg-gray-600',
    };
    return map[priority] ?? 'bg-gray-300';
  }

  setScope(s: string) {
    this.router.navigate(['/analytics'], { queryParams: { scope: s } });
  }
}
