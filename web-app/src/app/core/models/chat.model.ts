export type ConversationType = 'PRIVATE';
export type MessageContentType = 'TEXT';
export type MessageStatus = 'SENT' | 'READ';
export type ChatEventType = 'MESSAGE_CREATED' | 'CONVERSATION_READ';
export type ChatConnectionState = 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'error';

export interface ConversationLastMessage {
  messageId: string;
  senderId: string;
  contentPreview: string;
  contentType: MessageContentType;
  createdAt: string;
}

export interface Conversation {
  id: string;
  type: ConversationType;
  participantIds: string[];
  counterpartAuthId: string;
  lastMessage: ConversationLastMessage | null;
  lastActivityAt: string;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationUpsertResponse {
  created: boolean;
  conversation: Conversation;
}

export interface MarkConversationReadResponse {
  conversationId: string;
  markedCount: number;
  unreadCount: number;
  readAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  contentType: MessageContentType;
  status: MessageStatus;
  readBy: string[];
  createdAt: string;
  ownMessage: boolean;
  readByCurrentUser: boolean;
}

export interface ConversationReadReceipt {
  conversationId: string;
  readerAuthId: string;
  readCount: number;
  readAt: string;
}

export interface ConversationTopicEvent {
  eventType: ChatEventType;
  conversationId: string;
  message: Message | null;
  readReceipt: ConversationReadReceipt | null;
  occurredAt: string;
}

export interface UserQueueEvent {
  eventType: ChatEventType;
  conversation: Conversation | null;
  message: Message | null;
  occurredAt: string;
}

export interface ChatSocketError {
  code: string;
  message: string;
  timestamp: string;
}
