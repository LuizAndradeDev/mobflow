import { Injectable, signal } from '@angular/core';
import { AlertInterface } from '../model/alert.interface';
import { AlertType } from '../enum/alert-type.enum';

export interface AlertState extends AlertInterface {
  leaving: boolean;
}

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly MAX_ALERTS = 3;
  private readonly AUTO_CLOSE_MS = 5000;
  private readonly LEAVE_MS = 300;

  alerts = signal<AlertState[]>([]);

  show(alert: Omit<AlertInterface, 'id'>) {
    const id = crypto.randomUUID();
    const newAlert: AlertState = { ...alert, id, leaving: false };

    this.alerts.update((current) => {
      const updated = [newAlert, ...current];
      return updated.slice(0, this.MAX_ALERTS);
    });

    setTimeout(() => this.dismiss(id), this.AUTO_CLOSE_MS);
  }

  dismiss(id: string) {
    // mark as leaving first — triggers CSS animation
    this.alerts.update((current) =>
      current.map((a) => (a.id === id ? { ...a, leaving: true } : a)),
    );
    // remove after animation completes
    setTimeout(() => {
      this.alerts.update((current) => current.filter((a) => a.id !== id));
    }, this.LEAVE_MS);
  }

  info(message: string, title = 'Info') {
    this.show({ title, message, alertType: AlertType.info });
  }

  success(message: string, title = 'Success!') {
    this.show({ title, message, alertType: AlertType.success });
  }

  warning(message: string, title = 'Warning') {
    this.show({ title, message, alertType: AlertType.warning });
  }

  danger(message: string, title = 'Error!') {
    this.show({ title, message, alertType: AlertType.error });
  }

  clear() {
    this.alerts.set([]);
  }
}
