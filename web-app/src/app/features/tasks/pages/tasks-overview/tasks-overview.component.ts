import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TaskService } from '../../../../core/services/task.service';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { BoardSummary, WorkspaceSummary } from '../../../../core/models/task.model';
import { Workspace } from '../../../../core/models/workspace.model';

interface WorkspaceBlock {
  workspace: Workspace;
  summary: WorkspaceSummary | null;
  loading: boolean;
}

@Component({
  selector: 'app-tasks-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tasks-overview.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TasksOverviewComponent implements OnInit {
  blocks: WorkspaceBlock[] = [];
  isLoading = true;

  constructor(
    private taskService: TaskService,
    private workspaceService: WorkspaceService,
    public router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.workspaceService.listMine().subscribe({
      next: (workspaces) => {
        if (workspaces.length === 0) {
          this.isLoading = false;
          this.cdr.markForCheck();
          return;
        }

        this.blocks = workspaces.map((ws) => ({
          workspace: ws,
          summary: null,
          loading: true,
        }));
        this.cdr.markForCheck();

        const ids = workspaces.map((ws) => ws.id);
        this.taskService.getBatchSummaries(ids).pipe(
          catchError(() => of([] as WorkspaceSummary[]))
        ).subscribe({
          next: (summaries) => {
            const summaryMap = new Map(summaries.map((s) => [s.workspaceId, s]));
            this.blocks = this.blocks.map((block) => ({
              ...block,
              summary: summaryMap.get(block.workspace.id) ?? null,
              loading: false,
            }));
            this.isLoading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.blocks = this.blocks.map((b) => ({ ...b, loading: false }));
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

  get totalTasks(): number {
    return this.blocks.reduce((acc, b) => {
      if (!b.summary) return acc;
      return acc + b.summary.boards.reduce((ba, board) =>
        ba + board.lists.reduce((la, list) => la + list.taskCount, 0), 0);
    }, 0);
  }

  boardTotalTasks(board: BoardSummary): number {
    return board.lists.reduce((acc, l) => acc + l.taskCount, 0);
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

  navigateToWorkspace(workspaceId: string) {
    this.router.navigate(['/tasks', workspaceId]);
  }
}
