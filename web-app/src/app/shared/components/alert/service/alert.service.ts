import { Injectable, signal } from '@angular/core';
import { AlertInterface } from '../model/alert.interface';
import { AlertType } from '../enum/alert-type.enum';

export interface AlertState extends AlertInterface {
  leaving: boolean;
}

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly maxAlerts = 3;
  private readonly autoCloseMs = 5000;
  private readonly leaveMs = 300;

  readonly alerts = signal<AlertState[]>([]);

  show(alert: Omit<AlertInterface, 'id'>): void {
    const id = crypto.randomUUID();
    const newAlert: AlertState = { ...alert, id, leaving: false };

    this.alerts.update((current) => {
      const updated = [newAlert, ...current];
      return updated.slice(0, this.maxAlerts);
    });

    setTimeout(() => this.dismiss(id), this.autoCloseMs);
  }

  dismiss(id: string): void {
    this.alerts.update((current) =>
      current.map((a) => (a.id === id ? { ...a, leaving: true } : a)),
    );
    setTimeout(() => {
      this.alerts.update((current) => current.filter((a) => a.id !== id));
    }, this.leaveMs);
  }

  info(message: string, title = 'Info'): void {
    this.show({ title, message, alertType: AlertType.info });
  }

  success(message: string, title = 'Success!'): void {
    this.show({ title, message, alertType: AlertType.success });
  }

  warning(message: string, title = 'Warning'): void {
    this.show({ title, message, alertType: AlertType.warning });
  }

  danger(message: string, title = 'Error!'): void {
    this.show({ title, message, alertType: AlertType.error });
  }

  clear(): void {
    this.alerts.set([]);
  }
}
