import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AlertService } from '../../shared/components/alert/service/alert.service';
import { ErrorHandlerService } from '../services/error-handler.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const alertService = inject(AlertService);
  const errorHandlerService = inject(ErrorHandlerService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        localStorage.removeItem('token');
        alertService.danger('Your session has expired. Please sign in again.', 'Session expired');
        router.navigate(['/login']);
        return throwError(() => error);
      }

      if (error.status === 0) {
        alertService.danger(
          'Could not connect to the server. Please check your connection.',
          'Connection error',
        );
        localStorage.removeItem('token');
        router.navigate(['/login']);
        return throwError(() => error);
      }

      const alert = errorHandlerService.mapHttpErrorToAlert(error);
      alertService.show(alert);

      return throwError(() => error);
    }),
  );
};
