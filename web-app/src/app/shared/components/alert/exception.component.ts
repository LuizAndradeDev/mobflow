// exception.component.ts
import { Component, OnInit } from '@angular/core';
import { ExceptionService, AlertData } from '../../../core/services/exception.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-exception-handler',
  standalone: true,
  templateUrl: './exception.component.html',
  styleUrls: ['./exception.component.css'],
  imports: [CommonModule],
})
export class ExceptionComponent implements OnInit {
  alert: AlertData | null = null;

  constructor(private exceptionService: ExceptionService) {}

  ngOnInit() {
    console.log('ExceptionComponent carregado');
    this.exceptionService.alert$.subscribe((data) => {
      console.log('Alert recebido:', data);
      this.alert = data;
    });
  }

  getStyles() {
    if (!this.alert) return {};

    const stylesMap = {
      info: 'text-fg-brand-strong bg-brand-softer border-brand-subtle',
      danger: 'text-fg-danger-strong bg-danger-soft border-danger-subtle',
      success: 'text-fg-success-strong bg-success-soft border-success-subtle',
      warning: 'text-fg-warning bg-warning-soft border-warning-subtle',
      dark: 'text-heading bg-neutral-secondary-medium border-default-medium',
    };

    return stylesMap[this.alert.type];
  }
  close() {
    this.alert = null;
    this.exceptionService.clear();
  }
}
