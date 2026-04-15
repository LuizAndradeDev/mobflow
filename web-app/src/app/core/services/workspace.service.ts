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
  private readonly API = 'http://localhost:8082/api/workspaces';

  private _listCache$: Observable<Workspace[]> | null = null;
  private _memberCache = new Map<string, Observable<WorkspaceMember[]>>();

  constructor(private http: HttpClient) {}

  create(data: CreateWorkspaceRequest) {
    return this.http.post<Workspace>(this.API, data).pipe(tap(() => this.invalidateListCache()));
  }

  listMine(): Observable<Workspace[]> {
    if (!this._listCache$) {
      this._listCache$ = this.http.get<Workspace[]>(this.API).pipe(shareReplay(1));
    }
    return this._listCache$;
  }

  getById(id: string) {
    return this.http.get<Workspace>(`${this.API}/${id}`);
  }

  previewByCode(code: string) {
    return this.http.get<Workspace>(`${this.API}/join/${code}`);
  }

  joinByCode(code: string) {
    return this.http
      .post<WorkspaceMember>(`${this.API}/join/${code}`, {})
      .pipe(tap(() => this.invalidateListCache()));
  }

  update(id: string, data: UpdateWorkspaceRequest) {
    return this.http
      .put<Workspace>(`${this.API}/${id}`, data)
      .pipe(tap(() => this.invalidateListCache()));
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.API}/${id}`).pipe(tap(() => this.invalidateAll(id)));
  }

  leave(id: string) {
    return this.http
      .delete<void>(`${this.API}/${id}/leave`)
      .pipe(tap(() => this.invalidateAll(id)));
  }

  listMembers(workspaceId: string): Observable<WorkspaceMember[]> {
    return this.http.get<WorkspaceMember[]>(`${this.API}/${workspaceId}/members`);
  }

  inviteMember(workspaceId: string, data: AddMemberRequest) {
    return this.http.post<WorkspaceInvite>(`${this.API}/${workspaceId}/invites`, data);
  }

  acceptInvite(inviteId: string) {
    return this.http
      .post<WorkspaceMember>(`${this.API}/invites/${inviteId}/accept`, {})
      .pipe(tap(() => this.invalidateListCache()));
  }

  declineInvite(inviteId: string) {
    return this.http.post<WorkspaceInvite>(`${this.API}/invites/${inviteId}/decline`, {});
  }

  removeMember(workspaceId: string, memberAuthId: string) {
    return this.http
      .delete<void>(`${this.API}/${workspaceId}/members/${memberAuthId}`)
      .pipe(tap(() => this.invalidateMemberCache(workspaceId)));
  }

  updateMemberRole(workspaceId: string, memberAuthId: string, data: UpdateMemberRoleRequest) {
    return this.http
      .patch<WorkspaceMember>(`${this.API}/${workspaceId}/members/${memberAuthId}/role`, data)
      .pipe(tap(() => this.invalidateMemberCache(workspaceId)));
  }

  invalidateListCache() {
    this._listCache$ = null;
  }

  invalidateMemberCache(workspaceId: string) {
    this._memberCache.delete(workspaceId);
  }

  invalidateAll(workspaceId: string) {
    this.invalidateListCache();
    this.invalidateMemberCache(workspaceId);
  }
}
