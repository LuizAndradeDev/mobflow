import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { Workspace } from '../../../../core/models/workspace.model';

@Component({
  selector: 'app-workspace-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-list.component.html',
})
export class WorkspaceListComponent implements OnInit {
  workspaces: Workspace[] = [];
  isLoading = true;
  showModal = signal(false);
  modalTab = signal<'join' | 'create'>('join');
  activeMenu: string | null = null;

  joinForm: FormGroup;
  createForm: FormGroup;
  isJoining = false;
  isCreating = false;

  constructor(
    public router: Router,
    private workspaceService: WorkspaceService,
    private alertService: AlertService,
    private userProfileState: UserProfileStateService,
    private fb: FormBuilder,
  ) {
    this.joinForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(8)]],
    });
    this.createForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
    });
  }

  ngOnInit() {
    this.workspaceService.listMine().subscribe({
      next: (data) => { this.workspaces = data; this.isLoading = false; },
      error: () => { this.isLoading = false; },
    });
  }

  get currentAuthId() {
    return this.userProfileState.profile()?.authId;
  }

  isOwner(workspace: Workspace): boolean {
    return workspace.ownerAuthId === this.currentAuthId;
  }

  // stopPropagation prevents the card click (navigate) from firing
  // while keeping the dropdown open on the same click
  openMenu(event: MouseEvent, workspaceId: string) {
    event.stopPropagation();
    this.activeMenu = this.activeMenu === workspaceId ? null : workspaceId;
  }

  onJoin() {
    if (this.joinForm.invalid || this.isJoining) return;
    this.isJoining = true;
    this.workspaceService.joinByCode(this.joinForm.value.code).subscribe({
      next: () => {
        this.alertService.success('You joined the workspace!', 'Joined');
        this.showModal.set(false);
        this.joinForm.reset();
        this.reloadWorkspaces();
        this.isJoining = false;
      },
      error: () => { this.isJoining = false; },
    });
  }

  onCreate() {
    if (this.createForm.invalid || this.isCreating) return;
    this.isCreating = true;
    this.workspaceService.create(this.createForm.value).subscribe({
      next: (workspace) => {
        this.alertService.success(`"${workspace.name}" created!`, 'Workspace created');
        this.showModal.set(false);
        this.createForm.reset();
        this.router.navigate(['/workspaces', workspace.id]);
      },
      error: () => { this.isCreating = false; },
    });
  }

  onLeave(workspace: Workspace, event: MouseEvent) {
    event.stopPropagation();
    this.activeMenu = null;
    if (!confirm(`Leave "${workspace.name}"?`)) return;
    this.workspaceService.leave(workspace.id).subscribe({
      next: () => {
        this.workspaces = this.workspaces.filter((w) => w.id !== workspace.id);
        this.alertService.success('You left the workspace.', 'Left');
      },
    });
  }

  onDelete(workspace: Workspace, event: MouseEvent) {
    event.stopPropagation();
    this.activeMenu = null;
    if (!confirm(`Delete "${workspace.name}"? This cannot be undone.`)) return;
    this.workspaceService.delete(workspace.id).subscribe({
      next: () => {
        this.workspaces = this.workspaces.filter((w) => w.id !== workspace.id);
        this.alertService.success('Workspace deleted.', 'Deleted');
      },
    });
  }

  private reloadWorkspaces() {
    this.workspaceService.invalidateListCache();
    this.workspaceService.listMine().subscribe({
      next: (data) => { this.workspaces = data; },
    });
  }
}
