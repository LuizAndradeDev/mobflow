export interface UserProfile {
  id: string;
  authId: string;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  phone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserProfileRequest {
  displayName?: string;
  bio?: string;
  phone?: string;
}
