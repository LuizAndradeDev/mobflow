import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ErrorMessages,
  ErrorResponseDTO,
  ErrorTP,
  SocialErrorTP,
  UserErrorTP,
  WorkspaceErrorTP,
  TaskErrorTP,
  ChatErrorTP,
} from '../models/error-response.model';
import { AlertInterface } from '../../shared/components/alert/model/alert.interface';
import { AlertType } from '../../shared/components/alert/enum/alert-type.enum';

type AlertWithoutId = Omit<AlertInterface, 'id'>;

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  mapHttpErrorToAlert(error: HttpErrorResponse): AlertWithoutId {
    const backendError = error.error as ErrorResponseDTO | null;
    const socialErrorType = this.resolveProblemTitle(backendError);

    if (backendError?.errorType || socialErrorType) {
      return this.mapBackendError(backendError, socialErrorType);
    }

    if (backendError?.errors?.length) {
      return {
        title: 'Validation error',
        message: backendError.errors[0],
        alertType: AlertType.warning,
      };
    }

    return this.mapStatusError(error);
  }

  private mapBackendError(
    error: ErrorResponseDTO | null,
    socialErrorType?: SocialErrorTP,
  ): AlertWithoutId {
    const resolvedErrorType = error?.errorType ?? socialErrorType ?? ErrorTP.GENERIC_ERROR;
    const translatedMessage =
      ErrorMessages[resolvedErrorType] ?? error?.detail ?? error?.message ?? ErrorMessages[ErrorTP.GENERIC_ERROR];

    return {
      title: this.getTitleByErrorType(resolvedErrorType),
      message: translatedMessage,
      alertType: AlertType.error,
    };
  }

  private mapStatusError(error: HttpErrorResponse): AlertWithoutId {
    switch (error.status) {
      case 400:
        return { title: 'Invalid request', message: 'The submitted data is invalid.', alertType: AlertType.warning };
      case 401:
        return { title: 'Unauthorized', message: 'You need to sign in to continue.', alertType: AlertType.warning };
      case 403:
        return { title: 'Access denied', message: 'You do not have permission to access this resource.', alertType: AlertType.error };
      case 404:
        return { title: 'Not found', message: 'The requested resource was not found.', alertType: AlertType.warning };
      case 409:
        return { title: 'Conflict', message: 'This action conflicts with existing data.', alertType: AlertType.warning };
      case 422:
        return { title: 'Unprocessable request', message: 'The operation could not be completed.', alertType: AlertType.warning };
      case 500:
        return { title: 'Internal server error', message: 'The server encountered an unexpected error.', alertType: AlertType.error };
      default:
        return { title: 'Unexpected error', message: 'An unexpected error occurred. Please try again later.', alertType: AlertType.error };
    }
  }

  private getTitleByErrorType(
    errorType: ErrorTP | UserErrorTP | WorkspaceErrorTP | TaskErrorTP | SocialErrorTP | ChatErrorTP,
  ): string {
    switch (errorType) {
      case ErrorTP.USERNAME_ALREADY_EXIST: return 'Username already taken';
      case ErrorTP.EMAIL_ALREADY_EXIST: return 'Email already registered';
      case ErrorTP.INVALID_CREDENTIALS: return 'Invalid credentials';
      case UserErrorTP.USER_PROFILE_NOT_FOUND: return 'Profile not found';
      case WorkspaceErrorTP.WORKSPACE_NOT_FOUND: return 'Workspace not found';
      case WorkspaceErrorTP.MEMBER_ALREADY_EXISTS: return 'Member already exists';
      case WorkspaceErrorTP.MEMBER_NOT_FOUND: return 'Member not found';
      case WorkspaceErrorTP.UNAUTHORIZED_ACTION: return 'Permission denied';
      case WorkspaceErrorTP.CANNOT_REMOVE_OWNER: return 'Cannot remove owner';
      case TaskErrorTP.BOARD_NOT_FOUND: return 'Board not found';
      case TaskErrorTP.TASK_LIST_NOT_FOUND: return 'List not found';
      case TaskErrorTP.TASK_NOT_FOUND: return 'Task not found';
      case TaskErrorTP.ACCESS_DENIED: return 'Permission denied';
      case TaskErrorTP.WORKSPACE_MEMBER_NOT_FOUND: return 'Not a member';
      case SocialErrorTP.COMMENT_NOT_FOUND: return 'Comment not found';
      case SocialErrorTP.TASK_NOT_FOUND: return 'Task not found';
      case SocialErrorTP.FRIEND_REQUEST_NOT_FOUND: return 'Friend request not found';
      case SocialErrorTP.USER_NOT_FOUND: return 'User not found';
      case SocialErrorTP.ACCESS_DENIED: return 'Permission denied';
      case SocialErrorTP.WORKSPACE_MEMBERSHIP_REQUIRED: return 'Membership required';
      case SocialErrorTP.COMMENT_ALREADY_DELETED: return 'Comment already deleted';
      case SocialErrorTP.INVALID_COMMENT_STATE: return 'Comment unavailable';
      case SocialErrorTP.FRIEND_REQUEST_TO_SELF: return 'Invalid friend request';
      case SocialErrorTP.FRIEND_REQUEST_ALREADY_EXISTS: return 'Request already pending';
      case SocialErrorTP.FRIENDSHIP_ALREADY_EXISTS: return 'Already friends';
      case SocialErrorTP.INVALID_FRIEND_REQUEST_STATE: return 'Request already processed';
      case SocialErrorTP.UPSTREAM_SERVICE_ERROR: return 'Integration error';
      case ChatErrorTP.CONVERSATION_NOT_FOUND: return 'Conversation not found';
      case ChatErrorTP.ACCESS_DENIED: return 'Permission denied';
      case ChatErrorTP.SELF_CONVERSATION_NOT_ALLOWED: return 'Invalid conversation';
      case ChatErrorTP.FRIENDSHIP_REQUIRED: return 'Friendship required';
      case ChatErrorTP.SOCIAL_SERVICE_UNAVAILABLE: return 'Integration error';
      case ChatErrorTP.INVALID_MESSAGE_CONTENT: return 'Invalid message';
      case ChatErrorTP.WEBSOCKET_AUTHENTICATION_REQUIRED: return 'Reconnect required';
      case ChatErrorTP.INVALID_DESTINATION: return 'Invalid destination';
      default: return 'Error';
    }
  }

  private resolveProblemTitle(error: ErrorResponseDTO | null): SocialErrorTP | undefined {
    const title = error?.title;

    if (!title) {
      return undefined;
    }

    return Object.values(SocialErrorTP).find((value) => value === title);
  }
}
