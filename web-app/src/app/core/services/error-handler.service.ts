import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ErrorMessages, ErrorResponseDTO, ErrorTP } from '../models/error-response.model';
import { AlertInterface } from '../../shared/components/alert/model/alert.interface';
import { AlertType } from '../../shared/components/alert/enum/alert-type.enum';

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  mapHttpErrorToAlert(error: HttpErrorResponse): AlertInterface {
    const backendError = error.error as ErrorResponseDTO | null;

    if (backendError?.errorType) {
      return this.mapBackendError(backendError);
    }

    return this.mapStatusError(error);
  }

  private mapBackendError(error: ErrorResponseDTO): AlertInterface {
    const translatedMessage =
      ErrorMessages[error.errorType] || ErrorMessages[ErrorTP.GENERIC_ERROR];

    return {
      title: this.getTitleByErrorType(error.errorType),
      message: translatedMessage,
      alertType: AlertType.error,
    };
  }

  private mapStatusError(error: HttpErrorResponse): AlertInterface {
    switch (error.status) {
      case 400:
        return {
          title: 'Requisição inválida',
          message: 'Os dados enviados são inválidos.',
          alertType: AlertType.warning,
        };

      case 401:
        return {
          title: 'Não autorizado',
          message: 'Você precisa fazer login para continuar.',
          alertType: AlertType.success,
        };

      case 403:
        return {
          title: 'Acesso negado',
          message: 'Você não tem permissão para acessar este recurso.',
          alertType: AlertType.error,
        };

      case 404:
        return {
          title: 'Não encontrado',
          message: 'O recurso solicitado não foi encontrado.',
          alertType: AlertType.warning,
        };

      case 500:
        return {
          title: 'Erro interno',
          message: 'O servidor encontrou um erro inesperado.',
          alertType: AlertType.error,
        };

      default:
        return {
          title: 'Erro',
          message: 'Ocorreu um erro inesperado. Tente novamente mais tarde.',
          alertType: AlertType.error,
        };
    }
  }

  private getTitleByErrorType(errorType: ErrorTP): string {
    switch (errorType) {
      case ErrorTP.USERNAME_ALREADY_EXIST:
        return 'Usuário já existe';

      case ErrorTP.EMAIL_ALREADY_EXIST:
        return 'E-mail já cadastrado';

      default:
        return 'Erro de cadastro';
    }
  }
}
