import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Observable, Subject, of } from 'rxjs';
import { signal } from '@angular/core';
import { vi } from 'vitest';
import { SocialComponent } from './social.component';
import { ChatConnectionState, Conversation, ConversationTopicEvent, Message, UserQueueEvent } from '../../../../core/models/chat.model';
import { Friend } from '../../../../core/models/social.model';
import { ChatRealtimeService } from '../../../../core/services/chat-realtime.service';
import { ChatService } from '../../../../core/services/chat.service';
import { SocialService } from '../../../../core/services/social.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { ErrorHandlerService } from '../../../../core/services/error-handler.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { UserStateService } from '../../../../core/services/user-state.service';
import { ActiveChatStateService } from '../../../../core/services/active-chat-state.service';

class ChatRealtimeServiceMock {
  readonly connectionState = signal<ChatConnectionState>('connected').asReadonly();
  readonly userEvents$ = new Subject<UserQueueEvent>();
  readonly socketErrors$ = new Subject<{ code: string; message: string; timestamp: string }>();
  readonly conversationStreams = new Map<string, Subject<ConversationTopicEvent>>();
  readonly sendMessage = vi.fn();

  connect(): void {}

  disconnect(): void {}

  observeUserEvents(): Observable<UserQueueEvent> {
    return this.userEvents$.asObservable();
  }

  observeSocketErrors(): Observable<{ code: string; message: string; timestamp: string }> {
    return this.socketErrors$.asObservable();
  }

  observeConversation(conversationId: string): Observable<ConversationTopicEvent> {
    const stream = this.ensureConversationStream(conversationId);
    return stream.asObservable();
  }

  emitConversationEvent(conversationId: string, event: ConversationTopicEvent): void {
    this.ensureConversationStream(conversationId).next(event);
  }

  private ensureConversationStream(conversationId: string): Subject<ConversationTopicEvent> {
    let stream = this.conversationStreams.get(conversationId);
    if (!stream) {
      stream = new Subject<ConversationTopicEvent>();
      this.conversationStreams.set(conversationId, stream);
    }

    return stream;
  }
}

describe('SocialComponent', () => {
  const currentAuthId = 'auth-current';
  const routeParams$ = new BehaviorSubject(convertToParamMap({}));
  const conversation: Conversation = {
    id: 'conversation-1',
    type: 'PRIVATE',
    participantIds: [currentAuthId, 'auth-friend'],
    counterpartAuthId: 'auth-friend',
    lastMessage: null,
    lastActivityAt: '2026-04-17T12:00:00.000Z',
    unreadCount: 0,
    createdAt: '2026-04-17T12:00:00.000Z',
    updatedAt: '2026-04-17T12:00:00.000Z',
  };
  const friend: Friend = {
    authId: 'auth-friend',
    username: 'friend',
    avatarUrl: null,
    friendsSince: '2026-04-10T12:00:00.000Z',
  };

  let chatRealtime: ChatRealtimeServiceMock;
  let alertService: Pick<AlertService, 'show' | 'success' | 'info'>;
  let router: Pick<Router, 'navigate'>;

  beforeEach(async () => {
    chatRealtime = new ChatRealtimeServiceMock();
    alertService = {
      show: vi.fn(),
      success: vi.fn(),
      info: vi.fn(),
    };
    router = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [SocialComponent],
      providers: [
        ActiveChatStateService,
        UserStateService,
        UserProfileStateService,
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: routeParams$.asObservable(),
          },
        },
        {
          provide: Router,
          useValue: router,
        },
        {
          provide: ChatRealtimeService,
          useValue: chatRealtime,
        },
        {
          provide: ChatService,
          useValue: {
            listConversations: () => of([conversation]),
            getConversation: () => of(conversation),
            createOrGetPrivateConversation: () => of({ created: true, conversation }),
            listMessages: () => of({
              content: [],
              totalPages: 1,
              totalElements: 0,
              size: 20,
              number: 0,
              numberOfElements: 0,
              first: true,
              last: true,
              empty: true,
            }),
            markConversationAsRead: () => of({
              conversationId: conversation.id,
              markedCount: 0,
              unreadCount: 0,
              readAt: '2026-04-17T12:00:00.000Z',
            }),
          },
        },
        {
          provide: SocialService,
          useValue: {
            listFriends: () => of([friend]),
            listFriendRequests: () => of([]),
            sendFriendRequest: () => of(null),
            acceptFriendRequest: () => of(null),
            declineFriendRequest: () => of(null),
          },
        },
        {
          provide: AlertService,
          useValue: alertService,
        },
        {
          provide: ErrorHandlerService,
          useValue: {
            mapHttpErrorToAlert: () => ({
              title: 'Error',
              message: 'Mapped error',
              alertType: 'error',
            }),
          },
        },
      ],
    }).compileComponents();

    TestBed.inject(UserProfileStateService).set({
      id: 'profile-1',
      authId: currentAuthId,
      displayName: 'Current User',
      bio: null,
      avatarUrl: null,
      phone: null,
      createdAt: '2026-04-17T10:00:00.000Z',
      updatedAt: '2026-04-17T10:00:00.000Z',
    });
    TestBed.inject(UserStateService).set({
      username: 'current',
      email: 'current@mobflow.test',
    });
  });

  afterEach(() => {
    routeParams$.next(convertToParamMap({}));
  });

  it('finaliza o estado de envio quando o ack chega pelo tópico da conversa', () => {
    const fixture = TestBed.createComponent(SocialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.selectConversation(conversation);
    component.messageControl.setValue('hello world');
    component.sendMessage();

    expect(component.isSendingMessage).toBe(true);
    expect(chatRealtime.sendMessage).toHaveBeenCalledWith(conversation.id, 'hello world');

    chatRealtime.emitConversationEvent(conversation.id, {
      eventType: 'MESSAGE_CREATED',
      conversationId: conversation.id,
      message: createMessage({
        id: 'message-1',
        content: 'hello world',
        senderId: currentAuthId,
        ownMessage: false,
      }),
      readReceipt: null,
      occurredAt: '2026-04-17T12:01:00.000Z',
    });

    expect(component.isSendingMessage).toBe(false);
    expect(component.messages.length).toBe(1);
    expect(component.messages[0].id).toBe('message-1');
    expect(component.messages[0].sendState).toBe('sent');
    expect(component.messages[0].ownMessage).toBe(true);
  });

  it('permite enviar multiplas mensagens em sequencia sem travar o composer', () => {
    const fixture = TestBed.createComponent(SocialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.selectConversation(conversation);

    component.messageControl.setValue('first');
    component.sendMessage();
    chatRealtime.emitConversationEvent(conversation.id, {
      eventType: 'MESSAGE_CREATED',
      conversationId: conversation.id,
      message: createMessage({
        id: 'message-1',
        content: 'first',
        senderId: currentAuthId,
        ownMessage: false,
      }),
      readReceipt: null,
      occurredAt: '2026-04-17T12:01:00.000Z',
    });

    component.messageControl.setValue('second');
    component.sendMessage();
    chatRealtime.emitConversationEvent(conversation.id, {
      eventType: 'MESSAGE_CREATED',
      conversationId: conversation.id,
      message: createMessage({
        id: 'message-2',
        content: 'second',
        senderId: currentAuthId,
        ownMessage: false,
      }),
      readReceipt: null,
      occurredAt: '2026-04-17T12:02:00.000Z',
    });

    expect(chatRealtime.sendMessage.mock.calls.length).toBe(2);
    expect(component.isSendingMessage).toBe(false);
    expect(component.messages.map((message) => message.content)).toEqual(['first', 'second']);
    expect(component.messageControl.getRawValue()).toBe('');
  });

  it('nao exibe notificacao quando a conversa recebida esta aberta', () => {
    const fixture = TestBed.createComponent(SocialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.selectConversation(conversation);

    chatRealtime.userEvents$.next({
      eventType: 'MESSAGE_CREATED',
      conversation,
      message: createMessage({
        id: 'message-incoming',
        content: 'ping',
        senderId: friend.authId,
        ownMessage: false,
      }),
      occurredAt: '2026-04-17T12:03:00.000Z',
    });

    expect(alertService.info).not.toHaveBeenCalled();
  });

  it('exibe notificacao quando a mensagem chega fora da conversa ativa', () => {
    const fixture = TestBed.createComponent(SocialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    chatRealtime.userEvents$.next({
      eventType: 'MESSAGE_CREATED',
      conversation,
      message: createMessage({
        id: 'message-incoming',
        content: 'ping',
        senderId: friend.authId,
        ownMessage: false,
      }),
      occurredAt: '2026-04-17T12:03:00.000Z',
    });

    expect(alertService.info).toHaveBeenCalledWith('New message from @friend.', 'Messages');
  });

  it('aplica contraste legivel para mensagens recebidas no dark mode', () => {
    const fixture = TestBed.createComponent(SocialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.selectConversation(conversation);
    component.messages = [
      {
        ...createMessage({
          id: 'message-incoming',
          content: 'Readable message',
          senderId: friend.authId,
          ownMessage: false,
        }),
        clientId: null,
        sendState: 'sent',
      },
    ];
    fixture.detectChanges();

    const messageBubble = fixture.nativeElement.querySelector('article');

    expect(messageBubble.className).toContain('dark:bg-slate-800');
    expect(messageBubble.className).toContain('dark:text-slate-100');
  });
});

function createMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: 'message-default',
    conversationId: 'conversation-1',
    senderId: 'auth-friend',
    content: 'message',
    contentType: 'TEXT',
    status: 'SENT',
    readBy: [],
    createdAt: '2026-04-17T12:01:00.000Z',
    ownMessage: false,
    readByCurrentUser: false,
    ...overrides,
  };
}
