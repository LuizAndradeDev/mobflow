import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { ConfirmEmailComponent } from './features/auth/pages/confirm-email/confirm-email.component';
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
import { NotificationsComponent } from './features/notifications/pages/notifications/notifications.component';
import { SocialComponent } from './features/social/pages/social/social.component';
import { MainLayoutComponent } from './shared/components/sidebar/main-layout.component';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';
import { profileGuard } from './core/guards/profile.guard';

const protectedRouteGuards = [profileGuard];

export const routes: Routes = [
  { path: '', component: LandingComponent, canActivate: [guestGuard] },
  { path: 'confirm-email', component: ConfirmEmailComponent },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'signup', component: RegisterComponent, canActivate: [guestGuard] },
  { path: 'complete-profile', component: CompleteProfileComponent, canActivate: [authGuard] },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, canActivate: protectedRouteGuards },
      { path: 'home', component: HomeComponent, canActivate: protectedRouteGuards },
      { path: 'analytics', component: AnalyticsComponent, canActivate: protectedRouteGuards },
      { path: 'settings', component: SettingsComponent },
      { path: 'profile/edit', component: EditProfileComponent, canActivate: protectedRouteGuards },
      { path: 'workspaces', component: WorkspaceListComponent, canActivate: protectedRouteGuards },
      { path: 'workspaces/:id', component: WorkspaceDetailComponent, canActivate: protectedRouteGuards },
      { path: 'tasks', component: TasksOverviewComponent, canActivate: protectedRouteGuards },
      { path: 'tasks/:workspaceId', component: WorkspaceTasksComponent, canActivate: protectedRouteGuards },
      { path: 'notifications', component: NotificationsComponent, canActivate: protectedRouteGuards },
      { path: 'social', component: SocialComponent, canActivate: protectedRouteGuards },
    ],
  },
  { path: '**', redirectTo: '' },
];
