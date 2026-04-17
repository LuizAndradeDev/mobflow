import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ConfirmEmailComponent } from './confirm-email.component';
import { AuthService } from '../../../../core/services/auth.service';
import { ThemeService } from '../../../../core/services/theme.service';

describe('ConfirmEmailComponent', () => {
  it('confirms the account when the token is present', () => {
    const confirmEmail = vi.fn().mockReturnValue(of('Email confirmed successfully'));

    TestBed.configureTestingModule({
      imports: [ConfirmEmailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ token: 'abc-123' }),
            },
          },
        },
        {
          provide: AuthService,
          useValue: { confirmEmail },
        },
        {
          provide: Router,
          useValue: { url: '/confirm-email' },
        },
        {
          provide: ThemeService,
          useValue: { isDark: () => false, toggle: vi.fn() },
        },
      ],
    });

    const fixture = TestBed.createComponent(ConfirmEmailComponent);
    fixture.detectChanges();

    expect(confirmEmail).toHaveBeenCalledWith('abc-123');
    expect(fixture.componentInstance.state).toBe('success');
  });

  it('shows an error when the token is missing', () => {
    const confirmEmail = vi.fn();

    TestBed.configureTestingModule({
      imports: [ConfirmEmailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({}),
            },
          },
        },
        {
          provide: AuthService,
          useValue: { confirmEmail },
        },
        {
          provide: Router,
          useValue: { url: '/confirm-email' },
        },
        {
          provide: ThemeService,
          useValue: { isDark: () => false, toggle: vi.fn() },
        },
      ],
    });

    const fixture = TestBed.createComponent(ConfirmEmailComponent);
    fixture.detectChanges();

    expect(confirmEmail).not.toHaveBeenCalled();
    expect(fixture.componentInstance.state).toBe('error');
  });

  it('shows an error when the backend rejects the token', () => {
    const confirmEmail = vi.fn().mockReturnValue(throwError(() => new Error('expired')));

    TestBed.configureTestingModule({
      imports: [ConfirmEmailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ token: 'expired-token' }),
            },
          },
        },
        {
          provide: AuthService,
          useValue: { confirmEmail },
        },
        {
          provide: Router,
          useValue: { url: '/confirm-email' },
        },
        {
          provide: ThemeService,
          useValue: { isDark: () => false, toggle: vi.fn() },
        },
      ],
    });

    const fixture = TestBed.createComponent(ConfirmEmailComponent);
    fixture.detectChanges();

    expect(confirmEmail).toHaveBeenCalledWith('expired-token');
    expect(fixture.componentInstance.state).toBe('error');
  });
});
