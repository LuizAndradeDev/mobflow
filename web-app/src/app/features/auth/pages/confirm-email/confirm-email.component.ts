import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { NavbarPublicComponent } from '../../../../shared/components/navbar-public/navbar-public.component';

type ConfirmationState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-confirm-email',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarPublicComponent],
  templateUrl: './confirm-email.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmEmailComponent implements OnInit {
  state: ConfirmationState = 'loading';
  title = 'Confirming your email';
  message = 'We are validating your confirmation link and activating your account.';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.setError('Invalid confirmation link', 'This email confirmation link is missing a token. Request a new confirmation email and try again.');
      return;
    }

    this.authService.confirmEmail(token).subscribe({
      next: () => {
        this.state = 'success';
        this.title = 'Email confirmed';
        this.message = 'Your account is now active and ready to sign in.';
      },
      error: () => {
        this.setError('Confirmation failed', 'This confirmation link is invalid or has expired. Request a new email confirmation to activate your account.');
      },
    });
  }

  private setError(title: string, message: string): void {
    this.state = 'error';
    this.title = title;
    this.message = message;
  }
}
