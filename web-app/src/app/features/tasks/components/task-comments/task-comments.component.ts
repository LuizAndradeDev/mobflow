import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { SocialComment } from '../../../../core/models/social.model';
import { UserProfile } from '../../../../core/models/user-profile.model';
import { ErrorHandlerService } from '../../../../core/services/error-handler.service';
import { SocialService } from '../../../../core/services/social.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';

interface CommentForm {
  content: FormControl<string>;
}

@Component({
  selector: 'app-task-comments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './task-comments.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCommentsComponent implements OnChanges {
  @Input({ required: true }) taskId!: string;
  @Input() currentAuthId: string | null = null;
  @Input() highlightedCommentId: string | null = null;

  comments: SocialComment[] = [];
  readonly authorProfiles = new Map<string, UserProfile>();
  isLoading = true;
  isLoadingMore = false;
  isSubmitting = false;
  editingCommentId: string | null = null;
  savingCommentId: string | null = null;
  readonly deletingCommentIds = new Set<string>();

  private page = 0;
  totalElements = 0;
  private readonly pageSize = 20;

  readonly createCommentForm: FormGroup<CommentForm>;
  readonly editCommentForm: FormGroup<CommentForm>;

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly socialService: SocialService,
    private readonly userProfileService: UserProfileService,
    private readonly alertService: AlertService,
    private readonly errorHandler: ErrorHandlerService,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.createCommentForm = this.buildCommentForm();
    this.editCommentForm = this.buildCommentForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['taskId']?.currentValue) {
      this.resetState();
      this.loadComments();
      return;
    }

    if (changes['highlightedCommentId']?.currentValue) {
      this.scrollToHighlightedComment();
    }
  }

  get hasMoreComments(): boolean {
    return this.comments.length < this.totalElements;
  }

  submitComment(): void {
    if (this.createCommentForm.invalid || this.isSubmitting) {
      this.createCommentForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const content = this.createCommentForm.getRawValue().content.trim();

    this.socialService.createTaskComment(this.taskId, { content }).pipe(
      finalize(() => {
        this.isSubmitting = false;
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: () => {
        this.createCommentForm.reset({ content: '' });
        this.alertService.success('Comment posted.', 'Comment added');
        this.reloadVisibleComments(this.comments.length + 1);
      },
      error: (error) => {
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  loadMoreComments(): void {
    if (!this.hasMoreComments || this.isLoadingMore) {
      return;
    }

    this.loadComments(true);
  }

  startEdit(comment: SocialComment): void {
    if (!this.canManage(comment)) {
      return;
    }

    this.editingCommentId = comment.id;
    this.editCommentForm.reset({ content: comment.content ?? '' });
    this.cdr.markForCheck();
  }

  cancelEdit(): void {
    this.editingCommentId = null;
    this.savingCommentId = null;
    this.editCommentForm.reset({ content: '' });
    this.cdr.markForCheck();
  }

  saveEdit(comment: SocialComment): void {
    if (this.editCommentForm.invalid || this.savingCommentId === comment.id) {
      this.editCommentForm.markAllAsTouched();
      return;
    }

    this.savingCommentId = comment.id;
    const content = this.editCommentForm.getRawValue().content.trim();

    this.socialService.updateComment(comment.id, { content }).pipe(
      finalize(() => {
        this.savingCommentId = null;
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: (updatedComment) => {
        this.comments = this.comments.map((item) => item.id === comment.id ? updatedComment : item);
        this.editingCommentId = null;
        this.editCommentForm.reset({ content: '' });
        this.alertService.success('Comment updated.', 'Saved');
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  deleteComment(comment: SocialComment): void {
    if (!this.canDelete(comment) || this.deletingCommentIds.has(comment.id)) {
      return;
    }

    if (!confirm('Delete this comment?')) {
      return;
    }

    this.deletingCommentIds.add(comment.id);
    this.socialService.deleteComment(comment.id).pipe(
      finalize(() => {
        this.deletingCommentIds.delete(comment.id);
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: () => {
        this.comments = this.comments.map((item) =>
          item.id === comment.id
            ? { ...item, content: null, mentions: [], deleted: true }
            : item,
        );
        if (this.editingCommentId === comment.id) {
          this.cancelEdit();
        }
        this.alertService.info('Comment deleted.', 'Removed');
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  canManage(comment: SocialComment): boolean {
    return !comment.deleted && !!this.currentAuthId && comment.authorId === this.currentAuthId;
  }

  canDelete(comment: SocialComment): boolean {
    return !comment.deleted && this.canManage(comment);
  }

  isDeleting(commentId: string): boolean {
    return this.deletingCommentIds.has(commentId);
  }

  trackComment(_index: number, comment: SocialComment): string {
    return comment.id;
  }

  commentElementId(commentId: string): string {
    return `task-comment-${commentId}`;
  }

  authorDisplayName(comment: SocialComment): string {
    return this.authorProfiles.get(comment.authorId)?.displayName ?? `@${comment.authorUsername}`;
  }

  avatarUrl(comment: SocialComment): string | null {
    return this.authorProfiles.get(comment.authorId)?.avatarUrl ?? null;
  }

  authorInitial(comment: SocialComment): string {
    return this.authorDisplayName(comment).trim().charAt(0).toUpperCase() || '?';
  }

  isHighlighted(commentId: string): boolean {
    return this.highlightedCommentId === commentId;
  }

  private buildCommentForm(): FormGroup<CommentForm> {
    return this.formBuilder.group({
      content: ['', [Validators.required, Validators.maxLength(4000)]],
    });
  }

  private resetState(): void {
    this.comments = [];
    this.authorProfiles.clear();
    this.page = 0;
    this.totalElements = 0;
    this.isLoading = true;
    this.isLoadingMore = false;
    this.isSubmitting = false;
    this.editingCommentId = null;
    this.savingCommentId = null;
    this.deletingCommentIds.clear();
    this.createCommentForm.reset({ content: '' });
    this.editCommentForm.reset({ content: '' });
    this.cdr.markForCheck();
  }

  private loadComments(append = false): void {
    if (append) {
      this.isLoadingMore = true;
    } else {
      this.isLoading = true;
    }

    const nextPage = append ? this.page + 1 : 0;
    this.socialService.listTaskComments(this.taskId, nextPage, this.pageSize).pipe(
      finalize(() => {
        this.isLoading = false;
        this.isLoadingMore = false;
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: (response) => {
        this.comments = append
          ? [...this.comments, ...response.content]
          : response.content;
        this.page = response.number;
        this.totalElements = response.totalElements;
        this.loadAuthorProfiles(response.content);
        this.scrollToHighlightedComment();
        this.cdr.markForCheck();
      },
      error: (error) => {
        if (!append) {
          this.comments = [];
          this.totalElements = 0;
        }
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  private reloadVisibleComments(size: number): void {
    const targetSize = Math.max(this.pageSize, size);

    this.isLoading = true;
    this.socialService.listTaskComments(this.taskId, 0, targetSize).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: (response) => {
        this.comments = response.content;
        this.page = response.number;
        this.totalElements = response.totalElements;
        this.loadAuthorProfiles(response.content);
        this.scrollToHighlightedComment();
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.alertService.show(this.errorHandler.mapHttpErrorToAlert(error));
      },
    });
  }

  private loadAuthorProfiles(comments: SocialComment[]): void {
    const missingAuthorIds = [...new Set(comments.map((comment) => comment.authorId))]
      .filter((authorId) => !this.authorProfiles.has(authorId));

    for (const authorId of missingAuthorIds) {
      this.userProfileService.getProfileByAuthId(authorId).subscribe({
        next: (profile) => {
          this.authorProfiles.set(authorId, profile);
          this.cdr.markForCheck();
        },
      });
    }
  }

  private scrollToHighlightedComment(): void {
    if (!this.highlightedCommentId) {
      return;
    }

    queueMicrotask(() => {
      const element = document.getElementById(this.commentElementId(this.highlightedCommentId!));
      if (!element) {
        return;
      }

      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }
}
