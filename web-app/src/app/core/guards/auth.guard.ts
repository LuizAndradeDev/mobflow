import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { BrowserStorageService } from '../services/browser-storage.service';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const storage = inject(BrowserStorageService);

  if (!storage.hasToken()) {
    router.navigate(['/login']);
    return false;
  }

  return true;
};
