import { Injectable, signal } from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { AlertService } from '../../shared/components/alert/service/alert.service';
import { NotificationService } from './notification.service';

@Injectable({ providedIn: 'root' })
export class NotificationStateService {
  private readonly _unreadCount = signal(0);
  private pollingSubscription: Subscription | null = null;
  private lastUnreadCount: number | null = null;

  readonly unreadCount = this._unreadCount.asReadonly();

  constructor(
    private notificationService: NotificationService,
    private alertService: AlertService,
  ) {}

  loadUnreadCount() {
    if (!localStorage.getItem('token')) {
      this.lastUnreadCount = 0;
      this._unreadCount.set(0);
      return;
    }

    this.notificationService.getUnreadCount().subscribe({
      next: (response) => {
        this.updateUnreadCount(response.unreadCount, true);
      },
      error: () => {
        this.lastUnreadCount = 0;
        this._unreadCount.set(0);
      },
    });
  }

  setUnreadCount(count: number) {
    const normalizedCount = Math.max(0, count);
    this.lastUnreadCount = normalizedCount;
    this._unreadCount.set(normalizedCount);
  }

  decreaseUnreadCount() {
    const nextCount = Math.max(0, this._unreadCount() - 1);
    this.lastUnreadCount = nextCount;
    this._unreadCount.set(nextCount);
  }

  clear() {
    this.stopPolling();
    this.lastUnreadCount = 0;
    this._unreadCount.set(0);
  }

  startPolling(intervalMs = 10000) {
    if (this.pollingSubscription || !localStorage.getItem('token')) {
      return;
    }

    this.loadUnreadCount();
    this.pollingSubscription = interval(intervalMs).subscribe(() => this.loadUnreadCount());
  }

  stopPolling() {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = null;
  }

  private updateUnreadCount(nextCount: number, showAlert: boolean) {
    const normalizedCount = Math.max(0, nextCount);
    const previousCount = this.lastUnreadCount;

    this._unreadCount.set(normalizedCount);
    this.lastUnreadCount = normalizedCount;

    if (showAlert && previousCount !== null && normalizedCount > previousCount) {
      const newNotifications = normalizedCount - previousCount;
      this.alertService.info(
        newNotifications === 1
          ? 'You have a new notification.'
          : `You have ${newNotifications} new notifications.`,
        'Notifications',
      );
    }
  }
}
