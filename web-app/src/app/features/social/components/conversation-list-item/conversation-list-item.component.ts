import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Conversation } from '../../../../core/models/chat.model';

export interface ConversationListItemViewModel {
  conversation: Conversation;
  counterpartUsername: string;
  counterpartAvatarUrl: string | null;
  previewText: string;
  unreadCount: number;
}

@Component({
  selector: 'app-conversation-list-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conversation-list-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConversationListItemComponent {
  @Input({ required: true }) item!: ConversationListItemViewModel;
  @Input() active = false;

  @Output() readonly selectConversation = new EventEmitter<Conversation>();

  onSelect(): void {
    this.selectConversation.emit(this.item.conversation);
  }

  get hasAvatar(): boolean {
    return !!this.item.counterpartAvatarUrl?.trim();
  }

  get initial(): string {
    return this.item.counterpartUsername.slice(0, 1).toUpperCase();
  }
}
