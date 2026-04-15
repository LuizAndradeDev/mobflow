import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, interval } from 'rxjs';
import { NotificationItem } from '../../../../core/models/notification.model';
import { NotificationService } from '../../../../core/services/notification.service';
import { NotificationStateService } from '../../../../core/services/notification-state.service';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationsComponent implements OnInit, OnDestroy {
  notifications: NotificationItem[] = [];
  isLoading = true;
  isMarkingAll = false;
  processingInviteIds = new Set<string>();

  private pollingSubscription: Subscription | null = null;

  constructor(
    private notificationService: NotificationService,
    private notificationState: NotificationStateService,
    private workspaceService: WorkspaceService,
    private alertService: AlertService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.loadNotifications();
    this.pollingSubscription = interval(10000).subscribe(() => this.loadNotifications(true));
  }

  ngOnDestroy() {
    this.pollingSubscription?.unsubscribe();
  }

  get unreadCount(): number {
    return this.notifications.filter((notification) => !notification.read).length;
  }

  loadNotifications(silent = false) {
    if (!silent) {
      this.isLoading = true;
    }

    this.notificationService.listAll().subscribe({
      next: (notifications) => {
        this.notifications = notifications;
        this.notificationState.setUnreadCount(this.unreadCount);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        if (!silent) {
          this.notifications = [];
          this.notificationState.setUnreadCount(0);
          this.isLoading = false;
          this.cdr.markForCheck();
        }
      },
    });
  }

  canRespondToInvite(notification: NotificationItem): boolean {
    return notification.type === 'WORKSPACE_INVITE' && !notification.read && !!notification.metadata['inviteId'];
  }

  isProcessingInvite(notification: NotificationItem): boolean {
    return this.processingInviteIds.has(notification.id);
  }

  acceptInvite(notification: NotificationItem) {
    const inviteId = notification.metadata['inviteId'];
    if (!inviteId || this.isProcessingInvite(notification)) {
      return;
    }

    this.processingInviteIds.add(notification.id);
    this.workspaceService.acceptInvite(inviteId).subscribe({
      next: () => {
        this.workspaceService.invalidateListCache();
        this.alertService.success('You joined the workspace.', 'Invite accepted');
        this.finishInviteAction(notification);
      },
      error: () => {
        this.processingInviteIds.delete(notification.id);
        this.cdr.markForCheck();
      },
    });
  }

  declineInvite(notification: NotificationItem) {
    const inviteId = notification.metadata['inviteId'];
    if (!inviteId || this.isProcessingInvite(notification)) {
      return;
    }

    this.processingInviteIds.add(notification.id);
    this.workspaceService.declineInvite(inviteId).subscribe({
      next: () => {
        this.alertService.info('The workspace invitation was declined.', 'Invite declined');
        this.finishInviteAction(notification);
      },
      error: () => {
        this.processingInviteIds.delete(notification.id);
        this.cdr.markForCheck();
      },
    });
  }

  markAsRead(notification: NotificationItem) {
    if (notification.read) {
      return;
    }

    this.notificationService.markAsRead(notification.id).subscribe({
      next: (updated) => {
        this.notifications = this.notifications.map((item) =>
          item.id === updated.id ? updated : item,
        );
        this.notificationState.decreaseUnreadCount();
        this.cdr.markForCheck();
      },
    });
  }

  markAllAsRead() {
    if (this.unreadCount === 0 || this.isMarkingAll) {
      return;
    }

    this.isMarkingAll = true;
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications = this.notifications.map((notification) => ({
          ...notification,
          read: true,
          readAt: notification.readAt ?? new Date().toISOString(),
        }));
        this.notificationState.setUnreadCount(0);
        this.isMarkingAll = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isMarkingAll = false;
        this.cdr.markForCheck();
      },
    });
  }

  priorityClasses(priority: string): string {
    const classes: Record<string, string> = {
      HIGH: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
      MEDIUM: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
      LOW: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300',
    };

    return classes[priority] ?? classes['MEDIUM'];
  }

  private finishInviteAction(notification: NotificationItem) {
    this.processingInviteIds.delete(notification.id);
    if (notification.read) {
      this.loadNotifications(true);
      return;
    }

    this.notificationService.markAsRead(notification.id).subscribe({
      next: (updated) => {
        this.notifications = this.notifications.map((item) =>
          item.id === updated.id ? updated : item,
        );
        this.notificationState.decreaseUnreadCount();
        this.loadNotifications(true);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadNotifications(true);
      },
    });
  }
}
