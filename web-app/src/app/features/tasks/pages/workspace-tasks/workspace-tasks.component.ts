import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TaskService } from '../../../../core/services/task.service';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import {
  Board,
  CreateBoardRequest,
  CreateTaskRequest,
  Task,
  TaskList,
} from '../../../../core/models/task.model';
import { Workspace, WorkspaceMember } from '../../../../core/models/workspace.model';

@Component({
  selector: 'app-workspace-tasks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-tasks.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceTasksComponent implements OnInit {
  workspaceId!: string;
  workspace: Workspace | null = null;
  boards: Board[] = [];
  members: WorkspaceMember[] = [];
  selectedBoard: Board | null = null;

  isLoading = true;
  boardsLoading = false;

  showCreateBoard = signal(false);
  showCreateList = signal(false);
  showCreateTask = signal<string | null>(null); // listId

  createBoardForm: FormGroup;
  createListForm: FormGroup;
  createTaskForm: FormGroup;

  selectedColor = '#6366f1';
  readonly PRESET_COLORS = [
    '#6366f1',
    '#3b82f6',
    '#10b981',
    '#f59e0b',
    '#ef4444',
    '#8b5cf6',
    '#ec4899',
    '#14b8a6',
  ];

  isCreatingBoard = false;
  isCreatingList = false;
  isCreatingTask = false;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private fb: FormBuilder,
    private taskService: TaskService,
    private workspaceService: WorkspaceService,
    private alertService: AlertService,
    public userProfileState: UserProfileStateService,
    private cdr: ChangeDetectorRef,
  ) {
    this.createBoardForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    });
    this.createListForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
    });
    this.createTaskForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(255)]],
      priority: ['MEDIUM'],
    });
  }

  ngOnInit() {
    this.workspaceId = this.route.snapshot.paramMap.get('workspaceId')!;

    forkJoin({
      workspace: this.workspaceService.getById(this.workspaceId),
      members: this.workspaceService.listMembers(this.workspaceId),
    }).subscribe({
      next: ({ workspace, members }) => {
        this.workspace = workspace;
        this.members = members;
        this.loadBoards();
      },
      error: () => this.router.navigate(['/tasks']),
    });
  }

  loadBoards() {
    this.boardsLoading = true;
    this.taskService.listBoards(this.workspaceId).subscribe({
      next: (boards) => {
        this.boards = boards;
        if (boards.length > 0 && !this.selectedBoard) {
          this.selectedBoard = boards[0];
        }
        this.boardsLoading = false;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.boardsLoading = false;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  selectBoard(board: Board) {
    this.selectedBoard = board;
    this.showCreateList.set(false);
    this.cdr.markForCheck();
  }

  get isOwnerOrAdmin(): boolean {
    const profile = this.userProfileState.profile();
    if (!profile) return false;
    const member = this.members.find((m) => m.authId === profile.authId);
    return member?.role === 'OWNER' || member?.role === 'ADMIN';
  }

  onCreateBoard() {
    if (this.createBoardForm.invalid || this.isCreatingBoard) return;
    this.isCreatingBoard = true;

    const req: CreateBoardRequest = {
      name: this.createBoardForm.value.name,
      color: this.selectedColor,
    };

    this.taskService.createBoard(this.workspaceId, req).subscribe({
      next: (board) => {
        this.boards = [...this.boards, { ...board, lists: [] }];
        this.selectedBoard = this.boards[this.boards.length - 1];
        this.showCreateBoard.set(false);
        this.createBoardForm.reset();
        this.selectedColor = '#6366f1';
        this.isCreatingBoard = false;
        this.alertService.success(`Board "${board.name}" created!`, 'Board created');
        this.cdr.markForCheck();
      },
      error: () => {
        this.isCreatingBoard = false;
        this.cdr.markForCheck();
      },
    });
  }

  onCreateList() {
    if (this.createListForm.invalid || !this.selectedBoard || this.isCreatingList) return;
    this.isCreatingList = true;

    this.taskService
      .createList(this.workspaceId, this.selectedBoard.id, {
        name: this.createListForm.value.name,
      })
      .subscribe({
        next: (list) => {
          if (this.selectedBoard) {
            this.selectedBoard = {
              ...this.selectedBoard,
              lists: [...this.selectedBoard.lists, { ...list, tasks: [] }],
            };
            this.boards = this.boards.map((b) =>
              b.id === this.selectedBoard!.id ? this.selectedBoard! : b,
            );
          }
          this.showCreateList.set(false);
          this.createListForm.reset();
          this.isCreatingList = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.isCreatingList = false;
          this.cdr.markForCheck();
        },
      });
  }

  onCreateTask(listId: string) {
    if (this.createTaskForm.invalid || this.isCreatingTask) return;
    this.isCreatingTask = true;

    const req: CreateTaskRequest = {
      title: this.createTaskForm.value.title,
      priority: this.createTaskForm.value.priority,
    };

    this.taskService.createTask(this.workspaceId, listId, req).subscribe({
      next: (task) => {
        if (this.selectedBoard) {
          this.selectedBoard = {
            ...this.selectedBoard,
            lists: this.selectedBoard.lists.map((l) =>
              l.id === listId ? { ...l, tasks: [...l.tasks, task] } : l,
            ),
          };
          this.boards = this.boards.map((b) =>
            b.id === this.selectedBoard!.id ? this.selectedBoard! : b,
          );
        }
        this.showCreateTask.set(null);
        this.createTaskForm.reset({ priority: 'MEDIUM' });
        this.isCreatingTask = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isCreatingTask = false;
        this.cdr.markForCheck();
      },
    });
  }

  onDeleteTask(task: Task) {
    if (!confirm(`Delete "${task.title}"?`)) return;
    this.taskService.deleteTask(this.workspaceId, task.id).subscribe({
      next: () => {
        if (this.selectedBoard) {
          this.selectedBoard = {
            ...this.selectedBoard,
            lists: this.selectedBoard.lists.map((l) => ({
              ...l,
              tasks: l.tasks.filter((t) => t.id !== task.id),
            })),
          };
          this.boards = this.boards.map((b) =>
            b.id === this.selectedBoard!.id ? this.selectedBoard! : b,
          );
        }
        this.cdr.markForCheck();
      },
    });
  }

  onDeleteBoard(board: Board) {
    if (!confirm(`Delete board "${board.name}"? All lists and tasks will be removed.`)) return;
    this.taskService.deleteBoard(this.workspaceId, board.id).subscribe({
      next: () => {
        this.boards = this.boards.filter((b) => b.id !== board.id);
        this.selectedBoard = this.boards.length > 0 ? this.boards[0] : null;
        this.alertService.success(`Board "${board.name}" deleted.`, 'Deleted');
        this.cdr.markForCheck();
      },
    });
  }

  onDeleteList(list: TaskList) {
    if (!confirm(`Delete list "${list.name}"?`)) return;
    this.taskService.deleteList(this.workspaceId, this.selectedBoard!.id, list.id).subscribe({
      next: () => {
        if (this.selectedBoard) {
          this.selectedBoard = {
            ...this.selectedBoard,
            lists: this.selectedBoard.lists.filter((l) => l.id !== list.id),
          };
          this.boards = this.boards.map((b) =>
            b.id === this.selectedBoard!.id ? this.selectedBoard! : b,
          );
        }
        this.cdr.markForCheck();
      },
    });
  }

  priorityBadge(priority: string): string {
    const map: Record<string, string> = {
      URGENT: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
      HIGH: 'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400',
      MEDIUM: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
      LOW: 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400',
    };
    return map[priority] ?? map['MEDIUM'];
  }

  isDueSoon(dueDate: string | null): boolean {
    if (!dueDate) return false;
    const d = new Date(dueDate);
    const now = new Date();
    const diff = (d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    return diff <= 2 && diff >= 0;
  }

  isOverdue(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }
}
