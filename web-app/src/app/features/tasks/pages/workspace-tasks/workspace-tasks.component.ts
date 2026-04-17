import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TaskService } from '../../../../core/services/task.service';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { TaskCommentsComponent } from '../../components/task-comments/task-comments.component';
import {
  Board,
  CreateBoardRequest,
  CreateTaskRequest,
  Task,
  TaskList,
  TaskPriority,
  UpdateTaskRequest,
} from '../../../../core/models/task.model';
import { Workspace, WorkspaceMember } from '../../../../core/models/workspace.model';

interface CreateBoardForm {
  name: FormControl<string>;
}

interface CreateListForm {
  name: FormControl<string>;
}

interface TaskForm {
  title: FormControl<string>;
  description: FormControl<string>;
  priority: FormControl<TaskPriority>;
  assigneeAuthId: FormControl<string | null>;
  dueDate: FormControl<string | null>;
}

@Component({
  selector: 'app-workspace-tasks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TaskCommentsComponent],
  templateUrl: './workspace-tasks.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceTasksComponent implements OnInit {
  private readonly defaultBoardColor = '#6366f1';
  private readonly defaultTaskPriority: TaskPriority = 'MEDIUM';

  workspaceId!: string;
  workspace: Workspace | null = null;
  boards: Board[] = [];
  members: WorkspaceMember[] = [];
  selectedBoard: Board | null = null;

  isLoading = true;
  boardsLoading = false;

  readonly showCreateBoard = signal(false);
  readonly showCreateList = signal(false);
  readonly activeTask = signal<Task | null>(null);
  readonly createTaskListId = signal<string | null>(null);

  readonly createBoardForm: FormGroup<CreateBoardForm>;
  readonly createListForm: FormGroup<CreateListForm>;
  readonly createTaskForm: FormGroup<TaskForm>;
  readonly editTaskForm: FormGroup<TaskForm>;

  selectedColor = this.defaultBoardColor;
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
  readonly dragOverListId = signal<string | null>(null);
  readonly dragOverIndex = signal(-1);
  highlightedCommentId: string | null = null;

  private pendingBoardId: string | null = null;
  private pendingTaskId: string | null = null;
  private pendingCommentId: string | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    public router: Router,
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly taskService: TaskService,
    private readonly workspaceService: WorkspaceService,
    private readonly alertService: AlertService,
    public userProfileState: UserProfileStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.createBoardForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    });
    this.createListForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
    });
    this.createTaskForm = this.buildTaskForm();
    this.editTaskForm = this.buildTaskForm();
  }

  ngOnInit(): void {
    this.workspaceId = this.route.snapshot.paramMap.get('workspaceId')!;
    this.route.queryParamMap.subscribe((params) => {
      this.pendingBoardId = params.get('boardId');
      this.pendingTaskId = params.get('taskId');
      this.pendingCommentId = params.get('commentId');
      this.applyRouteTaskSelection();
    });

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

  loadBoards(): void {
    this.boardsLoading = true;
    this.taskService.listBoards(this.workspaceId).subscribe({
      next: (boards) => {
        this.boards = boards;
        this.selectedBoard ??= boards[0] ?? null;
        this.applyRouteTaskSelection();
        this.finishBoardLoading();
      },
      error: () => {
        this.finishBoardLoading();
      },
    });
  }

  selectBoard(board: Board): void {
    this.selectedBoard = board;
    this.showCreateList.set(false);
    this.cdr.markForCheck();
  }

  get isOwnerOrAdmin(): boolean {
    const profile = this.userProfileState.profile();
    if (!profile) {
      return false;
    }

    const member = this.members.find(({ authId }) => authId === profile.authId);
    return member?.role === 'OWNER' || member?.role === 'ADMIN';
  }

  memberById(authId: string | null): WorkspaceMember | undefined {
    return authId ? this.members.find((member) => member.authId === authId) : undefined;
  }

  onCreateBoard(): void {
    if (this.createBoardForm.invalid || this.isCreatingBoard) {
      return;
    }

    this.isCreatingBoard = true;
    this.taskService.createBoard(this.workspaceId, this.buildCreateBoardRequest()).subscribe({
      next: (board) => {
        this.boards = [...this.boards, { ...board, lists: [] }];
        this.selectedBoard = this.boards.at(-1) ?? null;
        this.showCreateBoard.set(false);
        this.createBoardForm.reset({ name: '' });
        this.selectedColor = this.defaultBoardColor;
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

  onCreateList(): void {
    if (this.createListForm.invalid || !this.selectedBoard || this.isCreatingList) {
      return;
    }

    this.isCreatingList = true;
    this.taskService.createList(
      this.workspaceId,
      this.selectedBoard.id,
      { name: this.createListForm.getRawValue().name },
    ).subscribe({
      next: (list) => {
        this.updateSelectedBoard((board) => ({
          ...board,
          lists: [...board.lists, { ...list, tasks: [] }],
        }));
        this.showCreateList.set(false);
        this.createListForm.reset({ name: '' });
        this.isCreatingList = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isCreatingList = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreateTaskModal(listId: string): void {
    this.resetTaskForm(this.createTaskForm);
    this.createTaskListId.set(listId);
  }

  closeCreateTaskModal(): void {
    this.createTaskListId.set(null);
  }

  onCreateTask(): void {
    const listId = this.createTaskListId();
    if (this.createTaskForm.invalid || this.isCreatingTask || !listId) {
      return;
    }

    this.isCreatingTask = true;
    this.taskService.createTask(
      this.workspaceId,
      listId,
      this.buildTaskRequest(this.createTaskForm),
    ).subscribe({
      next: (task) => {
        this.updateSelectedBoard((board) => ({
          ...board,
          lists: board.lists.map((list) =>
            list.id === listId ? { ...list, tasks: [...list.tasks, task] } : list,
          ),
        }));
        this.createTaskListId.set(null);
        this.isCreatingTask = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isCreatingTask = false;
        this.cdr.markForCheck();
      },
    });
  }

  openTaskDetail(task: Task, highlightedCommentId: string | null = null): void {
    this.editTaskForm.patchValue({
      title: task.title,
      description: task.description ?? '',
      priority: task.priority,
      assigneeAuthId: task.assigneeAuthId,
      dueDate: task.dueDate ? task.dueDate.substring(0, 10) : null,
    });
    this.highlightedCommentId = highlightedCommentId;
    this.activeTask.set(task);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        boardId: this.selectedBoard?.id ?? null,
        taskId: task.id,
        commentId: highlightedCommentId,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  closeTaskDetail(): void {
    this.highlightedCommentId = null;
    this.activeTask.set(null);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        taskId: null,
        commentId: null,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  onSaveTask(): void {
    const task = this.activeTask();
    if (!task || this.editTaskForm.invalid || this.isSavingTask) {
      return;
    }

    this.isSavingTask = true;
    this.taskService.updateTask(
      this.workspaceId,
      task.id,
      this.buildTaskRequest(this.editTaskForm),
    ).subscribe({
      next: (updated) => {
        this.replaceTaskInState(updated);
        this.activeTask.set(updated);
        this.isSavingTask = false;
        this.alertService.success('Task updated.', 'Saved');
        this.cdr.markForCheck();
      },
      error: () => {
        this.isSavingTask = false;
        this.cdr.markForCheck();
      },
    });
  }

  onToggleComplete(task: Task): void {
    const profile = this.userProfileState.profile();
    if (!profile) {
      return;
    }

    const request: UpdateTaskRequest = task.status === 'COMPLETED'
      ? { status: 'TODO', completedByAuthId: undefined }
      : { status: 'COMPLETED', completedByAuthId: profile.authId };

    this.taskService.updateTask(this.workspaceId, task.id, request).subscribe({
      next: (updated) => {
        this.replaceTaskInState(updated);
        if (this.activeTask()?.id === task.id) {
          this.activeTask.set(updated);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.alertService.danger('Could not update task status.', 'Error');
        this.cdr.markForCheck();
      },
    });
  }

  onDeleteTask(task: Task): void {
    if (!confirm(`Delete "${task.title}"?`)) {
      return;
    }

    this.taskService.deleteTask(this.workspaceId, task.id).subscribe({
      next: () => {
        this.updateSelectedBoard((board) => ({
          ...board,
          lists: board.lists.map((list) => ({
            ...list,
            tasks: list.tasks.filter(({ id }) => id !== task.id),
          })),
        }));
        if (this.activeTask()?.id === task.id) {
          this.activeTask.set(null);
        }
        this.cdr.markForCheck();
      },
    });
  }

  onDeleteBoard(board: Board): void {
    if (!confirm(`Delete board "${board.name}"? All lists and tasks will be removed.`)) {
      return;
    }

    this.taskService.deleteBoard(this.workspaceId, board.id).subscribe({
      next: () => {
        this.boards = this.boards.filter(({ id }) => id !== board.id);
        this.selectedBoard = this.boards[0] ?? null;
        this.alertService.success(`Board "${board.name}" deleted.`, 'Deleted');
        this.cdr.markForCheck();
      },
    });
  }

  onDeleteList(list: TaskList): void {
    if (!this.selectedBoard || !confirm(`Delete list "${list.name}"?`)) {
      return;
    }

    this.taskService.deleteList(this.workspaceId, this.selectedBoard.id, list.id).subscribe({
      next: () => {
        this.updateSelectedBoard((board) => ({
          ...board,
          lists: board.lists.filter(({ id }) => id !== list.id),
        }));
        this.cdr.markForCheck();
      },
    });
  }

  onDragStart(event: DragEvent, task: Task, listId: string): void {
    this.dragTask = task;
    this.dragSourceListId = listId;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', task.id);
    }
  }

  onDragEnd(): void {
    this.dragTask = null;
    this.dragSourceListId = null;
    this.dragOverListId.set(null);
    this.dragOverIndex.set(-1);
  }

  onDragOver(event: DragEvent, listId: string, index: number): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
    this.dragOverListId.set(listId);
    this.dragOverIndex.set(index);
  }

  onDrop(event: DragEvent, targetListId: string, position: number): void {
    event.preventDefault();
    this.moveDraggedTask(targetListId, position);
  }

  onDropOnList(event: DragEvent, targetListId: string): void {
    event.preventDefault();
    if (!this.selectedBoard) {
      return;
    }

    const targetList = this.selectedBoard.lists.find(({ id }) => id === targetListId);
    this.moveDraggedTask(targetListId, targetList?.tasks.length ?? 0);
  }

  isTaskCompleted(task: Task): boolean {
    return task.status === 'COMPLETED';
  }

  priorityBadge(priority: TaskPriority): string {
    const classes: Record<TaskPriority, string> = {
      URGENT: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
      HIGH: 'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400',
      MEDIUM: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
      LOW: 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400',
    };
    return classes[priority];
  }

  isDueSoon(dueDate: string | null): boolean {
    if (!dueDate) {
      return false;
    }

    const diffInDays = (new Date(dueDate).getTime() - Date.now()) / 86_400_000;
    return diffInDays <= 2 && diffInDays >= 0;
  }

  isOverdue(dueDate: string | null): boolean {
    return dueDate ? new Date(dueDate) < new Date() : false;
  }

  private buildTaskForm(): FormGroup<TaskForm> {
    return new FormGroup<TaskForm>({
      title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(1), Validators.maxLength(255)] }),
      description: new FormControl('', { nonNullable: true }),
      priority: new FormControl(this.defaultTaskPriority, { nonNullable: true }),
      assigneeAuthId: new FormControl<string | null>(null),
      dueDate: new FormControl<string | null>(null),
    });
  }

  private finishBoardLoading(): void {
    this.boardsLoading = false;
    this.isLoading = false;
    this.cdr.markForCheck();
  }

  private buildCreateBoardRequest(): CreateBoardRequest {
    const { name } = this.createBoardForm.getRawValue();
    return { name, color: this.selectedColor };
  }

  private buildTaskRequest(form: FormGroup<TaskForm>): CreateTaskRequest {
    const { assigneeAuthId, description, dueDate, priority, title } = form.getRawValue();
    return {
      title,
      description: description || undefined,
      priority,
      assigneeAuthId: assigneeAuthId || undefined,
      dueDate: dueDate || undefined,
    };
  }

  private resetTaskForm(form: FormGroup<TaskForm>): void {
    form.reset({
      title: '',
      description: '',
      priority: this.defaultTaskPriority,
      assigneeAuthId: null,
      dueDate: null,
    });
  }

  private replaceTaskInState(updatedTask: Task): void {
    this.updateSelectedBoard((board) => ({
      ...board,
      lists: board.lists.map((list) => ({
        ...list,
        tasks: list.tasks.map((task) => (task.id === updatedTask.id ? updatedTask : task)),
      })),
    }));
  }

  private applyRouteTaskSelection(): void {
    if (!this.boards.length) {
      return;
    }

    if (this.pendingBoardId) {
      const board = this.boards.find(({ id }) => id === this.pendingBoardId);
      if (board) {
        this.selectedBoard = board;
      }
    }

    if (!this.pendingTaskId) {
      return;
    }

    const task = this.findTaskAcrossBoards(this.pendingTaskId);
    if (!task) {
      return;
    }

    this.openTaskDetail(task, this.pendingCommentId);
    this.pendingTaskId = null;
    this.pendingCommentId = null;
  }

  private findTaskAcrossBoards(taskId: string): Task | null {
    for (const board of this.boards) {
      const task = this.findTaskById(board, taskId);
      if (task) {
        this.selectedBoard = board;
        return task;
      }
    }

    return null;
  }

  private findTaskById(board: Board, taskId: string): Task | null {
    for (const list of board.lists) {
      const task = list.tasks.find(({ id }) => id === taskId);
      if (task) {
        return task;
      }
    }

    return null;
  }

  private normalizeTaskPositions(tasks: Task[]): void {
    tasks.forEach((task, index) => {
      task.position = index;
    });
  }

  private restoreBoardSnapshot(board: Board): void {
    this.selectedBoard = board;
    this.boards = this.boards.map((item) => item.id === board.id ? board : item);
  }

  private applyOptimisticTaskMove(
    taskId: string,
    sourceListId: string,
    targetListId: string,
    requestedPosition: number,
  ): Task | null {
    let movedTask: Task | null = null;

    this.updateSelectedBoard((board) => {
      const nextBoard = structuredClone(board);
      const sourceList = nextBoard.lists.find(({ id }) => id === sourceListId);
      const targetList = nextBoard.lists.find(({ id }) => id === targetListId);
      if (!sourceList || !targetList) {
        return board;
      }

      const sourceIndex = sourceList.tasks.findIndex(({ id }) => id === taskId);
      if (sourceIndex < 0) {
        return board;
      }

      const [task] = sourceList.tasks.splice(sourceIndex, 1);
      const targetPosition = Math.max(0, Math.min(requestedPosition, targetList.tasks.length));
      movedTask = { ...task, listId: targetListId, position: targetPosition };
      targetList.tasks.splice(targetPosition, 0, movedTask);
      this.normalizeTaskPositions(sourceList.tasks);
      if (sourceListId !== targetListId) {
        this.normalizeTaskPositions(targetList.tasks);
      }

      return nextBoard;
    });

    return movedTask;
  }

  private replaceTaskInBoard(board: Board, movedTask: Task): Board {
    const nextBoard = structuredClone(board);

    nextBoard.lists = nextBoard.lists.map((list) => ({
      ...list,
      tasks: list.tasks
        .filter((task) => task.id !== movedTask.id || list.id === movedTask.listId)
        .map((task) => task.id === movedTask.id ? movedTask : task),
    }));

    const targetList = nextBoard.lists.find(({ id }) => id === movedTask.listId);
    if (targetList && !targetList.tasks.some(({ id }) => id === movedTask.id)) {
      targetList.tasks.push(movedTask);
    }

    for (const list of nextBoard.lists) {
      this.normalizeTaskPositions(list.tasks);
    }

    return nextBoard;
  }

  private moveDraggedTask(targetListId: string, position: number): void {
    const task = this.dragTask;
    const sourceListId = this.dragSourceListId;
    if (!task || !sourceListId || !this.selectedBoard) {
      return;
    }

    this.dragOverListId.set(null);
    this.dragOverIndex.set(-1);
    const previousBoard = structuredClone(this.selectedBoard);
    const optimisticTask = this.applyOptimisticTaskMove(task.id, sourceListId, targetListId, position);
    if (!optimisticTask) {
      this.onDragEnd();
      return;
    }

    this.taskService.moveTask(this.workspaceId, task.id, { targetListId, position }).subscribe({
      next: (movedTask) => {
        this.updateSelectedBoard((board) => this.replaceTaskInBoard(board, movedTask));
        if (this.activeTask()?.id === movedTask.id) {
          this.activeTask.set(movedTask);
        }
        this.onDragEnd();
        this.cdr.markForCheck();
      },
      error: () => {
        this.restoreBoardSnapshot(previousBoard);
        if (this.activeTask()?.id === task.id) {
          this.activeTask.set(this.findTaskById(previousBoard, task.id) ?? task);
        }
        this.onDragEnd();
        this.alertService.danger('Could not move the task.', 'Error');
        this.cdr.markForCheck();
      },
    });
  }

  private updateSelectedBoard(update: (board: Board) => Board): void {
    if (!this.selectedBoard) {
      return;
    }

    const nextBoard = update(this.selectedBoard);
    this.selectedBoard = nextBoard;
    this.boards = this.boards.map((board) => (board.id === nextBoard.id ? nextBoard : board));
  }
}
