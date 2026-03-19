import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { CreateWorkspaceRequest } from '../../../../core/models/workspace.model';

@Component({
  selector: 'app-create-workspace',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './create-workspace.component.html',
})
export class CreateWorkspaceComponent {
  workspaceForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private workspaceService: WorkspaceService,
    private alertService: AlertService,
    private router: Router,
  ) {
    this.workspaceForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
    });
  }

  onSubmit() {
    if (this.workspaceForm.invalid || this.isLoading) return;

    this.isLoading = true;
    const payload = this.workspaceForm.value as CreateWorkspaceRequest;

    this.workspaceService.create(payload).subscribe({
      next: (workspace) => {
        this.alertService.success(`"${workspace.name}" is ready!`, 'Workspace created');
        this.router.navigate(['/workspaces', workspace.id]);
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }
}
