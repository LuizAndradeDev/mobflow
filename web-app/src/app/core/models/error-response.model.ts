export enum ErrorTP {
  USERNAME_ALREADY_EXIST = 'USERNAME_ALREADY_EXIST',
  EMAIL_ALREADY_EXIST = 'EMAIL_ALREADY_EXIST',
  GENERIC_ERROR = 'GENERIC_ERROR',
}

export interface ErrorResponseDTO {
  errorType: ErrorTP;
  message: string;
  timestamp: string;
}

export const ErrorMessages: Record<ErrorTP, string> = {
  [ErrorTP.USERNAME_ALREADY_EXIST]: 'Este nome de usuário já está em uso.',
  [ErrorTP.EMAIL_ALREADY_EXIST]: 'Este e-mail já está cadastrado em nosso sistema.',
  [ErrorTP.GENERIC_ERROR]: 'Ocorreu um erro inesperado. Tente novamente mais tarde.',
};
