import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UpdateUserProfileRequest } from '../../../../core/models/user-profile.model';

@Component({
  selector: 'app-edit-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-profile.component.html',
})
export class EditProfileComponent implements OnInit {
  profileForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private userProfileService: UserProfileService,
    private userProfileState: UserProfileStateService,
    private alertService: AlertService,
    private router: Router,
  ) {
    this.profileForm = this.fb.group({
      displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      bio: ['', [Validators.maxLength(500)]],
      phone: ['', [Validators.maxLength(20)]],
    });
  }

  ngOnInit() {
    const profile = this.userProfileState.profile();
    if (profile) {
      this.profileForm.patchValue({
        displayName: profile.displayName,
        bio: profile.bio ?? '',
        phone: profile.phone ?? '',
      });
    }
  }

  onSubmit() {
    if (this.profileForm.invalid || this.isLoading) return;

    this.isLoading = true;
    const payload = this.profileForm.value as UpdateUserProfileRequest;

    this.userProfileService.updateMyProfile(payload).subscribe({
      next: () => {
        this.alertService.success('Your profile has been updated!', 'Profile saved');
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}
