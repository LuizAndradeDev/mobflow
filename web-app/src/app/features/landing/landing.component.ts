import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavbarPublicComponent } from '../../shared/components/navbar-public/navbar-public.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, NavbarPublicComponent],
  templateUrl: './landing.component.html',
})
export class LandingComponent {}
