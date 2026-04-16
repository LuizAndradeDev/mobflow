import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { BrowserStorageService } from '../services/browser-storage.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const storage = inject(BrowserStorageService);
  const token = storage.getToken();

  if (token) {
    return next(req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }));
  }

  return next(req);
};
