import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { AlertInterface } from '../model/alert.interface';
import { AlertType } from '../enum/alert-type.enum';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private alertSubject = new Subject<AlertInterface | null>();
  alert$ = this.alertSubject.asObservable();

  show(alert: AlertInterface) {
    this.alertSubject.next(null);
    this.alertSubject.next(alert);
  }

  info(message: string, title: string = 'Alert!') {
    this.show({
      title,
      message,
      alertType: AlertType.info,
    });
  }

  success(message: string, title: string = 'Success!') {
    this.show({
      title,
      message,
      alertType: AlertType.success,
    });
  }

  warning(message: string, title: string = 'Attention!') {
    this.show({
      title,
      message,
      alertType: AlertType.warning,
    });
  }

  danger(message: string, title: string = 'Error!') {
    this.show({
      title,
      message,
      alertType: AlertType.error,
    });
  }

  clear() {
    this.alertSubject.next(null);
  }
}
