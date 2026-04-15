export interface NotificationItem {
  id: string;
  recipientId: string;
  recipientEmail: string | null;
  type: string;
  channel: string;
  priority: string;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
  sentAt: string | null;
  deliveredAt: string | null;
  readAt: string | null;
  metadata: Record<string, string>;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
