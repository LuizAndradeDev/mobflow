import { Routes } from '@angular/router';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { DashboardComponent } from './features/auth/pages/dashboard/dashboard.component';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'signup', component: RegisterComponent, canActivate: [guestGuard] },
  { path: 'dashboard', component: DashboardComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
