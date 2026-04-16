import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TaskService } from '../../../../core/services/task.service';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { BoardSummary, WorkspaceSummary } from '../../../../core/models/task.model';
import { Workspace } from '../../../../core/models/workspace.model';
import { getBoardTaskCount, getPriorityDotClass } from '../../../../core/utils/task-board.util';

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
    private readonly taskService: TaskService,
    private readonly workspaceService: WorkspaceService,
    public router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.workspaceService.listMine().subscribe({
      next: (workspaces) => {
        if (workspaces.length === 0) {
          this.finishLoading();
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
          catchError(() => of([] as WorkspaceSummary[])),
        ).subscribe({
          next: (summaries) => {
            const summaryMap = new Map(summaries.map((s) => [s.workspaceId, s]));
            this.blocks = this.blocks.map((block) => ({
              ...block,
              summary: summaryMap.get(block.workspace.id) ?? null,
              loading: false,
            }));
            this.finishLoading();
          },
          error: () => {
            this.blocks = this.blocks.map((b) => ({ ...b, loading: false }));
            this.finishLoading();
          },
        });
      },
      error: () => {
        this.finishLoading();
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
    return getBoardTaskCount(board);
  }

  priorityColor(priority: BoardSummary['lists'][number]['previewTasks'][number]['priority']): string {
    return getPriorityDotClass(priority);
  }

  navigateToWorkspace(workspaceId: string): void {
    this.router.navigate(['/tasks', workspaceId]);
  }

  private finishLoading(): void {
    this.isLoading = false;
    this.cdr.markForCheck();
  }
}
