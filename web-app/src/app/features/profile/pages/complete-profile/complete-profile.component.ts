import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UpdateUserProfileRequest } from '../../../../core/models/user-profile.model';

interface CompleteProfileForm {
  displayName: FormControl<string>;
  bio: FormControl<string>;
  phone: FormControl<string>;
}

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './complete-profile.component.html',
})
export class CompleteProfileComponent {
  readonly profileForm: FormGroup<CompleteProfileForm>;
  isLoading = false;

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly userProfileService: UserProfileService,
    private readonly alertService: AlertService,
    private readonly router: Router,
  ) {
    this.profileForm = this.formBuilder.group({
      displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      bio: ['', [Validators.maxLength(500)]],
      phone: ['', [Validators.maxLength(20)]],
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.userProfileService.updateMyProfile(this.buildUpdateRequest()).subscribe({
      next: () => {
        this.alertService.success('Your profile is all set!', 'Profile created');
        this.router.navigate(['/home']);
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  private buildUpdateRequest(): UpdateUserProfileRequest {
    const { bio, displayName, phone } = this.profileForm.getRawValue();
    return { bio, displayName, phone };
  }
}
