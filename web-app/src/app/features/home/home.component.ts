import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UserProfileStateService } from '../../core/services/user-profile-state.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="p-8 max-w-5xl mx-auto">

      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">
          Welcome back, {{ profile?.displayName || 'there' }} 👋
        </h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Here's what's happening across your teams</p>
      </div>

      <!-- Quick stats -->
      <div class="grid grid-cols-4 gap-4 mb-8">
        @for (stat of stats; track stat.label) {
          <div class="bg-white dark:bg-gray-800 rounded-2xl p-5 border border-gray-100 dark:border-gray-700 shadow-sm">
            <p class="text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wider mb-1">{{ stat.label }}</p>
            <p class="text-3xl font-bold text-gray-800 dark:text-gray-100">{{ stat.value }}</p>
            <p class="text-xs text-emerald-500 mt-1">{{ stat.change }}</p>
          </div>
        }
      </div>

      <!-- Recent activity + My teams -->
      <div class="grid grid-cols-3 gap-6">

        <!-- Recent tasks placeholder -->
        <div class="col-span-2 bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-100 dark:border-gray-700 shadow-sm">
          <div class="flex items-center justify-between mb-5">
            <h2 class="font-semibold text-gray-800 dark:text-gray-100">Recent Tasks</h2>
            <span class="text-xs bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 px-2 py-0.5 rounded-full">Coming soon</span>
          </div>
          <div class="space-y-3">
            @for (task of mockTasks; track task.title) {
              <div class="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-700/50">
                <div class="w-2 h-2 rounded-full flex-shrink-0" [ngClass]="task.color"></div>
                <div class="flex-1">
                  <p class="text-sm font-medium text-gray-700 dark:text-gray-200">{{ task.title }}</p>
                  <p class="text-xs text-gray-400">{{ task.team }} · Due {{ task.due }}</p>
                </div>
                <span class="text-xs px-2 py-0.5 rounded-full" [ngClass]="task.statusClass">{{ task.status }}</span>
              </div>
            }
          </div>
        </div>

        <!-- My teams shortcut -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-100 dark:border-gray-700 shadow-sm">
          <h2 class="font-semibold text-gray-800 dark:text-gray-100 mb-5">My Teams</h2>
          <div class="space-y-3">
            @for (team of mockTeams; track team.name) {
              <div class="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-700/50 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition">
                <div class="w-8 h-8 rounded-lg flex-shrink-0 flex items-center justify-center font-bold text-white text-xs" [ngClass]="team.color">
                  {{ team.name[0] }}
                </div>
                <div>
                  <p class="text-sm font-medium text-gray-700 dark:text-gray-200">{{ team.name }}</p>
                  <p class="text-xs text-gray-400">{{ team.members }} members</p>
                </div>
              </div>
            }
          </div>
          <a routerLink="/workspaces" class="mt-4 block text-center text-sm text-blue-600 dark:text-blue-400 hover:underline">
            View all teams →
          </a>
        </div>

      </div>
    </div>
  `,
})
export class HomeComponent {
  constructor(public userProfileState: UserProfileStateService) {}

  get profile() { return this.userProfileState.profile(); }

  stats = [
    { label: 'Open Tasks', value: '24', change: '+3 this week' },
    { label: 'Completed', value: '128', change: '+12 this week' },
    { label: 'Teams', value: '4', change: 'Active' },
    { label: 'Due Today', value: '5', change: '2 overdue' },
  ];

  mockTasks = [
    { title: 'Design new onboarding flow', team: 'Product', due: 'Today', status: 'In Progress', color: 'bg-blue-500', statusClass: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300' },
    { title: 'Fix auth token refresh bug', team: 'Backend', due: 'Tomorrow', status: 'Todo', color: 'bg-red-500', statusClass: 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300' },
    { title: 'Write unit tests for WorkspaceService', team: 'Backend', due: 'Mar 25', status: 'Todo', color: 'bg-purple-500', statusClass: 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300' },
    { title: 'Update landing page copy', team: 'Marketing', due: 'Mar 26', status: 'Review', color: 'bg-amber-500', statusClass: 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300' },
  ];

  mockTeams = [
    { name: 'Backend Squad', members: 4, color: 'bg-blue-600' },
    { name: 'Product', members: 6, color: 'bg-emerald-600' },
    { name: 'Marketing', members: 3, color: 'bg-purple-600' },
  ];
}
