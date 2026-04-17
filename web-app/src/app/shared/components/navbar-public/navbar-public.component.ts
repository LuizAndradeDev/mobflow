import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-navbar-public',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar-public.component.html',
})
export class NavbarPublicComponent {
  constructor(
    readonly router: Router,
    readonly themeService: ThemeService,
  ) {}

  get ctaLabel(): string {
    return this.router.url === '/login' ? 'Create account' : 'Login';
  }

  get ctaLink(): string {
    return this.router.url === '/login' ? '/signup' : '/login';
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }
}
