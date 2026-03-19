import { Routes } from '@angular/router';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { DashboardComponent } from './features/auth/pages/dashboard/dashboard.component';
import { CompleteProfileComponent } from './features/profile/pages/complete-profile/complete-profile.component';
import { EditProfileComponent } from './features/profile/pages/edit-profile/edit-profile.component';
import { WorkspaceListComponent } from './features/workspace/pages/workspace-list/workspace-list.component';
import { CreateWorkspaceComponent } from './features/workspace/pages/create-workspace/create-workspace.component';
import { WorkspaceDetailComponent } from './features/workspace/pages/workspace-detail/workspace-detail.component';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';
import { profileGuard } from './core/guards/profile.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'signup', component: RegisterComponent, canActivate: [guestGuard] },
  {
    path: 'complete-profile',
    component: CompleteProfileComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard, profileGuard],
  },
  {
    path: 'profile/edit',
    component: EditProfileComponent,
    canActivate: [authGuard, profileGuard],
  },
  {
    path: 'workspaces',
    component: WorkspaceListComponent,
    canActivate: [authGuard, profileGuard],
  },
  {
    path: 'workspaces/new',
    component: CreateWorkspaceComponent,
    canActivate: [authGuard, profileGuard],
  },
  {
    path: 'workspaces/:id',
    component: WorkspaceDetailComponent,
    canActivate: [authGuard, profileGuard],
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
