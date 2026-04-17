import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Friend } from '../../../../core/models/social.model';
import { FriendListItemComponent } from '../friend-list-item/friend-list-item.component';

@Component({
  selector: 'app-social-sidebar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FriendListItemComponent],
  templateUrl: './social-sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SocialSidebarComponent {
  @Input({ required: true }) username = '';
  @Input() userAvatarUrl: string | null = null;
  @Input({ required: true }) friendSearch!: FormControl<string>;
  @Input({ required: true }) friends: Friend[] = [];
  @Input() selectedFriendId: string | null = null;
  @Input() isLoading = false;

  @Output() readonly addFriend = new EventEmitter<void>();
  @Output() readonly openPendingRequests = new EventEmitter<void>();
  @Output() readonly selectFriend = new EventEmitter<Friend>();

  get hasUserAvatar(): boolean {
    return !!this.userAvatarUrl?.trim();
  }

  get userInitial(): string {
    return this.username.slice(0, 1).toUpperCase();
  }

  trackFriend(_: number, friend: Friend): string {
    return friend.authId;
  }
}
