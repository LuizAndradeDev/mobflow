import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ErrorMessages,
  ErrorResponseDTO,
  ErrorTP,
  UserErrorTP,
  WorkspaceErrorTP,
  TaskErrorTP,
} from '../models/error-response.model';
import { AlertInterface } from '../../shared/components/alert/model/alert.interface';
import { AlertType } from '../../shared/components/alert/enum/alert-type.enum';

type AlertWithoutId = Omit<AlertInterface, 'id'>;

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  mapHttpErrorToAlert(error: HttpErrorResponse): AlertWithoutId {
    const backendError = error.error as ErrorResponseDTO | null;

    if (backendError?.errorType) {
      return this.mapBackendError(backendError);
    }

    return this.mapStatusError(error);
  }

  private mapBackendError(error: ErrorResponseDTO): AlertWithoutId {
    const translatedMessage =
      ErrorMessages[error.errorType] ?? ErrorMessages[ErrorTP.GENERIC_ERROR];

    return {
      title: this.getTitleByErrorType(error.errorType),
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
    errorType: ErrorTP | UserErrorTP | WorkspaceErrorTP | TaskErrorTP,
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
      default: return 'Error';
    }
  }
}
