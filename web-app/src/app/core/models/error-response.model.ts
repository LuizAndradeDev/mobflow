export enum ErrorTP {
  USERNAME_ALREADY_EXIST = 'USERNAME_ALREADY_EXIST',
  EMAIL_ALREADY_EXIST = 'EMAIL_ALREADY_EXIST',
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  GENERIC_ERROR = 'GENERIC_ERROR',
}

export interface ErrorResponseDTO {
  errorType: ErrorTP;
  message: string;
  timestamp: string;
}

export const ErrorMessages: Record<ErrorTP, string> = {
  [ErrorTP.USERNAME_ALREADY_EXIST]: 'This username is already in use.',
  [ErrorTP.EMAIL_ALREADY_EXIST]: 'This email is already registered.',
  [ErrorTP.INVALID_CREDENTIALS]: 'Invalid email or password.',
  [ErrorTP.GENERIC_ERROR]: 'An unexpected error occurred. Please try again later.',
};
