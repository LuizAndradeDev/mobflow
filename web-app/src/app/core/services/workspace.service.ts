import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  AddMemberRequest,
  CreateWorkspaceRequest,
  UpdateMemberRoleRequest,
  UpdateWorkspaceRequest,
  Workspace,
  WorkspaceMember,
} from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly API = 'http://localhost:8082/api/workspaces';

  constructor(private http: HttpClient) {}

  create(data: CreateWorkspaceRequest) {
    return this.http.post<Workspace>(this.API, data);
  }

  listMine() {
    return this.http.get<Workspace[]>(this.API);
  }

  getById(id: string) {
    return this.http.get<Workspace>(`${this.API}/${id}`);
  }

  update(id: string, data: UpdateWorkspaceRequest) {
    return this.http.put<Workspace>(`${this.API}/${id}`, data);
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  listMembers(workspaceId: string) {
    return this.http.get<WorkspaceMember[]>(`${this.API}/${workspaceId}/members`);
  }

  addMember(workspaceId: string, data: AddMemberRequest) {
    return this.http.post<WorkspaceMember>(`${this.API}/${workspaceId}/members`, data);
  }

  removeMember(workspaceId: string, memberAuthId: string) {
    return this.http.delete<void>(`${this.API}/${workspaceId}/members/${memberAuthId}`);
  }

  updateMemberRole(workspaceId: string, memberAuthId: string, data: UpdateMemberRoleRequest) {
    return this.http.patch<WorkspaceMember>(
      `${this.API}/${workspaceId}/members/${memberAuthId}/role`,
      data,
    );
  }
}
