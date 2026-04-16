import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { CreateWorkspaceRequest, Workspace } from '../../../../core/models/workspace.model';

interface JoinWorkspaceForm {
  code: FormControl<string>;
}

interface CreateWorkspaceForm {
  name: FormControl<string>;
  description: FormControl<string>;
}

@Component({
  selector: 'app-workspace-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-list.component.html',
})
export class WorkspaceListComponent implements OnInit {
  workspaces: Workspace[] = [];
  isLoading = true;
  readonly showModal = signal(false);
  readonly modalTab = signal<'join' | 'create'>('join');

  readonly joinForm: FormGroup<JoinWorkspaceForm>;
  readonly createForm: FormGroup<CreateWorkspaceForm>;
  isJoining = false;
  isCreating = false;

  constructor(
    public router: Router,
    private readonly workspaceService: WorkspaceService,
    private readonly alertService: AlertService,
    private readonly userProfileState: UserProfileStateService,
    private readonly formBuilder: NonNullableFormBuilder,
  ) {
    this.joinForm = this.formBuilder.group({
      code: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(8)]],
    });
    this.createForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
    });
  }

  ngOnInit(): void {
    this.workspaceService.listMine().subscribe({
      next: (data) => {
        this.workspaces = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  get currentAuthId() {
    return this.userProfileState.profile()?.authId;
  }

  isOwner(workspace: Workspace): boolean {
    return workspace.ownerAuthId === this.currentAuthId;
  }

  onJoin(): void {
    if (this.joinForm.invalid || this.isJoining) {
      return;
    }

    this.isJoining = true;
    this.workspaceService.joinByCode(this.joinForm.getRawValue().code).subscribe({
      next: () => {
        this.alertService.success('You joined the workspace!', 'Joined');
        this.showModal.set(false);
        this.joinForm.reset({ code: '' });
        this.loadWorkspaces();
        this.isJoining = false;
      },
      error: () => {
        this.isJoining = false;
      },
    });
  }

  onCreate(): void {
    if (this.createForm.invalid || this.isCreating) {
      return;
    }

    this.isCreating = true;
    this.workspaceService.create(this.buildCreateRequest()).subscribe({
      next: (workspace) => {
        this.alertService.success(`"${workspace.name}" created!`, 'Workspace created');
        this.showModal.set(false);
        this.createForm.reset({ description: '', name: '' });
        this.router.navigate(['/workspaces', workspace.id]);
      },
      error: () => {
        this.isCreating = false;
      },
    });
  }

  private loadWorkspaces(): void {
    this.workspaceService.invalidateListCache();
    this.workspaceService.listMine().subscribe({
      next: (data) => {
        this.workspaces = data;
      },
    });
  }

  private buildCreateRequest(): CreateWorkspaceRequest {
    const { description, name } = this.createForm.getRawValue();
    return { description, name };
  }
}
