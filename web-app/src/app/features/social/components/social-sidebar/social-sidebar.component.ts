import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Conversation } from '../../../../core/models/chat.model';
import { Friend } from '../../../../core/models/social.model';
import {
  ConversationListItemComponent,
  ConversationListItemViewModel,
} from '../conversation-list-item/conversation-list-item.component';
import { FriendListItemComponent } from '../friend-list-item/friend-list-item.component';

export type SocialSidebarMode = 'conversations' | 'friends';

@Component({
  selector: 'app-social-sidebar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FriendListItemComponent, ConversationListItemComponent],
  templateUrl: './social-sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SocialSidebarComponent {
  @Input({ required: true }) username = '';
  @Input() userAvatarUrl: string | null = null;
  @Input({ required: true }) friendSearch!: FormControl<string>;
  @Input({ required: true }) conversations: ConversationListItemViewModel[] = [];
  @Input({ required: true }) friends: Friend[] = [];
  @Input() sidebarMode: SocialSidebarMode = 'conversations';
  @Input() conversationCount = 0;
  @Input() totalUnreadCount = 0;
  @Input() selectedConversationId: string | null = null;
  @Input() selectedFriendId: string | null = null;
  @Input() isLoadingConversations = false;
  @Input() isLoadingFriends = false;

  @Output() readonly addFriend = new EventEmitter<void>();
  @Output() readonly openPendingRequests = new EventEmitter<void>();
  @Output() readonly changeMode = new EventEmitter<SocialSidebarMode>();
  @Output() readonly selectConversation = new EventEmitter<Conversation>();
  @Output() readonly selectFriend = new EventEmitter<Friend>();

  get hasUserAvatar(): boolean {
    return !!this.userAvatarUrl?.trim();
  }

  get userInitial(): string {
    return this.username.slice(0, 1).toUpperCase();
  }

  trackConversation(_: number, item: ConversationListItemViewModel): string {
    return item.conversation.id;
  }

  trackFriend(_: number, friend: Friend): string {
    return friend.authId;
  }
}
