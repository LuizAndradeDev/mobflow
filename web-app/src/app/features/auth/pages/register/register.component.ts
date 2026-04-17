import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { SignupRequest } from '../../../../core/models/auth.model';
import { HttpErrorResponse } from '@angular/common/http';
import { NavbarPublicComponent } from '../../../../shared/components/navbar-public/navbar-public.component';

interface RegisterForm {
  username: FormControl<string>;
  email: FormControl<string>;
  password: FormControl<string>;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NavbarPublicComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  readonly registerForm: FormGroup<RegisterForm>;
  fieldErrors: { username?: string; email?: string } = {};
  registrationCompleted = false;
  confirmationEmail = '';

  constructor(
    private readonly formBuilder: NonNullableFormBuilder,
    private readonly authService: AuthService,
    private readonly alertService: AlertService,
    private readonly router: Router,
  ) {
    this.registerForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.fieldErrors = {};
    this.registrationCompleted = false;
    const payload = this.buildSignupRequest();
    this.authService.register(payload).subscribe({
      next: () => {
        this.confirmationEmail = payload.email;
        this.registrationCompleted = true;
        this.alertService.success('Account created. Check your inbox to confirm your email.', 'Confirm your email');
        this.registerForm.reset({ username: '', email: '', password: '' });
      },
      error: (err: HttpErrorResponse) => {
        const errorType = err.error?.errorType;
        if (errorType === 'USERNAME_ALREADY_EXIST') {
          this.fieldErrors.username = 'This username is already taken.';
          this.registerForm.get('username')?.reset();
        } else if (errorType === 'EMAIL_ALREADY_EXIST') {
          this.fieldErrors.email = 'This email is already registered.';
          this.registerForm.get('email')?.reset();
        }
      },
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  private buildSignupRequest(): SignupRequest {
    const { email, password, username } = this.registerForm.getRawValue();
    return { email, password, username };
  }
}
