export type FriendRequestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface SocialComment {
  id: string;
  taskId: string;
  workspaceId: string;
  authorId: string;
  authorUsername: string;
  content: string | null;
  mentions: string[];
  createdAt: string;
  editedAt: string | null;
  deleted: boolean;
}

export interface CreateCommentRequest {
  content: string;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface SendFriendRequestRequest {
  username: string;
}

export interface FriendRequest {
  id: string;
  requesterId: string;
  requesterUsername: string;
  targetId: string;
  targetUsername: string;
  status: FriendRequestStatus;
  createdAt: string;
  respondedAt: string | null;
  incoming: boolean;
}

export interface Friend {
  authId: string;
  username: string;
  avatarUrl: string | null;
  friendsSince: string;
}
