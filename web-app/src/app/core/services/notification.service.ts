import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NotificationItem, UnreadCountResponse } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly API = 'http://localhost:8084/api/notifications';

  constructor(private http: HttpClient) {}

  listAll() {
    return this.http.get<NotificationItem[]>(this.API);
  }

  getUnreadCount() {
    return this.http.get<UnreadCountResponse>(`${this.API}/unread-count`);
  }

  markAsRead(notificationId: string) {
    return this.http.patch<NotificationItem>(`${this.API}/${notificationId}/read`, {});
  }

  markAllAsRead() {
    return this.http.patch<UnreadCountResponse>(`${this.API}/read-all`, {});
  }
}
