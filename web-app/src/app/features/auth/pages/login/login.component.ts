import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { LoginRequest } from '../../../../core/models/auth.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  loginForm: FormGroup;
  fieldError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userProfileService: UserProfileService,
    private router: Router,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) return;
    this.fieldError = null;

    const payload = this.loginForm.value as LoginRequest;
    this.authService.login(payload).subscribe({
      next: () => {
        this.userProfileService.getMyProfile().subscribe({
          next: () => this.router.navigate(['/home']),
          error: (err: HttpErrorResponse) => {
            if (err.status === 404) {
              this.router.navigate(['/complete-profile']);
            } else {
              this.router.navigate(['/home']);
            }
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        if (err.error?.errorType === 'INVALID_CREDENTIALS') {
          this.fieldError = 'invalid_credentials';
          this.loginForm.get('password')?.reset();
        }
      },
    });
  }
}
