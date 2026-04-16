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
  private readonly apiPath = '/tasks/api';

  constructor(private readonly http: HttpClient) {}

  listBoards(workspaceId: string): Observable<Board[]> {
    return this.http.get<Board[]>(this.workspaceBoardsPath(workspaceId));
  }

  getBoard(workspaceId: string, boardId: string): Observable<Board> {
    return this.http.get<Board>(`${this.workspaceBoardsPath(workspaceId)}/${boardId}`);
  }

  createBoard(workspaceId: string, data: CreateBoardRequest): Observable<Board> {
    return this.http.post<Board>(this.workspaceBoardsPath(workspaceId), data);
  }

  updateBoard(workspaceId: string, boardId: string, data: UpdateBoardRequest): Observable<Board> {
    return this.http.put<Board>(`${this.workspaceBoardsPath(workspaceId)}/${boardId}`, data);
  }

  deleteBoard(workspaceId: string, boardId: string): Observable<void> {
    return this.http.delete<void>(`${this.workspaceBoardsPath(workspaceId)}/${boardId}`);
  }

  createList(workspaceId: string, boardId: string, data: CreateTaskListRequest): Observable<TaskList> {
    return this.http.post<TaskList>(this.boardListsPath(workspaceId, boardId), data);
  }

  updateList(workspaceId: string, boardId: string, listId: string, data: UpdateTaskListRequest): Observable<TaskList> {
    return this.http.put<TaskList>(`${this.boardListsPath(workspaceId, boardId)}/${listId}`, data);
  }

  deleteList(workspaceId: string, boardId: string, listId: string): Observable<void> {
    return this.http.delete<void>(`${this.boardListsPath(workspaceId, boardId)}/${listId}`);
  }

  reorderLists(workspaceId: string, boardId: string, data: ReorderListsRequest): Observable<void> {
    return this.http.patch<void>(`${this.boardListsPath(workspaceId, boardId)}/reorder`, data);
  }

  listTasks(workspaceId: string, listId: string): Observable<Task[]> {
    return this.http.get<Task[]>(this.listTasksPath(workspaceId, listId));
  }

  getTask(workspaceId: string, taskId: string): Observable<Task> {
    return this.http.get<Task>(this.taskPath(workspaceId, taskId));
  }

  createTask(workspaceId: string, listId: string, data: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.listTasksPath(workspaceId, listId), data);
  }

  updateTask(workspaceId: string, taskId: string, data: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(this.taskPath(workspaceId, taskId), data);
  }

  moveTask(workspaceId: string, taskId: string, data: MoveTaskRequest): Observable<Task> {
    return this.http.patch<Task>(`${this.taskPath(workspaceId, taskId)}/move`, data);
  }

  deleteTask(workspaceId: string, taskId: string): Observable<void> {
    return this.http.delete<void>(this.taskPath(workspaceId, taskId));
  }

  getWorkspaceSummary(workspaceId: string): Observable<WorkspaceSummary> {
    return this.http.get<WorkspaceSummary>(`${this.apiPath}/workspace-summaries/${workspaceId}`);
  }

  getBatchSummaries(workspaceIds: string[]): Observable<WorkspaceSummary[]> {
    return this.http.post<WorkspaceSummary[]>(`${this.apiPath}/workspace-summaries/batch`, workspaceIds);
  }
  getWorkspaceAnalytics(workspaceId: string, authId: string): Observable<TaskAnalytics> {
    return this.http.get<TaskAnalytics>(`${this.apiPath}/task-analytics/workspace/${workspaceId}/user/${authId}`);
  }

  getUserAnalyticsAcrossWorkspaces(authId: string, workspaceIds: string[]): Observable<TaskAnalytics> {
    return this.http.post<TaskAnalytics>(`${this.apiPath}/task-analytics/user/${authId}/workspaces`, workspaceIds);
  }

  getUserAnalytics(authId: string): Observable<TaskAnalytics> {
    return this.http.get<TaskAnalytics>(`${this.apiPath}/task-analytics/user/${authId}`);
  }

  private workspaceBoardsPath(workspaceId: string): string {
    return `${this.apiPath}/workspaces/${workspaceId}/boards`;
  }

  private boardListsPath(workspaceId: string, boardId: string): string {
    return `${this.workspaceBoardsPath(workspaceId)}/${boardId}/lists`;
  }

  private listTasksPath(workspaceId: string, listId: string): string {
    return `${this.apiPath}/workspaces/${workspaceId}/lists/${listId}/tasks`;
  }

  private taskPath(workspaceId: string, taskId: string): string {
    return `${this.apiPath}/workspaces/${workspaceId}/tasks/${taskId}`;
  }
}
