import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { UserProfileStateService } from '../../../../core/services/user-profile-state.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { UpdateUserProfileRequest } from '../../../../core/models/user-profile.model';

interface EditProfileForm {
  displayName: FormControl<string>;
  bio: FormControl<string>;
  phone: FormControl<string>;
}

@Component({
  selector: 'app-edit-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-profile.component.html',
})
export class EditProfileComponent implements OnInit {
  readonly profileForm: FormGroup<EditProfileForm>;
  isLoading = false;
  isUploadingAvatar = false;
  avatarPreview: string | null = null;

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly userProfileService: UserProfileService,
    private readonly userProfileState: UserProfileStateService,
    private readonly alertService: AlertService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.profileForm = this.formBuilder.group({
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
      this.avatarPreview = profile.avatarUrl;
    }
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.alertService.danger('Only JPEG, PNG or WebP images are allowed.', 'Invalid file type');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.alertService.danger('File size must not exceed 5MB.', 'File too large');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.avatarPreview = reader.result as string;
    };
    reader.readAsDataURL(file);

    this.isUploadingAvatar = true;
    this.cdr.detectChanges();
    this.userProfileService.updateAvatar(file).subscribe({
      next: () => {
        this.isUploadingAvatar = false;
        this.cdr.detectChanges();
        this.alertService.success('Avatar updated successfully!', 'Avatar saved');
      },
      error: () => {
        this.isUploadingAvatar = false;
        this.cdr.detectChanges();
        this.avatarPreview = this.userProfileState.profile()?.avatarUrl ?? null;
      },
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.userProfileService.updateMyProfile(this.buildUpdateRequest()).subscribe({
      next: () => {
        this.alertService.success('Your profile has been updated!', 'Profile saved');
        this.router.navigate(['/home']);
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/home']);
  }

  private buildUpdateRequest(): UpdateUserProfileRequest {
    const { bio, displayName, phone } = this.profileForm.getRawValue();
    return { bio, displayName, phone };
  }
}
