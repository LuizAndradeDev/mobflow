import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, finalize, forkJoin } from 'rxjs';
import {
  ChatConnectionState,
  Conversation,
  ConversationReadReceipt,
  Message,
  UserQueueEvent,
} from '../../../../core/models/chat.model';
import { Friend, FriendRequest } from '../../../../core/models/social.model';
import { ChatRealtimeService } from '../../../../core/services/chat-realtime.service';
import { ChatService } from '../../../../core/services/chat.service';
import { ErrorHandlerService } from '../../../../core/services/error-handler.service';
import { ActiveChatStateService } from '../../../../core/services/active-chat-state.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { UserStateService } from '../../../../core/services/user-state.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import {
  SocialSidebarComponent,
  SocialSidebarMode,
} from '../../components/social-sidebar/social-sidebar.component';
import { ConversationListItemViewModel } from '../../components/conversation-list-item/conversation-list-item.component';
import { SocialService } from '../../../../core/services/social.service';

interface SendFriendRequestForm {
  username: FormControl<string>;
}

interface ChatMessageViewModel extends Message {
  clientId: string | null;
  sendState: 'pending' | 'sent' | 'failed';
}

@Component({
  selector: 'app-social',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SocialSidebarComponent],
  templateUrl: './social.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SocialComponent implements OnInit, OnDestroy {
  @ViewChild('messageViewport') private messageViewport?: ElementRef<HTMLDivElement>;

  friends: Friend[] = [];
  requests: FriendRequest[] = [];
  conversations: Conversation[] = [];
  messages: ChatMessageViewModel[] = [];
  selectedConversation: Conversation | null = null;

  readonly friendSearch = new FormControl('', { nonNullable: true });
  readonly messageControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(4000)],
  });
  readonly sendFriendRequestForm: FormGroup<SendFriendRequestForm>;

  sidebarMode: SocialSidebarMode = 'conversations';
  isLoading = true;
  isLoadingMessages = false;
  isLoadingOlderMessages = false;
  isSubmitting = false;
  showAddFriendModal = false;
  showPendingRequestsModal = false;
  addFriendFeedback: { tone: 'success' | 'error'; message: string } | null = null;
  chatPanelError: string | null = null;
  readonly processingRequestIds = new Set<string>();

  private readonly pageSize = 20;
  private nextMessagesPage = 0;
  private hasMoreMessagesFlag = false;
  private isMarkingConversationRead = false;
  private pendingRouteConversationId: string | null = null;
  private selectedConversationSubscription: Subscription | null = null;
  private readonly subscriptions = new Subscription();

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly socialService: SocialService,
    private readonly chatService: ChatService,
    private readonly chatRealtime: ChatRealtimeService,
    private readonly alertService: AlertService,
    private readonly errorHandler: ErrorHandlerService,
    private readonly activeChatState: ActiveChatStateService,
    readonly userState: UserStateService,
    readonly userProfileState: UserProfileStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.sendFriendRequestForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9_]+$/)]],
    });
  }

  ngOnInit(): void {
    this.chatRealtime.connect();
    this.registerRealtimeSubscriptions();
    this.registerRouteSubscription();
    this.loadSocialData();
  }

  ngOnDestroy(): void {
    this.activeChatState.clear();
    this.selectedConversationSubscription?.unsubscribe();
    this.subscriptions.unsubscribe();
    this.chatRealtime.disconnect();
  }

  get incomingRequests(): FriendRequest[] {
    return this.requests
      .filter((request) => request.incoming && request.status === 'PENDING')
      .sort((left, right) => left.requesterUsername.localeCompare(right.requesterUsername));
  }

  get outgoingRequests(): FriendRequest[] {
    return this.requests
      .filter((request) => !request.incoming && request.status === 'PENDING')
      .sort((left, right) => left.targetUsername.localeCompare(right.targetUsername));
  }

  get filteredFriends(): Friend[] {
    const term = this.friendSearch.getRawValue().trim().toLowerCase();
    return [...this.friends]
      .sort((left, right) => left.username.localeCompare(right.username))
      .filter((friend) => !term || friend.username.toLowerCase().includes(term));
  }

  get filteredConversationItems(): ConversationListItemViewModel[] {
    const term = this.friendSearch.getRawValue().trim().toLowerCase();

    return [...this.conversations]
      .sort((left, right) => new Date(right.lastActivityAt).getTime() - new Date(left.lastActivityAt).getTime())
      .map((conversation) => {
        const counterpart = this.findFriend(conversation.counterpartAuthId);
        return {
          conversation,
          counterpartUsername: counterpart?.username ?? 'Unknown user',
          counterpartAvatarUrl: counterpart?.avatarUrl ?? null,
          previewText: conversation.lastMessage?.contentPreview?.trim() || 'No messages yet',
          unreadCount: conversation.unreadCount,
        };
      })
      .filter((item) => !term || item.counterpartUsername.toLowerCase().includes(term));
  }

  get currentUsername(): string {
    return this.userState.user()?.username ?? this.userProfileState.profile()?.displayName ?? 'You';
  }

  get currentUserAvatarUrl(): string | null {
    return this.userProfileState.profile()?.avatarUrl ?? null;
  }

  get currentAuthId(): string | null {
    return this.userProfileState.profile()?.authId ?? null;
  }

  get totalUnreadCount(): number {
    return this.conversations.reduce((total, conversation) => total + Math.max(0, conversation.unreadCount), 0);
  }

  get selectedConversationId(): string | null {
    return this.selectedConversation?.id ?? null;
  }

  get selectedFriendId(): string | null {
    return this.selectedConversation?.counterpartAuthId ?? null;
  }

  get selectedCounterpart(): Friend | null {
    return this.selectedConversation ? this.findFriend(this.selectedConversation.counterpartAuthId) ?? null : null;
  }

  get selectedConversationName(): string {
    return this.selectedCounterpart?.username ?? 'Conversation';
  }

  get selectedConversationAvatarUrl(): string | null {
    return this.selectedCounterpart?.avatarUrl ?? null;
  }

  get selectedConversationInitial(): string {
    return this.selectedConversationName.slice(0, 1).toUpperCase();
  }

  get selectedConversationSince(): string | null {
    return this.selectedCounterpart?.friendsSince ?? null;
  }

  get hasMoreMessages(): boolean {
    return this.hasMoreMessagesFlag;
  }

  get connectionState(): ChatConnectionState {
    return this.chatRealtime.connectionState();
  }

  get composerDisabled(): boolean {
    return !this.selectedConversation || this.connectionState === 'connecting' || this.connectionState === 'reconnecting';
  }

  get isSendingMessage(): boolean {
    return this.messages.some((message) => message.sendState === 'pending' && this.isOwnMessage(message));
  }

  loadSocialData(silent = false): void {
    if (!silent) {
      this.isLoading = true;
    }

    forkJoin({
      friends: this.socialService.listFriends(),
      requests: this.socialService.listFriendRequests(),
      conversations: this.chatService.listConversations(),
    }).subscribe({
      next: ({ friends, requests, conversations }) => {
        this.friends = friends;
        this.requests = requests;
        this.conversations = conversations;
        this.syncSelectedConversation();
        this.isLoading = false;
        this.tryOpenPendingRouteConversation();
        this.cdr.markForCheck();
      },
      error: (error) => {
        if (!silent) {
          this.friends = [];
          this.requests = [];
          this.conversations = [];
          this.selectedConversation = null;
          this.messages = [];
          this.isLoading = false;
        }
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
        this.cdr.markForCheck();
      },
    });
  }

  changeSidebarMode(mode: SocialSidebarMode): void {
    this.sidebarMode = mode;
  }

  selectConversation(conversation: Conversation): void {
    this.openConversation(conversation);
  }

  selectFriend(friend: Friend): void {
    this.sidebarMode = 'friends';
    const existingConversation = this.conversations.find(
      (conversation) => conversation.counterpartAuthId === friend.authId,
    );

    if (existingConversation) {
      this.openConversation(existingConversation);
      return;
    }

    this.chatService.createOrGetPrivateConversation(friend.authId).subscribe({
      next: ({ conversation }) => {
        this.upsertConversation(conversation);
        this.openConversation(conversation);
      },
      error: (error) => {
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  loadOlderMessages(): void {
    if (!this.selectedConversation || !this.hasMoreMessagesFlag || this.isLoadingOlderMessages || this.isLoadingMessages) {
      return;
    }

    const viewport = this.messageViewport?.nativeElement;
    const previousScrollHeight = viewport?.scrollHeight ?? 0;
    const previousScrollTop = viewport?.scrollTop ?? 0;

    this.isLoadingOlderMessages = true;
    this.chatService.listMessages(this.selectedConversation.id, this.nextMessagesPage, this.pageSize).subscribe({
      next: (response) => {
        const olderMessages = [...response.content].reverse().map((message) => this.toMessageViewModel(message));
        this.messages = [...olderMessages, ...this.messages];
        this.nextMessagesPage += 1;
        this.hasMoreMessagesFlag = !response.last;
        this.updateConversationUnreadCount(this.selectedConversation?.id ?? '', 0);
        this.isLoadingOlderMessages = false;
        this.cdr.markForCheck();
        this.restoreScrollPosition(previousScrollHeight, previousScrollTop);
      },
      error: (error) => {
        this.isLoadingOlderMessages = false;
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
        this.cdr.markForCheck();
      },
    });
  }

  sendMessage(): void {
    if (this.messageControl.invalid || !this.selectedConversation) {
      this.messageControl.markAsTouched();
      return;
    }

    const content = this.messageControl.getRawValue().trim();
    if (!content) {
      return;
    }

    const optimisticMessage = this.createOptimisticMessage(content, this.selectedConversation.id);
    this.messages = [...this.messages, optimisticMessage];
    this.bumpConversationPreview(this.selectedConversation.id, content, optimisticMessage.createdAt);
    this.messageControl.reset('');
    this.chatPanelError = null;
    this.cdr.markForCheck();
    this.scrollToBottom(true);

    try {
      this.chatRealtime.sendMessage(this.selectedConversation.id, content);
    } catch {
      this.markMessageAsFailed(optimisticMessage.clientId);
      this.chatPanelError = 'Unable to send right now. Reconnect and try again.';
      this.cdr.markForCheck();
    }
  }

  retryMessage(message: ChatMessageViewModel): void {
    if (message.sendState !== 'failed' || !message.clientId || !this.selectedConversation) {
      return;
    }

    this.messages = this.messages.map((item) =>
      item.clientId === message.clientId ? { ...item, sendState: 'pending' } : item,
    );
    this.chatPanelError = null;

    try {
      this.chatRealtime.sendMessage(this.selectedConversation.id, message.content);
    } catch {
      this.markMessageAsFailed(message.clientId);
      this.chatPanelError = 'Unable to send right now. Reconnect and try again.';
    }

    this.cdr.markForCheck();
  }

  onComposerKeydown(event: Event): void {
    if (!(event instanceof KeyboardEvent) || event.shiftKey) {
      return;
    }

    event.preventDefault();
    this.sendMessage();
  }

  onSendFriendRequest(): void {
    if (this.sendFriendRequestForm.invalid || this.isSubmitting) {
      this.sendFriendRequestForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.addFriendFeedback = null;
    const username = this.sendFriendRequestForm.getRawValue().username.trim();

    this.socialService.sendFriendRequest({ username }).pipe(
      finalize(() => {
        this.isSubmitting = false;
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: (request) => {
        this.requests = [request, ...this.requests];
        this.sendFriendRequestForm.reset({ username: '' });
        this.addFriendFeedback = { tone: 'success', message: 'Request sent' };
        this.alertService.success(`Friend request sent to @${request.targetUsername}.`, 'Request sent');
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.addFriendFeedback = {
          tone: 'error',
          message: error.status === 404 ? 'User not found' : this.errorHandler.mapHttpErrorToAlert(error).message,
        };
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  acceptRequest(request: FriendRequest): void {
    if (this.processingRequestIds.has(request.id)) {
      return;
    }

    this.processingRequestIds.add(request.id);
    this.socialService.acceptFriendRequest(request.id).pipe(
      finalize(() => {
        this.processingRequestIds.delete(request.id);
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: (updatedRequest) => {
        this.requests = this.requests.map((item) => item.id === request.id ? updatedRequest : item);
        this.loadSocialData(true);
        this.alertService.success(`You are now friends with @${request.requesterUsername}.`, 'Friend added');
      },
      error: (error) => {
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  declineRequest(request: FriendRequest): void {
    if (this.processingRequestIds.has(request.id)) {
      return;
    }

    this.processingRequestIds.add(request.id);
    this.socialService.declineFriendRequest(request.id).pipe(
      finalize(() => {
        this.processingRequestIds.delete(request.id);
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: (updatedRequest) => {
        this.requests = this.requests.map((item) => item.id === request.id ? updatedRequest : item);
        this.alertService.info(`Friend request from @${request.requesterUsername} declined.`, 'Request declined');
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  isProcessing(requestId: string): boolean {
    return this.processingRequestIds.has(requestId);
  }

  openAddFriendModal(): void {
    this.showAddFriendModal = true;
    this.addFriendFeedback = null;
  }

  closeAddFriendModal(): void {
    this.showAddFriendModal = false;
    this.sendFriendRequestForm.reset({ username: '' });
    this.addFriendFeedback = null;
  }

  openPendingRequestsModal(): void {
    this.showPendingRequestsModal = true;
  }

  closePendingRequestsModal(): void {
    this.showPendingRequestsModal = false;
  }

  clearSelectedConversation(): void {
    this.activeChatState.clear();
    this.selectedConversationSubscription?.unsubscribe();
    this.selectedConversationSubscription = null;
    this.selectedConversation = null;
    this.messages = [];
    this.chatPanelError = null;
    this.syncConversationQueryParam(null);
  }

  isOwnMessage(message: ChatMessageViewModel): boolean {
    return !!this.currentAuthId && message.senderId === this.currentAuthId;
  }

  private registerRealtimeSubscriptions(): void {
    this.subscriptions.add(
      this.chatRealtime.observeUserEvents().subscribe((event) => this.handleUserQueueEvent(event)),
    );

    this.subscriptions.add(
      this.chatRealtime.observeSocketErrors().subscribe((error) => {
        this.failPendingMessages();
        this.chatPanelError = error.message;
        this.cdr.markForCheck();
      }),
    );
  }

  private registerRouteSubscription(): void {
    this.subscriptions.add(
      this.route.queryParamMap.subscribe((params) => {
        const conversationId = params.get('conversationId');
        this.pendingRouteConversationId = conversationId;

        if (!conversationId) {
          if (this.selectedConversation) {
            this.activeChatState.clear();
            this.selectedConversationSubscription?.unsubscribe();
            this.selectedConversationSubscription = null;
            this.selectedConversation = null;
            this.messages = [];
            this.chatPanelError = null;
            this.cdr.markForCheck();
          }
          return;
        }

        if (this.selectedConversation?.id === conversationId) {
          return;
        }

        this.tryOpenPendingRouteConversation();
      }),
    );
  }

  private tryOpenPendingRouteConversation(): void {
    if (!this.pendingRouteConversationId || this.isLoading) {
      return;
    }

    const existingConversation = this.conversations.find(
      (conversation) => conversation.id === this.pendingRouteConversationId,
    );

    if (existingConversation) {
      this.openConversation(existingConversation, false);
      return;
    }

    this.chatService.getConversation(this.pendingRouteConversationId).subscribe({
      next: (conversation) => {
        this.upsertConversation(conversation);
        this.openConversation(conversation, false);
      },
      error: (error) => {
        this.pendingRouteConversationId = null;
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
        this.syncConversationQueryParam(null);
      },
    });
  }

  private openConversation(conversation: Conversation, syncRoute = true): void {
    this.sidebarMode = 'conversations';
    this.chatPanelError = null;
    this.selectedConversation = this.findConversation(conversation.id) ?? conversation;
    this.activeChatState.setActiveConversation(conversation.id);
    this.messages = [];
    this.nextMessagesPage = 0;
    this.hasMoreMessagesFlag = false;

    this.selectedConversationSubscription?.unsubscribe();
    this.selectedConversationSubscription = this.chatRealtime.observeConversation(conversation.id).subscribe((event) => {
      if (event.eventType === 'MESSAGE_CREATED' && event.message) {
        if (this.isCurrentUserSender(event.message) && this.hasPendingMessageFor(event.message)) {
          this.reconcileOptimisticMessage(event.message);
          return;
        }

        this.upsertSelectedMessage(event.message);
        if (!this.isCurrentUserSender(event.message)) {
          this.markConversationAsRead(conversation.id, false);
        }
      }

      if (event.eventType === 'CONVERSATION_READ' && event.readReceipt) {
        this.applyReadReceipt(event.readReceipt);
      }
    });

    this.loadConversationMessages(conversation.id);
    if (syncRoute) {
      this.syncConversationQueryParam(conversation.id);
    }
    this.cdr.markForCheck();
  }

  private loadConversationMessages(conversationId: string): void {
    this.isLoadingMessages = true;
    this.chatService.listMessages(conversationId, 0, this.pageSize).subscribe({
      next: (response) => {
        this.messages = [...response.content].reverse().map((message) => this.toMessageViewModel(message));
        this.nextMessagesPage = 1;
        this.hasMoreMessagesFlag = !response.last;
        this.isLoadingMessages = false;
        this.updateConversationUnreadCount(conversationId, 0);
        this.applyCurrentUserReadState();
        this.cdr.markForCheck();
        this.scrollToBottom(true);
      },
      error: (error) => {
        this.messages = [];
        this.isLoadingMessages = false;
        this.chatPanelError = this.errorHandler.mapHttpErrorToAlert(error).message;
        this.cdr.markForCheck();
      },
    });
  }

  private handleUserQueueEvent(event: UserQueueEvent): void {
    if (event.conversation) {
      this.upsertConversation(event.conversation);
    }

    if (!event.message || !event.conversation) {
      this.cdr.markForCheck();
      return;
    }

    if (!event.message.ownMessage && !this.activeChatState.isViewingConversation(event.conversation.id)) {
      const counterpartName = this.findFriend(event.conversation.counterpartAuthId)?.username ?? 'Someone';
      this.alertService.info(`New message from @${counterpartName}.`, 'Messages');
    }

    if (!this.selectedConversation || event.conversation.id !== this.selectedConversation.id) {
      this.cdr.markForCheck();
      return;
    }

    if (event.message.ownMessage) {
      this.reconcileOptimisticMessage(event.message);
    }

    this.cdr.markForCheck();
  }

  private reconcileOptimisticMessage(message: Message): void {
    const normalizedMessage = this.toMessageViewModel(message);
    const existingIndex = this.messages.findIndex((item) => item.id === message.id);
    if (existingIndex >= 0) {
      this.messages[existingIndex] = normalizedMessage;
      return;
    }

    const optimisticIndex = this.messages.findIndex((item) =>
      item.sendState !== 'sent'
      && item.conversationId === message.conversationId
      && item.content === message.content
      && this.isOwnMessage(item),
    );

    if (optimisticIndex >= 0) {
      this.messages[optimisticIndex] = {
        ...normalizedMessage,
        clientId: this.messages[optimisticIndex]?.clientId ?? null,
      };
      this.scrollToBottom(true);
      return;
    }

    this.upsertSelectedMessage(normalizedMessage);
  }

  private upsertSelectedMessage(message: Message | ChatMessageViewModel): void {
    if (!this.selectedConversation || message.conversationId !== this.selectedConversation.id) {
      return;
    }

    const existingIndex = this.messages.findIndex((item) => item.id === message.id);
    if (existingIndex >= 0) {
      this.messages[existingIndex] = this.toMessageViewModel(message);
      return;
    }

    this.messages = [...this.messages, this.toMessageViewModel(message)];
    this.scrollToBottom(this.shouldStickToBottom());
  }

  private applyReadReceipt(readReceipt: ConversationReadReceipt): void {
    if (!this.selectedConversation || readReceipt.conversationId !== this.selectedConversation.id) {
      return;
    }

    this.messages = this.messages.map((message) => {
      if (!this.isOwnMessage(message)) {
        return message;
      }

      const readBy = new Set(message.readBy);
      readBy.add(readReceipt.readerAuthId);

      return {
        ...message,
        readBy: [...readBy],
        status: 'READ',
      };
    });
  }

  private markConversationAsRead(conversationId: string, showError: boolean): void {
    if (this.isMarkingConversationRead) {
      return;
    }

    this.isMarkingConversationRead = true;
    this.chatService.markConversationAsRead(conversationId).subscribe({
      next: () => {
        this.updateConversationUnreadCount(conversationId, 0);
        this.applyCurrentUserReadState();
        this.isMarkingConversationRead = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.isMarkingConversationRead = false;
        if (showError) {
          this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
        }
        this.cdr.markForCheck();
      },
    });
  }

  private applyCurrentUserReadState(): void {
    const authId = this.currentAuthId;
    if (!authId) {
      return;
    }

    this.messages = this.messages.map((message) => {
      if (this.isOwnMessage(message)) {
        return message;
      }

      const readBy = new Set(message.readBy);
      readBy.add(authId);

      return {
        ...message,
        readBy: [...readBy],
        readByCurrentUser: true,
        status: 'READ',
      };
    });
  }

  private updateConversationUnreadCount(conversationId: string, unreadCount: number): void {
    this.conversations = this.conversations.map((conversation) =>
      conversation.id === conversationId
        ? {
            ...conversation,
            unreadCount,
          }
        : conversation,
    );
    this.syncSelectedConversation();
  }

  private bumpConversationPreview(conversationId: string, content: string, createdAt: string): void {
    this.conversations = this.conversations.map((conversation) =>
      conversation.id === conversationId
        ? {
            ...conversation,
            lastActivityAt: createdAt,
            lastMessage: {
              messageId: conversation.lastMessage?.messageId ?? `temp-${createdAt}`,
              senderId: this.currentAuthId ?? conversation.lastMessage?.senderId ?? '',
              contentPreview: content,
              contentType: 'TEXT',
              createdAt,
            },
            unreadCount: 0,
          }
        : conversation,
    );
    this.syncSelectedConversation();
  }

  private syncSelectedConversation(): void {
    if (!this.selectedConversation) {
      return;
    }

    this.selectedConversation = this.findConversation(this.selectedConversation.id) ?? this.selectedConversation;
  }

  private upsertConversation(conversation: Conversation): void {
    const existingIndex = this.conversations.findIndex((item) => item.id === conversation.id);
    if (existingIndex >= 0) {
      this.conversations[existingIndex] = conversation;
      this.conversations = [...this.conversations];
    } else {
      this.conversations = [conversation, ...this.conversations];
    }

    this.syncSelectedConversation();
  }

  private findFriend(authId: string): Friend | undefined {
    return this.friends.find((friend) => friend.authId === authId);
  }

  private findConversation(conversationId: string): Conversation | undefined {
    return this.conversations.find((conversation) => conversation.id === conversationId);
  }

  private toMessageViewModel(message: Message): ChatMessageViewModel {
    const ownMessage = this.isCurrentUserSender(message);
    const readByCurrentUser = ownMessage || message.readByCurrentUser || !!this.currentAuthId && message.readBy.includes(this.currentAuthId);

    return {
      ...message,
      ownMessage,
      readByCurrentUser,
      clientId: null,
      sendState: 'sent',
    };
  }

  private createOptimisticMessage(content: string, conversationId: string): ChatMessageViewModel {
    const timestamp = new Date().toISOString();
    const clientId = `temp-${crypto.randomUUID()}`;

    return {
      id: clientId,
      clientId,
      conversationId,
      senderId: this.currentAuthId ?? 'current-user',
      content,
      contentType: 'TEXT',
      status: 'SENT',
      readBy: this.currentAuthId ? [this.currentAuthId] : [],
      createdAt: timestamp,
      ownMessage: true,
      readByCurrentUser: true,
      sendState: 'pending',
    };
  }

  private hasPendingMessageFor(message: Message): boolean {
    return this.messages.some((item) =>
      item.sendState === 'pending'
      && item.conversationId === message.conversationId
      && item.content === message.content
      && this.isOwnMessage(item),
    );
  }

  private isCurrentUserSender(message: Message): boolean {
    return !!this.currentAuthId && message.senderId === this.currentAuthId;
  }

  private markMessageAsFailed(clientId: string | null): void {
    if (!clientId) {
      return;
    }

    this.messages = this.messages.map((message) =>
      message.clientId === clientId
        ? {
            ...message,
            sendState: 'failed',
          }
        : message,
    );
  }

  private failPendingMessages(): void {
    this.messages = this.messages.map((message) =>
      message.sendState === 'pending'
        ? {
            ...message,
            sendState: 'failed',
          }
        : message,
    );
  }

  private shouldStickToBottom(): boolean {
    const viewport = this.messageViewport?.nativeElement;
    if (!viewport) {
      return true;
    }

    return viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight < 96;
  }

  private scrollToBottom(force: boolean): void {
    if (!force && !this.shouldStickToBottom()) {
      return;
    }

    requestAnimationFrame(() => {
      const viewport = this.messageViewport?.nativeElement;
      if (!viewport) {
        return;
      }

      viewport.scrollTop = viewport.scrollHeight;
    });
  }

  private restoreScrollPosition(previousScrollHeight: number, previousScrollTop: number): void {
    requestAnimationFrame(() => {
      const viewport = this.messageViewport?.nativeElement;
      if (!viewport) {
        return;
      }

      viewport.scrollTop = viewport.scrollHeight - previousScrollHeight + previousScrollTop;
    });
  }

  private syncConversationQueryParam(conversationId: string | null): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { conversationId },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
