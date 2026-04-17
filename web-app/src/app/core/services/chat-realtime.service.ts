import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';
import {
  ChatConnectionState,
  ChatSocketError,
  ConversationTopicEvent,
  UserQueueEvent,
} from '../models/chat.model';
import { BrowserStorageService } from './browser-storage.service';

@Injectable({ providedIn: 'root' })
export class ChatRealtimeService {
  private client: Client | null = null;
  private userQueueSubscription: StompSubscription | null = null;
  private errorSubscription: StompSubscription | null = null;
  private readonly conversationSubjects = new Map<string, Subject<ConversationTopicEvent>>();
  private readonly conversationObserverCount = new Map<string, number>();
  private readonly conversationSubscriptions = new Map<string, StompSubscription>();
  private readonly userEventsSubject = new Subject<UserQueueEvent>();
  private readonly socketErrorsSubject = new Subject<ChatSocketError>();
  private manualDisconnect = false;

  private readonly _connectionState = signal<ChatConnectionState>('disconnected');

  readonly connectionState = this._connectionState.asReadonly();

  constructor(private readonly storage: BrowserStorageService) {}

  connect(): void {
    const token = this.storage.getToken();
    if (!token) {
      this.disconnect();
      return;
    }

    if (this.client?.active) {
      return;
    }

    this.manualDisconnect = false;
    this._connectionState.set(this.client ? 'reconnecting' : 'connecting');

    this.client = new Client({
      brokerURL: this.resolveBrokerUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        this._connectionState.set('connected');
        this.subscribeToUserQueue();
        this.subscribeToSocketErrors();
        this.resubscribeConversationTopics();
      },
      onStompError: (frame) => {
        this._connectionState.set('error');
        this.socketErrorsSubject.next({
          code: 'STOMP_ERROR',
          message: frame.headers['message'] || 'The chat connection failed.',
          timestamp: new Date().toISOString(),
        });
      },
      onWebSocketClose: () => {
        this.unsubscribeActiveSubscriptions();
        if (this.manualDisconnect) {
          this._connectionState.set('disconnected');
          return;
        }

        this._connectionState.set('reconnecting');
      },
      onWebSocketError: () => {
        this._connectionState.set('error');
      },
      debug: () => undefined,
    });

    this.client.activate();
  }

  disconnect(): void {
    this.manualDisconnect = true;
    this.unsubscribeActiveSubscriptions();
    this.client?.deactivate();
    this.client = null;
    this._connectionState.set('disconnected');
  }

  observeUserEvents(): Observable<UserQueueEvent> {
    return this.userEventsSubject.asObservable();
  }

  observeSocketErrors(): Observable<ChatSocketError> {
    return this.socketErrorsSubject.asObservable();
  }

  observeConversation(conversationId: string): Observable<ConversationTopicEvent> {
    return new Observable<ConversationTopicEvent>((observer) => {
      const subject = this.ensureConversationSubject(conversationId);
      const innerSubscription = subject.subscribe(observer);
      this.conversationObserverCount.set(
        conversationId,
        (this.conversationObserverCount.get(conversationId) ?? 0) + 1,
      );
      this.subscribeConversationTopic(conversationId);

      return () => {
        innerSubscription.unsubscribe();
        const nextCount = Math.max(0, (this.conversationObserverCount.get(conversationId) ?? 1) - 1);
        if (nextCount === 0) {
          this.conversationObserverCount.delete(conversationId);
          this.conversationSubscriptions.get(conversationId)?.unsubscribe();
          this.conversationSubscriptions.delete(conversationId);
          this.conversationSubjects.get(conversationId)?.complete();
          this.conversationSubjects.delete(conversationId);
          return;
        }

        this.conversationObserverCount.set(conversationId, nextCount);
      };
    });
  }

  sendMessage(conversationId: string, content: string): void {
    if (!this.client?.connected) {
      throw new Error('CHAT_SOCKET_UNAVAILABLE');
    }

    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ conversationId, content }),
    });
  }

  private resolveBrokerUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/chat/ws/chat`;
  }

  private ensureConversationSubject(conversationId: string): Subject<ConversationTopicEvent> {
    let subject = this.conversationSubjects.get(conversationId);
    if (!subject) {
      subject = new Subject<ConversationTopicEvent>();
      this.conversationSubjects.set(conversationId, subject);
    }

    return subject;
  }

  private subscribeToUserQueue(): void {
    if (!this.client?.connected || this.userQueueSubscription) {
      return;
    }

    this.userQueueSubscription = this.client.subscribe('/user/queue/messages', (message) => {
      this.userEventsSubject.next(this.parseFrame<UserQueueEvent>(message));
    });
  }

  private subscribeToSocketErrors(): void {
    if (!this.client?.connected || this.errorSubscription) {
      return;
    }

    this.errorSubscription = this.client.subscribe('/user/queue/errors', (message) => {
      this.socketErrorsSubject.next(this.parseFrame<ChatSocketError>(message));
    });
  }

  private subscribeConversationTopic(conversationId: string): void {
    if (!this.client?.connected || this.conversationSubscriptions.has(conversationId)) {
      return;
    }

    const subject = this.ensureConversationSubject(conversationId);
    const subscription = this.client.subscribe(`/topic/conversations/${conversationId}`, (message) => {
      subject.next(this.parseFrame<ConversationTopicEvent>(message));
    });

    this.conversationSubscriptions.set(conversationId, subscription);
  }

  private resubscribeConversationTopics(): void {
    for (const conversationId of this.conversationSubjects.keys()) {
      this.subscribeConversationTopic(conversationId);
    }
  }

  private unsubscribeActiveSubscriptions(): void {
    this.userQueueSubscription?.unsubscribe();
    this.errorSubscription?.unsubscribe();
    this.userQueueSubscription = null;
    this.errorSubscription = null;

    for (const subscription of this.conversationSubscriptions.values()) {
      subscription.unsubscribe();
    }

    this.conversationSubscriptions.clear();
  }

  private parseFrame<T>(message: IMessage): T {
    return JSON.parse(message.body) as T;
  }
}
