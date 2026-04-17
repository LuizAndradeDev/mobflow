import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Friend } from '../../../../core/models/social.model';

@Component({
  selector: 'app-friend-list-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './friend-list-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FriendListItemComponent {
  @Input({ required: true }) friend!: Friend;
  @Input() active = false;

  @Output() readonly selectFriend = new EventEmitter<Friend>();

  onSelect(): void {
    this.selectFriend.emit(this.friend);
  }

  get hasAvatar(): boolean {
    return !!this.friend.avatarUrl?.trim();
  }

  get initial(): string {
    return this.friend.username.slice(0, 1).toUpperCase();
  }
}
