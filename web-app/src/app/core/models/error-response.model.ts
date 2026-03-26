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

export interface ErrorResponseDTO {
  errorType: ErrorTP | UserErrorTP | WorkspaceErrorTP | TaskErrorTP;
  message: string;
  timestamp: string;
}

export const ErrorMessages: Record<
  ErrorTP | UserErrorTP | WorkspaceErrorTP | TaskErrorTP,
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
};
