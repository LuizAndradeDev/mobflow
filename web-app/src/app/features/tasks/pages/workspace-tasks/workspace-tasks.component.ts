import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  signal,
} from '@angular/core';
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
  UpdateTaskRequest,
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
  activeTask = signal<Task | null>(null);
  createTaskListId = signal<string | null>(null);

  createBoardForm: FormGroup;
  createListForm: FormGroup;
  createTaskForm: FormGroup;
  editTaskForm: FormGroup;

  selectedColor = '#6366f1';
  readonly PRESET_COLORS = [
    '#6366f1', '#3b82f6', '#10b981', '#f59e0b',
    '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6',
  ];

  isCreatingBoard = false;
  isCreatingList = false;
  isCreatingTask = false;
  isSavingTask = false;

  dragTask: Task | null = null;
  dragSourceListId: string | null = null;
  dragOverListId = signal<string | null>(null);
  dragOverIndex = signal<number>(-1);

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
      description: [''],
      priority: ['MEDIUM'],
      assigneeAuthId: [null],
      dueDate: [null],
    });
    this.editTaskForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(255)]],
      description: [''],
      priority: ['MEDIUM'],
      assigneeAuthId: [null],
      dueDate: [null],
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
        if (boards.length > 0 && !this.selectedBoard) this.selectedBoard = boards[0];
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

  memberById(authId: string | null): WorkspaceMember | undefined {
    if (!authId) return undefined;
    return this.members.find((m) => m.authId === authId);
  }

  // ── Create Board ──
  onCreateBoard() {
    if (this.createBoardForm.invalid || this.isCreatingBoard) return;
    this.isCreatingBoard = true;
    const req: CreateBoardRequest = { name: this.createBoardForm.value.name, color: this.selectedColor };
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
      error: () => { this.isCreatingBoard = false; this.cdr.markForCheck(); },
    });
  }

  // ── Create List ──
  onCreateList() {
    if (this.createListForm.invalid || !this.selectedBoard || this.isCreatingList) return;
    this.isCreatingList = true;
    this.taskService.createList(this.workspaceId, this.selectedBoard.id, { name: this.createListForm.value.name }).subscribe({
      next: (list) => {
        if (this.selectedBoard) {
          this.selectedBoard = { ...this.selectedBoard, lists: [...this.selectedBoard.lists, { ...list, tasks: [] }] };
          this.boards = this.boards.map((b) => b.id === this.selectedBoard!.id ? this.selectedBoard! : b);
        }
        this.showCreateList.set(false);
        this.createListForm.reset();
        this.isCreatingList = false;
        this.cdr.markForCheck();
      },
      error: () => { this.isCreatingList = false; this.cdr.markForCheck(); },
    });
  }

  // ── Create Task Modal ──
  openCreateTaskModal(listId: string) {
    this.createTaskForm.reset({ priority: 'MEDIUM' });
    this.createTaskListId.set(listId);
  }

  closeCreateTaskModal() {
    this.createTaskListId.set(null);
  }

  onCreateTask() {
    const listId = this.createTaskListId();
    if (this.createTaskForm.invalid || this.isCreatingTask || !listId) return;
    this.isCreatingTask = true;
    const v = this.createTaskForm.value;
    const req: CreateTaskRequest = {
      title: v.title,
      description: v.description || undefined,
      priority: v.priority,
      assigneeAuthId: v.assigneeAuthId || undefined,
      dueDate: v.dueDate || undefined,
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
          this.boards = this.boards.map((b) => b.id === this.selectedBoard!.id ? this.selectedBoard! : b);
        }
        this.createTaskListId.set(null);
        this.isCreatingTask = false;
        this.cdr.markForCheck();
      },
      error: () => { this.isCreatingTask = false; this.cdr.markForCheck(); },
    });
  }

  // ── Task Detail Modal ──
  openTaskDetail(task: Task) {
    this.editTaskForm.patchValue({
      title: task.title,
      description: task.description ?? '',
      priority: task.priority,
      assigneeAuthId: task.assigneeAuthId ?? null,
      dueDate: task.dueDate ? task.dueDate.substring(0, 10) : null,
    });
    this.activeTask.set(task);
  }

  closeTaskDetail() {
    this.activeTask.set(null);
  }

  onSaveTask() {
    const task = this.activeTask();
    if (!task || this.editTaskForm.invalid || this.isSavingTask) return;
    this.isSavingTask = true;
    const v = this.editTaskForm.value;
    const req: UpdateTaskRequest = {
      title: v.title,
      description: v.description || undefined,
      priority: v.priority,
      assigneeAuthId: v.assigneeAuthId || undefined,
      dueDate: v.dueDate || undefined,
    };
    this.taskService.updateTask(this.workspaceId, task.id, req).subscribe({
      next: (updated) => {
        this._replaceTaskInState(updated);
        this.activeTask.set(updated);
        this.isSavingTask = false;
        this.alertService.success('Task updated.', 'Saved');
        this.cdr.markForCheck();
      },
      error: () => { this.isSavingTask = false; this.cdr.markForCheck(); },
    });
  }

  // ── Complete Task: alterna status entre COMPLETED e TODO via updateTask ──
  onToggleComplete(task: Task) {
    const profile = this.userProfileState.profile();
    if (!profile) return;

    const isCompleted = task.status === 'COMPLETED';
    const req: UpdateTaskRequest = isCompleted
      ? { status: 'TODO', completedByAuthId: undefined }
      : { status: 'COMPLETED', completedByAuthId: profile.authId };

    this.taskService.updateTask(this.workspaceId, task.id, req).subscribe({
      next: (updated) => {
        this._replaceTaskInState(updated);
        if (this.activeTask()?.id === task.id) this.activeTask.set(updated);
        this.cdr.markForCheck();
      },
      error: () => {
        this.alertService.danger('Could not update task status.', 'Error');
        this.cdr.markForCheck();
      },
    });
  }

  // ── Delete ──
  onDeleteTask(task: Task) {
    if (!confirm(`Delete "${task.title}"?`)) return;
    this.taskService.deleteTask(this.workspaceId, task.id).subscribe({
      next: () => {
        if (this.selectedBoard) {
          this.selectedBoard = {
            ...this.selectedBoard,
            lists: this.selectedBoard.lists.map((l) => ({ ...l, tasks: l.tasks.filter((t) => t.id !== task.id) })),
          };
          this.boards = this.boards.map((b) => b.id === this.selectedBoard!.id ? this.selectedBoard! : b);
        }
        if (this.activeTask()?.id === task.id) this.activeTask.set(null);
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
          this.selectedBoard = { ...this.selectedBoard, lists: this.selectedBoard.lists.filter((l) => l.id !== list.id) };
          this.boards = this.boards.map((b) => b.id === this.selectedBoard!.id ? this.selectedBoard! : b);
        }
        this.cdr.markForCheck();
      },
    });
  }

  // ── Drag & Drop ──
  onDragStart(event: DragEvent, task: Task, listId: string) {
    this.dragTask = task;
    this.dragSourceListId = listId;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', task.id);
    }
  }

  onDragEnd() {
    this.dragTask = null;
    this.dragSourceListId = null;
    this.dragOverListId.set(null);
    this.dragOverIndex.set(-1);
  }

  onDragOver(event: DragEvent, listId: string, index: number) {
    event.preventDefault();
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
    this.dragOverListId.set(listId);
    this.dragOverIndex.set(index);
  }

  onDrop(event: DragEvent, targetListId: string, position: number) {
    event.preventDefault();
    const task = this.dragTask;
    const sourceListId = this.dragSourceListId;
    if (!task || !sourceListId || !this.selectedBoard) return;
    this.dragOverListId.set(null);
    this.dragOverIndex.set(-1);
    this.taskService.moveTask(this.workspaceId, task.id, { targetListId, position }).subscribe({
      next: (moved) => {
        this._moveTaskInState(task, sourceListId, targetListId, position, moved);
        this.cdr.markForCheck();
      },
    });
  }

  onDropOnList(event: DragEvent, targetListId: string) {
    event.preventDefault();
    const task = this.dragTask;
    const sourceListId = this.dragSourceListId;
    if (!task || !sourceListId || !this.selectedBoard) return;
    const targetList = this.selectedBoard.lists.find((l) => l.id === targetListId);
    const position = targetList ? targetList.tasks.length : 0;
    this.dragOverListId.set(null);
    this.dragOverIndex.set(-1);
    this.taskService.moveTask(this.workspaceId, task.id, { targetListId, position }).subscribe({
      next: (moved) => {
        this._moveTaskInState(task, sourceListId, targetListId, position, moved);
        this.cdr.markForCheck();
      },
    });
  }

  // ── Helpers ──

  /** Task está concluída quando seu status é COMPLETED */
  isTaskCompleted(task: Task): boolean {
    return task.status === 'COMPLETED';
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
    const diff = (new Date(dueDate).getTime() - Date.now()) / 86_400_000;
    return diff <= 2 && diff >= 0;
  }

  isOverdue(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }

  private _replaceTaskInState(updated: Task) {
    if (!this.selectedBoard) return;
    this.selectedBoard = {
      ...this.selectedBoard,
      lists: this.selectedBoard.lists.map((l) => ({
        ...l,
        tasks: l.tasks.map((t) => (t.id === updated.id ? updated : t)),
      })),
    };
    this.boards = this.boards.map((b) => b.id === this.selectedBoard!.id ? this.selectedBoard! : b);
  }

  private _moveTaskInState(task: Task, sourceListId: string, targetListId: string, position: number, updated: Task) {
    if (!this.selectedBoard) return;
    this.selectedBoard = {
      ...this.selectedBoard,
      lists: this.selectedBoard.lists.map((l) => {
        if (l.id === sourceListId) return { ...l, tasks: l.tasks.filter((t) => t.id !== task.id) };
        if (l.id === targetListId) {
          const tasks = l.tasks.filter((t) => t.id !== task.id);
          tasks.splice(position, 0, updated);
          return { ...l, tasks };
        }
        return l;
      }),
    };
    this.boards = this.boards.map((b) => b.id === this.selectedBoard!.id ? this.selectedBoard! : b);
  }
}
