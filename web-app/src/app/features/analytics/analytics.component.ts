import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8 max-w-5xl mx-auto">

      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Analytics</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          {{ scope === 'team' ? 'Team performance overview' : 'Your personal productivity metrics' }}
        </p>
      </div>

      <!-- KPI cards -->
      <div class="grid grid-cols-4 gap-4 mb-8">
        @for (kpi of (scope === 'team' ? teamKpis : personalKpis); track kpi.label) {
          <div class="bg-white dark:bg-gray-800 rounded-2xl p-5 border border-gray-100 dark:border-gray-700 shadow-sm">
            <div class="flex items-center justify-between mb-3">
              <p class="text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wider">{{ kpi.label }}</p>
              <span class="text-lg">{{ kpi.icon }}</span>
            </div>
            <p class="text-3xl font-bold text-gray-800 dark:text-gray-100">{{ kpi.value }}</p>
            <div class="flex items-center gap-1 mt-1">
              <span class="text-xs font-medium" [class.text-emerald-500]="kpi.up" [class.text-red-400]="!kpi.up">
                {{ kpi.up ? '▲' : '▼' }} {{ kpi.delta }}
              </span>
              <span class="text-xs text-gray-400">vs last week</span>
            </div>
          </div>
        }
      </div>

      <!-- Charts mockup -->
      <div class="grid grid-cols-2 gap-6 mb-6">
        <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-100 dark:border-gray-700 shadow-sm">
          <h3 class="font-semibold text-gray-700 dark:text-gray-200 mb-4">Tasks completed per day</h3>
          <div class="flex items-end gap-2 h-32">
            @for (bar of chartBars; track $index) {
              <div class="flex-1 flex flex-col items-center gap-1">
                <div
                  class="w-full rounded-t-md bg-blue-500 dark:bg-blue-600 opacity-80 hover:opacity-100 transition"
                  [style.height.%]="bar.pct"
                ></div>
                <span class="text-xs text-gray-400">{{ bar.day }}</span>
              </div>
            }
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-100 dark:border-gray-700 shadow-sm">
          <h3 class="font-semibold text-gray-700 dark:text-gray-200 mb-4">Tasks by status</h3>
          <div class="space-y-3 mt-2">
            @for (item of statusBreakdown; track item.label) {
              <div>
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs text-gray-600 dark:text-gray-300">{{ item.label }}</span>
                  <span class="text-xs font-semibold text-gray-700 dark:text-gray-200">{{ item.value }}%</span>
                </div>
                <div class="h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                  <div class="h-full rounded-full" [ngClass]="item.color" [style.width.%]="item.value"></div>
                </div>
              </div>
            }
          </div>
        </div>
      </div>

      <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-2xl px-5 py-4 text-sm text-amber-700 dark:text-amber-300">
        📊 These are sample metrics. Real analytics will be available once task tracking is implemented.
      </div>

    </div>
  `,
})
export class AnalyticsComponent implements OnInit {
  scope: 'personal' | 'team' = 'personal';

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.queryParams.subscribe((p) => {
      this.scope = p['scope'] === 'team' ? 'team' : 'personal';
    });
  }

  personalKpis = [
    { label: 'Tasks Done', value: '34', delta: '8', up: true, icon: '✅' },
    { label: 'In Progress', value: '7', delta: '2', up: true, icon: '🔄' },
    { label: 'Overdue', value: '2', delta: '1', up: false, icon: '⚠️' },
    { label: 'Streak', value: '5d', delta: '2d', up: true, icon: '🔥' },
  ];

  teamKpis = [
    { label: 'Total Tasks', value: '142', delta: '23', up: true, icon: '📋' },
    { label: 'Completed', value: '98', delta: '15', up: true, icon: '✅' },
    { label: 'Members', value: '8', delta: '1', up: true, icon: '👥' },
    { label: 'Velocity', value: '92%', delta: '5%', up: true, icon: '🚀' },
  ];

  chartBars = [
    { day: 'Mon', pct: 60 }, { day: 'Tue', pct: 80 }, { day: 'Wed', pct: 45 },
    { day: 'Thu', pct: 90 }, { day: 'Fri', pct: 70 }, { day: 'Sat', pct: 30 }, { day: 'Sun', pct: 20 },
  ];

  statusBreakdown = [
    { label: 'Done', value: 58, color: 'bg-emerald-500' },
    { label: 'In Progress', value: 24, color: 'bg-blue-500' },
    { label: 'Todo', value: 13, color: 'bg-gray-400' },
    { label: 'Overdue', value: 5, color: 'bg-red-500' },
  ];
}
