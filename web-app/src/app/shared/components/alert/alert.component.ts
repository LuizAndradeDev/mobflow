import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService, AlertState } from './service/alert.service';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-3 w-80 pointer-events-none">
      @for (alert of alertService.alerts(); track alert.id) {
        <div
          class="pointer-events-auto flex items-stretch bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-100 dark:border-gray-700 overflow-hidden"
          [class.alert-enter]="!alert.leaving"
          [class.alert-leave]="alert.leaving"
          role="alert"
        >
          <div class="w-1.5 flex-shrink-0" [ngClass]="barClass(alert)"></div>

          <div class="flex-1 px-4 py-3">
            <p class="text-sm font-semibold text-gray-800 dark:text-gray-100">{{ alert.title }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{{ alert.message }}</p>
          </div>

          <button
            (click)="alertService.dismiss(alert.id)"
            class="self-start p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 transition"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes slideIn {
      from { opacity: 0; transform: translateX(110%); }
      to   { opacity: 1; transform: translateX(0); }
    }
    @keyframes slideOut {
      from { opacity: 1; transform: translateX(0); }
      to   { opacity: 0; transform: translateX(110%); }
    }
    .alert-enter { animation: slideIn 0.25s ease-out forwards; }
    .alert-leave { animation: slideOut 0.3s ease-in forwards; }
  `],
})
export class AlertComponent {
  constructor(public alertService: AlertService) {}

  barClass(alert: AlertState): string {
    const map: Record<string, string> = {
      success: 'bg-emerald-500',
      error: 'bg-red-500',
      warning: 'bg-amber-400',
      info: 'bg-blue-500',
    };
    return map[alert.alertType] ?? 'bg-gray-400';
  }
}
