import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ExceptionComponent } from './shared/components/alert/exception.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ExceptionComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('web-app');
}
