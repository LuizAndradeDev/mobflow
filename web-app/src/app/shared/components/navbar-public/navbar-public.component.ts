import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-navbar-public',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar-public.component.html',
})
export class NavbarPublicComponent {}
