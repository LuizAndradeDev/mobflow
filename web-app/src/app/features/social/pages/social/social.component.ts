import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { Friend, FriendRequest } from '../../../../core/models/social.model';
import { ErrorHandlerService } from '../../../../core/services/error-handler.service';
import { SocialService } from '../../../../core/services/social.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';

interface SendFriendRequestForm {
  username: FormControl<string>;
}

@Component({
  selector: 'app-social',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './social.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SocialComponent implements OnInit {
  friends: Friend[] = [];
  requests: FriendRequest[] = [];
  readonly friendSearch = new FormControl('', { nonNullable: true });

  isLoading = true;
  isSubmitting = false;
  showAddFriendModal = false;
  showPendingRequestsModal = false;
  addFriendFeedback: { tone: 'success' | 'error'; message: string } | null = null;
  readonly processingRequestIds = new Set<string>();

  readonly sendFriendRequestForm: FormGroup<SendFriendRequestForm>;

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly socialService: SocialService,
    private readonly alertService: AlertService,
    private readonly errorHandler: ErrorHandlerService,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.sendFriendRequestForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9_]+$/)]],
    });
  }

  ngOnInit(): void {
    this.loadSocialData();
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

  get friendsCount(): number {
    return this.friends.length;
  }

  get pendingCount(): number {
    return this.incomingRequests.length;
  }

  loadSocialData(silent = false): void {
    if (!silent) {
      this.isLoading = true;
    }

    forkJoin({
      friends: this.socialService.listFriends(),
      requests: this.socialService.listFriendRequests(),
    }).subscribe({
      next: ({ friends, requests }) => {
        this.friends = friends;
        this.requests = requests;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        if (!silent) {
          this.friends = [];
          this.requests = [];
          this.isLoading = false;
        }
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
        this.cdr.markForCheck();
      },
    });
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

  friendInitial(username: string): string {
    return username.slice(0, 1).toUpperCase();
  }

  hasAvatar(friend: Friend): boolean {
    return !!friend.avatarUrl?.trim();
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
}
