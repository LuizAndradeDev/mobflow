import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { DashboardComponent } from './features/auth/pages/dashboard/dashboard.component';
import { HomeComponent } from './features/home/home.component';
import { AnalyticsComponent } from './features/analytics/analytics.component';
import { SettingsComponent } from './features/settings/settings.component';
import { CompleteProfileComponent } from './features/profile/pages/complete-profile/complete-profile.component';
import { EditProfileComponent } from './features/profile/pages/edit-profile/edit-profile.component';
import { WorkspaceListComponent } from './features/workspace/pages/workspace-list/workspace-list.component';
import { WorkspaceDetailComponent } from './features/workspace/pages/workspace-detail/workspace-detail.component';
import { TasksOverviewComponent } from './features/tasks/pages/tasks-overview/tasks-overview.component';
import { WorkspaceTasksComponent } from './features/tasks/pages/workspace-tasks/workspace-tasks.component';
import { MainLayoutComponent } from './shared/components/sidebar/main-layout.component';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';
import { profileGuard } from './core/guards/profile.guard';

export const routes: Routes = [
  // Public routes
  { path: '', component: LandingComponent, canActivate: [guestGuard] },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'signup', component: RegisterComponent, canActivate: [guestGuard] },
  { path: 'complete-profile', component: CompleteProfileComponent, canActivate: [authGuard] },

  // Authenticated routes (with sidebar layout)
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, canActivate: [profileGuard] },
      { path: 'home', component: HomeComponent, canActivate: [profileGuard] },
      { path: 'analytics', component: AnalyticsComponent, canActivate: [profileGuard] },
      { path: 'settings', component: SettingsComponent },
      { path: 'profile/edit', component: EditProfileComponent, canActivate: [profileGuard] },
      { path: 'workspaces', component: WorkspaceListComponent, canActivate: [profileGuard] },
      { path: 'workspaces/:id', component: WorkspaceDetailComponent, canActivate: [profileGuard] },
      { path: 'tasks', component: TasksOverviewComponent, canActivate: [profileGuard] },
      { path: 'tasks/:workspaceId', component: WorkspaceTasksComponent, canActivate: [profileGuard] },
    ],
  },

  { path: '**', redirectTo: '' },
];
