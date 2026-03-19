export enum ErrorTP {
  USERNAME_ALREADY_EXIST = 'USERNAME_ALREADY_EXIST',
  EMAIL_ALREADY_EXIST = 'EMAIL_ALREADY_EXIST',
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  GENERIC_ERROR = 'GENERIC_ERROR',
}

export enum UserErrorTP {
  USER_PROFILE_NOT_FOUND = 'USER_PROFILE_NOT_FOUND',
  GENERIC_ERROR = 'GENERIC_ERROR',
}

export interface ErrorResponseDTO {
  errorType: ErrorTP | UserErrorTP;
  message: string;
  timestamp: string;
}

export const ErrorMessages: Record<ErrorTP | UserErrorTP, string> = {
  [ErrorTP.USERNAME_ALREADY_EXIST]: 'This username is already in use.',
  [ErrorTP.EMAIL_ALREADY_EXIST]: 'This email is already registered.',
  [ErrorTP.INVALID_CREDENTIALS]: 'Invalid email or password.',
  [ErrorTP.GENERIC_ERROR]: 'An unexpected error occurred. Please try again later.',
  [UserErrorTP.USER_PROFILE_NOT_FOUND]: 'User profile not found.',
};
