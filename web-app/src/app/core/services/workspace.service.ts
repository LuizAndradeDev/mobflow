import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import {
  AddMemberRequest,
  CreateWorkspaceRequest,
  UpdateMemberRoleRequest,
  UpdateWorkspaceRequest,
  Workspace,
  WorkspaceInvite,
  WorkspaceMember,
} from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly apiPath = '/api/workspaces';

  private listCache$: Observable<Workspace[]> | null = null;

  constructor(private readonly http: HttpClient) {}

  create(data: CreateWorkspaceRequest) {
    return this.http.post<Workspace>(this.apiPath, data).pipe(tap(() => this.invalidateListCache()));
  }

  listMine(): Observable<Workspace[]> {
    if (!this.listCache$) {
      this.listCache$ = this.http.get<Workspace[]>(this.apiPath).pipe(shareReplay(1));
    }
    return this.listCache$;
  }

  getById(id: string) {
    return this.http.get<Workspace>(`${this.apiPath}/${id}`);
  }

  previewByCode(code: string) {
    return this.http.get<Workspace>(`${this.apiPath}/join/${code}`);
  }

  joinByCode(code: string) {
    return this.http
      .post<WorkspaceMember>(`${this.apiPath}/join/${code}`, {})
      .pipe(tap(() => this.invalidateListCache()));
  }

  update(id: string, data: UpdateWorkspaceRequest) {
    return this.http
      .put<Workspace>(`${this.apiPath}/${id}`, data)
      .pipe(tap(() => this.invalidateListCache()));
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.apiPath}/${id}`).pipe(tap(() => this.invalidateAll()));
  }

  leave(id: string) {
    return this.http
      .delete<void>(`${this.apiPath}/${id}/leave`)
      .pipe(tap(() => this.invalidateAll()));
  }

  listMembers(workspaceId: string): Observable<WorkspaceMember[]> {
    return this.http.get<WorkspaceMember[]>(`${this.apiPath}/${workspaceId}/members`);
  }

  inviteMember(workspaceId: string, data: AddMemberRequest) {
    return this.http.post<WorkspaceInvite>(`${this.apiPath}/${workspaceId}/invites`, data);
  }

  acceptInvite(inviteId: string) {
    return this.http
      .post<WorkspaceMember>(`${this.apiPath}/invites/${inviteId}/accept`, {})
      .pipe(tap(() => this.invalidateListCache()));
  }

  declineInvite(inviteId: string) {
    return this.http.post<WorkspaceInvite>(`${this.apiPath}/invites/${inviteId}/decline`, {});
  }

  removeMember(workspaceId: string, memberAuthId: string) {
    return this.http.delete<void>(`${this.apiPath}/${workspaceId}/members/${memberAuthId}`);
  }

  updateMemberRole(workspaceId: string, memberAuthId: string, data: UpdateMemberRoleRequest) {
    return this.http.patch<WorkspaceMember>(`${this.apiPath}/${workspaceId}/members/${memberAuthId}/role`, data);
  }

  invalidateListCache() {
    this.listCache$ = null;
  }

  invalidateAll() {
    this.invalidateListCache();
  }
}
