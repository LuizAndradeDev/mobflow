import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { SignupRequest } from '../../../../core/models/auth.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  registerForm: FormGroup;
  fieldErrors: { username?: string; email?: string } = {};

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private alertService: AlertService,
  ) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onSubmit() {
    if (this.registerForm.invalid) return;
    this.fieldErrors = {};

    const payload = this.registerForm.value as SignupRequest;
    this.authService.register(payload).subscribe({
      next: () => {
        this.alertService.success('Account created! Please sign in.', 'Welcome to Mobflow');
        this.registerForm.reset();
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
}
