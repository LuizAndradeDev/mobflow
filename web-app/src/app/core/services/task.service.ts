import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Board,
  CreateBoardRequest,
  CreateTaskListRequest,
  CreateTaskRequest,
  MoveTaskRequest,
  ReorderListsRequest,
  Task,
  TaskAnalytics,
  TaskList,
  UpdateBoardRequest,
  UpdateTaskListRequest,
  UpdateTaskRequest,
  WorkspaceSummary,
} from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly API = 'http://localhost:8083/tasks/api';
  private readonly INTERNAL_API = 'http://localhost:8083/tasks/internal/tasks';
  private readonly INTERNAL_SECRET = 'mobflow-internal-secret-2024';

  constructor(private http: HttpClient) {}

  // ---- Boards ----

  listBoards(workspaceId: string): Observable<Board[]> {
    return this.http.get<Board[]>(`${this.API}/workspaces/${workspaceId}/boards`);
  }

  getBoard(workspaceId: string, boardId: string): Observable<Board> {
    return this.http.get<Board>(`${this.API}/workspaces/${workspaceId}/boards/${boardId}`);
  }

  createBoard(workspaceId: string, data: CreateBoardRequest): Observable<Board> {
    return this.http.post<Board>(`${this.API}/workspaces/${workspaceId}/boards`, data);
  }

  updateBoard(workspaceId: string, boardId: string, data: UpdateBoardRequest): Observable<Board> {
    return this.http.put<Board>(`${this.API}/workspaces/${workspaceId}/boards/${boardId}`, data);
  }

  deleteBoard(workspaceId: string, boardId: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/workspaces/${workspaceId}/boards/${boardId}`);
  }

  // ---- Task Lists ----

  createList(workspaceId: string, boardId: string, data: CreateTaskListRequest): Observable<TaskList> {
    return this.http.post<TaskList>(
      `${this.API}/workspaces/${workspaceId}/boards/${boardId}/lists`,
      data,
    );
  }

  updateList(workspaceId: string, boardId: string, listId: string, data: UpdateTaskListRequest): Observable<TaskList> {
    return this.http.put<TaskList>(
      `${this.API}/workspaces/${workspaceId}/boards/${boardId}/lists/${listId}`,
      data,
    );
  }

  deleteList(workspaceId: string, boardId: string, listId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.API}/workspaces/${workspaceId}/boards/${boardId}/lists/${listId}`,
    );
  }

  reorderLists(workspaceId: string, boardId: string, data: ReorderListsRequest): Observable<void> {
    return this.http.patch<void>(
      `${this.API}/workspaces/${workspaceId}/boards/${boardId}/lists/reorder`,
      data,
    );
  }

  // ---- Tasks ----

  listTasks(workspaceId: string, listId: string): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.API}/workspaces/${workspaceId}/lists/${listId}/tasks`);
  }

  getTask(workspaceId: string, taskId: string): Observable<Task> {
    return this.http.get<Task>(`${this.API}/workspaces/${workspaceId}/tasks/${taskId}`);
  }

  createTask(workspaceId: string, listId: string, data: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(
      `${this.API}/workspaces/${workspaceId}/lists/${listId}/tasks`,
      data,
    );
  }

  updateTask(workspaceId: string, taskId: string, data: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.API}/workspaces/${workspaceId}/tasks/${taskId}`, data);
  }

  moveTask(workspaceId: string, taskId: string, data: MoveTaskRequest): Observable<Task> {
    return this.http.patch<Task>(
      `${this.API}/workspaces/${workspaceId}/tasks/${taskId}/move`,
      data,
    );
  }

  deleteTask(workspaceId: string, taskId: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/workspaces/${workspaceId}/tasks/${taskId}`);
  }

  // ---- Summaries (overview page) ----

  getWorkspaceSummary(workspaceId: string): Observable<WorkspaceSummary> {
    return this.http.get<WorkspaceSummary>(`${this.INTERNAL_API}/summary/${workspaceId}`, {
      headers: { 'X-Internal-Secret': this.INTERNAL_SECRET },
    });
  }

  getBatchSummaries(workspaceIds: string[]): Observable<WorkspaceSummary[]> {
    return this.http.post<WorkspaceSummary[]>(`${this.INTERNAL_API}/summaries`, workspaceIds, {
      headers: { 'X-Internal-Secret': this.INTERNAL_SECRET },
    });
  }

  // ---- Analytics ----

  /**
   * Analytics de um workspace específico para o usuário autenticado.
   * GET /api/task-analytics/workspace/{workspaceId}/user/{authId}
   */
  getWorkspaceAnalytics(workspaceId: string, authId: string): Observable<TaskAnalytics> {
    return this.http.get<TaskAnalytics>(
      `${this.API}/task-analytics/workspace/${workspaceId}/user/${authId}`,
    );
  }

  /**
   * Analytics do usuário em múltiplos workspaces.
   * POST /api/task-analytics/user/{authId}/workspaces
   */
  getUserAnalyticsAcrossWorkspaces(authId: string, workspaceIds: string[]): Observable<TaskAnalytics> {
    return this.http.post<TaskAnalytics>(
      `${this.API}/task-analytics/user/${authId}/workspaces`,
      workspaceIds,
    );
  }

  /**
   * Analytics globais do usuário (todos os workspaces).
   * GET /api/task-analytics/user/{authId}
   */
  getUserAnalytics(authId: string): Observable<TaskAnalytics> {
    return this.http.get<TaskAnalytics>(`${this.API}/task-analytics/user/${authId}`);
  }
}
