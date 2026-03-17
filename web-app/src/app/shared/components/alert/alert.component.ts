import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService } from './service/alert.service';
import { AlertInterface } from './model/alert.interface';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.css',
})
export class AlertComponent implements OnInit {
  alert: AlertInterface | null = null;
  showAlert = false;
  isLeaving = false;
  private autoCloseTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private alertService: AlertService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.alertService.alert$.subscribe((data) => {
      if (this.autoCloseTimeout) {
        clearTimeout(this.autoCloseTimeout);
        this.autoCloseTimeout = null;
      }

      if (data) {
        this.alert = data;
        this.showAlert = true;
        this.isLeaving = false;

        this.autoCloseTimeout = setTimeout(() => {
          this.close();
        }, 6000);
      } else {
        this.alert = null;
        this.showAlert = false;
        this.isLeaving = false;
      }

      this.cdr.detectChanges();
    });
  }

  close() {
    this.isLeaving = true;
    this.cdr.detectChanges();

    setTimeout(() => {
      this.alertService.clear();
    }, 300);
  }
}
