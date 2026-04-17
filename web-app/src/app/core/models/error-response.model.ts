export enum ErrorTP {
  USERNAME_ALREADY_EXIST = 'USERNAME_ALREADY_EXIST',
  EMAIL_ALREADY_EXIST = 'EMAIL_ALREADY_EXIST',
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  GENERIC_ERROR = 'GENERIC_ERROR',
}

export enum UserErrorTP {
  USER_PROFILE_NOT_FOUND = 'USER_PROFILE_NOT_FOUND',
}

export enum WorkspaceErrorTP {
  WORKSPACE_NOT_FOUND = 'WORKSPACE_NOT_FOUND',
  MEMBER_ALREADY_EXISTS = 'MEMBER_ALREADY_EXISTS',
  MEMBER_NOT_FOUND = 'MEMBER_NOT_FOUND',
  UNAUTHORIZED_ACTION = 'UNAUTHORIZED_ACTION',
  CANNOT_REMOVE_OWNER = 'CANNOT_REMOVE_OWNER',
}

export enum TaskErrorTP {
  BOARD_NOT_FOUND = 'BOARD_NOT_FOUND',
  TASK_LIST_NOT_FOUND = 'TASK_LIST_NOT_FOUND',
  TASK_NOT_FOUND = 'TASK_NOT_FOUND',
  ACCESS_DENIED = 'ACCESS_DENIED',
  WORKSPACE_MEMBER_NOT_FOUND = 'WORKSPACE_MEMBER_NOT_FOUND',
}

export enum SocialErrorTP {
  COMMENT_NOT_FOUND = 'COMMENT_NOT_FOUND',
  TASK_NOT_FOUND = 'TASK_NOT_FOUND',
  FRIEND_REQUEST_NOT_FOUND = 'FRIEND_REQUEST_NOT_FOUND',
  USER_NOT_FOUND = 'USER_NOT_FOUND',
  ACCESS_DENIED = 'ACCESS_DENIED',
  WORKSPACE_MEMBERSHIP_REQUIRED = 'WORKSPACE_MEMBERSHIP_REQUIRED',
  COMMENT_ALREADY_DELETED = 'COMMENT_ALREADY_DELETED',
  INVALID_COMMENT_STATE = 'INVALID_COMMENT_STATE',
  FRIEND_REQUEST_TO_SELF = 'FRIEND_REQUEST_TO_SELF',
  FRIEND_REQUEST_ALREADY_EXISTS = 'FRIEND_REQUEST_ALREADY_EXISTS',
  FRIENDSHIP_ALREADY_EXISTS = 'FRIENDSHIP_ALREADY_EXISTS',
  INVALID_FRIEND_REQUEST_STATE = 'INVALID_FRIEND_REQUEST_STATE',
  UPSTREAM_SERVICE_ERROR = 'UPSTREAM_SERVICE_ERROR',
}

export enum ChatErrorTP {
  CONVERSATION_NOT_FOUND = 'CONVERSATION_NOT_FOUND',
  ACCESS_DENIED = 'ACCESS_DENIED',
  SELF_CONVERSATION_NOT_ALLOWED = 'SELF_CONVERSATION_NOT_ALLOWED',
  FRIENDSHIP_REQUIRED = 'FRIENDSHIP_REQUIRED',
  SOCIAL_SERVICE_UNAVAILABLE = 'SOCIAL_SERVICE_UNAVAILABLE',
  INVALID_MESSAGE_CONTENT = 'INVALID_MESSAGE_CONTENT',
  WEBSOCKET_AUTHENTICATION_REQUIRED = 'WEBSOCKET_AUTHENTICATION_REQUIRED',
  INVALID_DESTINATION = 'INVALID_DESTINATION',
}

export interface ErrorResponseDTO {
  errorType?: ErrorTP | UserErrorTP | WorkspaceErrorTP | TaskErrorTP | SocialErrorTP | ChatErrorTP;
  title?: string;
  detail?: string;
  message: string;
  timestamp: string;
  errors?: string[];
}

export const ErrorMessages: Record<
  ErrorTP | UserErrorTP | WorkspaceErrorTP | TaskErrorTP | SocialErrorTP | ChatErrorTP,
  string
> = {
  [ErrorTP.USERNAME_ALREADY_EXIST]: 'This username is already in use.',
  [ErrorTP.EMAIL_ALREADY_EXIST]: 'This email is already registered.',
  [ErrorTP.INVALID_CREDENTIALS]: 'Invalid email or password.',
  [ErrorTP.GENERIC_ERROR]: 'An unexpected error occurred. Please try again later.',
  [UserErrorTP.USER_PROFILE_NOT_FOUND]: 'User profile not found.',
  [WorkspaceErrorTP.WORKSPACE_NOT_FOUND]: 'Workspace not found.',
  [WorkspaceErrorTP.MEMBER_ALREADY_EXISTS]: 'This user is already a member of the workspace.',
  [WorkspaceErrorTP.MEMBER_NOT_FOUND]: 'Member not found in this workspace.',
  [WorkspaceErrorTP.UNAUTHORIZED_ACTION]: 'You do not have permission to perform this action.',
  [WorkspaceErrorTP.CANNOT_REMOVE_OWNER]: 'The workspace owner cannot be removed.',
  [TaskErrorTP.BOARD_NOT_FOUND]: 'Board not found.',
  [TaskErrorTP.TASK_LIST_NOT_FOUND]: 'List not found.',
  [TaskErrorTP.TASK_NOT_FOUND]: 'Task not found.',
  [TaskErrorTP.ACCESS_DENIED]: 'You do not have permission to perform this action.',
  [TaskErrorTP.WORKSPACE_MEMBER_NOT_FOUND]: 'You are not a member of this workspace.',
  [SocialErrorTP.COMMENT_NOT_FOUND]: 'Comment not found.',
  [SocialErrorTP.FRIEND_REQUEST_NOT_FOUND]: 'Friend request not found.',
  [SocialErrorTP.USER_NOT_FOUND]: 'The requested user does not exist.',
  [SocialErrorTP.WORKSPACE_MEMBERSHIP_REQUIRED]: 'You must be a member of this workspace to use comments.',
  [SocialErrorTP.COMMENT_ALREADY_DELETED]: 'This comment was already deleted.',
  [SocialErrorTP.INVALID_COMMENT_STATE]: 'This comment cannot be edited anymore.',
  [SocialErrorTP.FRIEND_REQUEST_TO_SELF]: 'You cannot send a friend request to yourself.',
  [SocialErrorTP.FRIEND_REQUEST_ALREADY_EXISTS]: 'There is already a pending friend request for this user.',
  [SocialErrorTP.FRIENDSHIP_ALREADY_EXISTS]: 'You are already friends with this user.',
  [SocialErrorTP.INVALID_FRIEND_REQUEST_STATE]: 'This friend request was already processed.',
  [SocialErrorTP.UPSTREAM_SERVICE_ERROR]: 'A dependent service failed to validate the request.',
  [ChatErrorTP.CONVERSATION_NOT_FOUND]: 'Conversation not found.',
  [ChatErrorTP.SELF_CONVERSATION_NOT_ALLOWED]: 'You cannot start a conversation with yourself.',
  [ChatErrorTP.FRIENDSHIP_REQUIRED]: 'Only friends can exchange messages.',
  [ChatErrorTP.SOCIAL_SERVICE_UNAVAILABLE]: 'The friendship validation service is unavailable right now.',
  [ChatErrorTP.INVALID_MESSAGE_CONTENT]: 'Message content must not be blank.',
  [ChatErrorTP.WEBSOCKET_AUTHENTICATION_REQUIRED]: 'You need to reconnect your chat session.',
  [ChatErrorTP.INVALID_DESTINATION]: 'The chat destination is invalid.',
};
