import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService, AlertState } from './service/alert.service';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.css',
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
