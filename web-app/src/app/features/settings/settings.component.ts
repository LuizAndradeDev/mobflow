import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8 max-w-2xl mx-auto">

      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Settings</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Manage your preferences</p>
      </div>

      <!-- Appearance -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 shadow-sm p-6">
        <h2 class="font-semibold text-gray-800 dark:text-gray-100 mb-1">Appearance</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-6">Choose how Mobflow looks to you</p>

        <div class="flex gap-4">

          <!-- Light theme card -->
          <button
            (click)="themeService.setTheme('light')"
            class="flex-1 rounded-xl border-2 p-4 transition focus:outline-none"
            [class.border-blue-500]="!themeService.isDark()"
            [class.border-gray-200]="themeService.isDark()"
            [class.dark:border-blue-500]="!themeService.isDark()"
            [class.dark:border-gray-600]="themeService.isDark()"
          >
            <!-- Light preview -->
            <div class="rounded-lg overflow-hidden mb-3 border border-gray-200">
              <div class="h-6 bg-white flex items-center px-2 gap-1.5">
                <div class="w-1.5 h-1.5 rounded-full bg-gray-300"></div>
                <div class="w-8 h-1.5 rounded bg-gray-200"></div>
              </div>
              <div class="flex h-12 bg-gray-50">
                <div class="w-8 bg-white border-r border-gray-100"></div>
                <div class="flex-1 p-1.5 space-y-1">
                  <div class="h-2 bg-gray-200 rounded w-3/4"></div>
                  <div class="h-2 bg-gray-200 rounded w-1/2"></div>
                </div>
              </div>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium text-gray-700 dark:text-gray-200">Light</span>
              @if (!themeService.isDark()) {
                <div class="w-4 h-4 bg-blue-500 rounded-full flex items-center justify-center">
                  <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                  </svg>
                </div>
              }
            </div>
          </button>

          <!-- Dark theme card -->
          <button
            (click)="themeService.setTheme('dark')"
            class="flex-1 rounded-xl border-2 p-4 transition focus:outline-none"
            [class.border-blue-500]="themeService.isDark()"
            [class.border-gray-200]="!themeService.isDark()"
            [class.dark:border-blue-500]="themeService.isDark()"
            [class.dark:border-gray-600]="!themeService.isDark()"
          >
            <!-- Dark preview -->
            <div class="rounded-lg overflow-hidden mb-3 border border-gray-700">
              <div class="h-6 bg-gray-900 flex items-center px-2 gap-1.5">
                <div class="w-1.5 h-1.5 rounded-full bg-gray-600"></div>
                <div class="w-8 h-1.5 rounded bg-gray-700"></div>
              </div>
              <div class="flex h-12 bg-gray-950">
                <div class="w-8 bg-gray-900 border-r border-gray-800"></div>
                <div class="flex-1 p-1.5 space-y-1">
                  <div class="h-2 bg-gray-700 rounded w-3/4"></div>
                  <div class="h-2 bg-gray-700 rounded w-1/2"></div>
                </div>
              </div>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium text-gray-700 dark:text-gray-200">Dark</span>
              @if (themeService.isDark()) {
                <div class="w-4 h-4 bg-blue-500 rounded-full flex items-center justify-center">
                  <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                  </svg>
                </div>
              }
            </div>
          </button>

        </div>
      </div>

    </div>
  `,
})
export class SettingsComponent {
  constructor(public themeService: ThemeService) {}
}
