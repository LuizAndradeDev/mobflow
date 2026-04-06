import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UpdateUserProfileRequest } from '../../../../core/models/user-profile.model';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './complete-profile.component.html',
})
export class CompleteProfileComponent {
  profileForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private userProfileService: UserProfileService,
    private alertService: AlertService,
    private router: Router,
  ) {
    this.profileForm = this.fb.group({
      displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      bio: ['', [Validators.maxLength(500)]],
      phone: ['', [Validators.maxLength(20)]],
    });
  }

  onSubmit() {
    if (this.profileForm.invalid || this.isLoading) return;

    this.isLoading = true;
    const payload = this.profileForm.value as UpdateUserProfileRequest;

    this.userProfileService.updateMyProfile(payload).subscribe({
      next: () => {
        this.alertService.success('Your profile is all set!', 'Profile created');
        this.router.navigate(['/home']);
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }
}
