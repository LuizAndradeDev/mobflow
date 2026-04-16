import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { LoginRequest } from '../../../../core/models/auth.model';
import { HttpErrorResponse } from '@angular/common/http';

interface LoginForm {
  email: FormControl<string>;
  password: FormControl<string>;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  readonly loginForm: FormGroup<LoginForm>;
  fieldError: string | null = null;

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly authService: AuthService,
    private readonly userProfileService: UserProfileService,
    private readonly router: Router,
  ) {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.fieldError = null;
    this.authService.login(this.buildLoginRequest()).subscribe({
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

  private buildLoginRequest(): LoginRequest {
    const { email, password } = this.loginForm.getRawValue();
    return { email, password };
  }
}
