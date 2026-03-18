import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { UserProfileService } from '../services/user-profile.service';
import { UserProfileStateService } from '../services/user-profile-state.service';

export const profileGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userProfileService = inject(UserProfileService);
  const userProfileState = inject(UserProfileStateService);

  if (userProfileState.hasProfile()) {
    return true;
  }

  return userProfileService.getMyProfile().pipe(
    map(() => true),
    catchError((error) => {
      if (error.status === 404) {
        router.navigate(['/complete-profile']);
        return of(false);
      }
      router.navigate(['/login']);
      return of(false);
    }),
  );
};
