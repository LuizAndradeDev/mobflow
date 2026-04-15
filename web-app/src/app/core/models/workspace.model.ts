export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export interface Workspace {
  id: string;
  publicCode: string;
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
  displayName: string;
  avatarUrl: string | null;
}

export interface WorkspaceInvite {
  id: string;
  workspaceId: string;
  targetAuthId: string;
  invitedByAuthId: string;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
  createdAt: string;
  respondedAt: string | null;
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
  username: string;
}

export interface UpdateMemberRoleRequest {
  role: WorkspaceRole;
}
