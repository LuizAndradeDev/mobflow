import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { CreateWorkspaceRequest } from '../../../../core/models/workspace.model';

interface CreateWorkspaceForm {
  name: FormControl<string>;
  description: FormControl<string>;
}

@Component({
  selector: 'app-create-workspace',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './create-workspace.component.html',
})
export class CreateWorkspaceComponent {
  readonly workspaceForm: FormGroup<CreateWorkspaceForm>;
  isLoading = false;

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly workspaceService: WorkspaceService,
    private readonly alertService: AlertService,
    private readonly router: Router,
  ) {
    this.workspaceForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
    });
  }

  onSubmit(): void {
    if (this.workspaceForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.workspaceService.create(this.buildCreateRequest()).subscribe({
      next: (workspace) => {
        this.alertService.success(`"${workspace.name}" is ready!`, 'Workspace created');
        this.router.navigate(['/workspaces', workspace.id]);
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  private buildCreateRequest(): CreateWorkspaceRequest {
    const { description, name } = this.workspaceForm.getRawValue();
    return { description, name };
  }
}
