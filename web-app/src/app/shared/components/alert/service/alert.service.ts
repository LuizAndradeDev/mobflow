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

  info(message: string, title: string = 'Alerta!') {
    this.show({
      title,
      message,
      alertType: AlertType.info,
    });
  }

  success(message: string, title: string = 'Sucesso!') {
    this.show({
      title,
      message,
      alertType: AlertType.success,
    });
  }

  warning(message: string, title: string = 'Atenção!') {
    this.show({
      title,
      message,
      alertType: AlertType.warning,
    });
  }

  danger(message: string, title: string = 'Erro!') {
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
