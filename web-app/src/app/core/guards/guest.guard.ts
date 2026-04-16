import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { BrowserStorageService } from '../services/browser-storage.service';

export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const storage = inject(BrowserStorageService);

  if (storage.hasToken()) {
    router.navigate(['/home']);
    return false;
  }

  return true;
};
