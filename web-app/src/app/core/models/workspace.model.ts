export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export interface Workspace {
  id: string;
  name: string;
  description: string | null;
  ownerAuthId: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceMember {
  id: string;
  workspaceId: string;
  authId: string;
  role: WorkspaceRole;
  joinedAt: string;
}

export interface CreateWorkspaceRequest {
  name: string;
  description?: string;
}

export interface UpdateWorkspaceRequest {
  name?: string;
  description?: string;
}

export interface AddMemberRequest {
  authId: string;
}

export interface UpdateMemberRoleRequest {
  role: WorkspaceRole;
}
