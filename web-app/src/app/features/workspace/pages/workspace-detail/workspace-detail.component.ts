import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, switchMap } from 'rxjs';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { Workspace, WorkspaceMember, WorkspaceRole } from '../../../../core/models/workspace.model';

@Component({
  selector: 'app-workspace-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-detail.component.html',
})
export class WorkspaceDetailComponent implements OnInit {
  workspace: Workspace | null = null;
  members: WorkspaceMember[] = [];

  isLoading = true;
  membersLoading = true;

  isUpdating = false;
  isAddingMember = false;
  showEditForm = false;
  showAddMember = false;
  codeCopied = false;

  editForm: FormGroup;
  addMemberForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private fb: FormBuilder,
    private workspaceService: WorkspaceService,
    private alertService: AlertService,
    public userProfileState: UserProfileStateService,
    private cdr: ChangeDetectorRef,
  ) {
    this.editForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
    });
    this.addMemberForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
    });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;

    forkJoin({
      workspace: this.workspaceService.getById(id),
      members: this.workspaceService.listMembers(id),
    }).subscribe({
      next: ({ workspace, members }) => {
        this.workspace = workspace;
        this.members = members;
        this.isLoading = false;
        this.membersLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isLoading = false;
        this.membersLoading = false;
        this.cdr.markForCheck();
        this.router.navigate(['/workspaces']);
      },
    });
  }

  copyCode() {
    if (!this.workspace?.publicCode) return;
    navigator.clipboard.writeText(this.workspace.publicCode);
    this.codeCopied = true;
    setTimeout(() => (this.codeCopied = false), 2000);
  }

  get currentUserRole(): WorkspaceRole | null {
    const profile = this.userProfileState.profile();
    if (!profile) return null;
    const member = this.members.find((m) => m.authId === profile.authId);
    return member?.role ?? null;
  }

  get isOwnerOrAdmin(): boolean {
    return this.currentUserRole === 'OWNER' || this.currentUserRole === 'ADMIN';
  }

  get isOwner(): boolean {
    return this.currentUserRole === 'OWNER';
  }

  onUpdate() {
    if (this.editForm.invalid || this.isUpdating || !this.workspace) return;
    this.isUpdating = true;
    this.workspaceService.update(this.workspace.id, this.editForm.value).subscribe({
      next: (updated) => {
        this.workspace = updated;
        this.showEditForm = false;
        this.isUpdating = false;
        this.alertService.success('Workspace updated!', 'Saved');
      },
      error: () => {
        this.isUpdating = false;
      },
    });
  }

  onDelete() {
    if (!this.workspace) return;
    if (!confirm(`Delete "${this.workspace.name}"? This cannot be undone.`)) return;
    this.workspaceService.delete(this.workspace.id).subscribe({
      next: () => {
        this.alertService.success('Workspace deleted.', 'Deleted');
        this.router.navigate(['/workspaces']);
      },
    });
  }

  onAddMember() {
    if (this.addMemberForm.invalid || this.isAddingMember || !this.workspace) return;
    this.isAddingMember = true;

    this.workspaceService
      .addMember(this.workspace.id, this.addMemberForm.value)
      .pipe(
        switchMap(() => {
          this.workspaceService.invalidateMemberCache(this.workspace!.id);
          this.membersLoading = true;
          return this.workspaceService.listMembers(this.workspace!.id);
        }),
      )
      .subscribe({
        next: (members) => {
          this.members = members;
          this.membersLoading = false;
          this.isAddingMember = false;
          this.addMemberForm.reset();
          this.showAddMember = false;
          this.alertService.success('Member added to the workspace.', 'Member added');
          this.cdr.markForCheck();
        },
        error: () => {
          this.membersLoading = false;
          this.isAddingMember = false;
        },
      });
  }
  onRemoveMember(member: WorkspaceMember) {
    if (!this.workspace) return;
    if (!confirm(`Remove ${member.displayName} from this workspace?`)) return;
    this.workspaceService.removeMember(this.workspace.id, member.authId).subscribe({
      next: () => {
        this.members = this.members.filter((m) => m.id !== member.id);
        this.cdr.markForCheck();
        this.alertService.success('Member removed.', 'Removed');
      },
    });
  }

  roleLabel(role: WorkspaceRole): string {
    const labels: Record<WorkspaceRole, string> = {
      OWNER: 'Owner',
      ADMIN: 'Admin',
      MEMBER: 'Member',
    };
    return labels[role];
  }

  roleBadgeClass(role: WorkspaceRole): string {
    const classes: Record<WorkspaceRole, string> = {
      OWNER: 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400',
      ADMIN: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400',
      MEMBER: 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300',
    };
    return classes[role];
  }
}
