import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { Workspace, WorkspaceMember, WorkspaceRole } from '../../../../core/models/workspace.model';

@Component({
  selector: 'app-workspace-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './workspace-detail.component.html',
})
export class WorkspaceDetailComponent implements OnInit {
  workspace: Workspace | null = null;
  members: WorkspaceMember[] = [];
  isLoading = true;
  isUpdating = false;
  isAddingMember = false;
  showEditForm = false;
  showAddMember = false;

  editForm: FormGroup;
  addMemberForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private workspaceService: WorkspaceService,
    private alertService: AlertService,
    public userProfileState: UserProfileStateService,
  ) {
    this.editForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
    });

    this.addMemberForm = this.fb.group({
      authId: ['', [Validators.required]],
    });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadWorkspace(id);
  }

  private loadWorkspace(id: string) {
    this.workspaceService.getById(id).subscribe({
      next: (workspace) => {
        this.workspace = workspace;
        this.editForm.patchValue({ name: workspace.name, description: workspace.description ?? '' });
        this.loadMembers(id);
      },
      error: () => {
        this.isLoading = false;
        this.router.navigate(['/workspaces']);
      },
    });
  }

  private loadMembers(workspaceId: string) {
    this.workspaceService.listMembers(workspaceId).subscribe({
      next: (members) => {
        this.members = members;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
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
        this.alertService.success('Workspace updated successfully!', 'Saved');
      },
      error: () => {
        this.isUpdating = false;
      },
    });
  }

  onDelete() {
    if (!this.workspace) return;
    if (!confirm(`Delete "${this.workspace.name}"? This action cannot be undone.`)) return;

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

    this.workspaceService.addMember(this.workspace.id, this.addMemberForm.value).subscribe({
      next: (member) => {
        this.members = [...this.members, member];
        this.addMemberForm.reset();
        this.showAddMember = false;
        this.isAddingMember = false;
        this.alertService.success('Member added to the workspace.', 'Member added');
      },
      error: () => {
        this.isAddingMember = false;
      },
    });
  }

  onRemoveMember(member: WorkspaceMember) {
    if (!this.workspace) return;
    if (!confirm('Remove this member from the workspace?')) return;

    this.workspaceService.removeMember(this.workspace.id, member.authId).subscribe({
      next: () => {
        this.members = this.members.filter((m) => m.id !== member.id);
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
      OWNER: 'bg-purple-100 text-purple-700',
      ADMIN: 'bg-blue-100 text-blue-700',
      MEMBER: 'bg-gray-100 text-gray-600',
    };
    return classes[role];
  }
}
