import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AlertService } from '../../shared/components/alert/service/alert.service';
import { ErrorHandlerService } from '../services/error-handler.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const alertService = inject(AlertService);
  const errorHandlerService = inject(ErrorHandlerService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const alert = errorHandlerService.mapHttpErrorToAlert(error);
      alertService.show(alert);

      return throwError(() => error);
    }),
  );
};
