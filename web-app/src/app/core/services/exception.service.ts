import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { ErrorMessages, ErrorTP } from '../models/exception.model';

export interface AlertData {
  message: string;
  type: 'info' | 'danger' | 'success' | 'warning' | 'dark';
  title: string;
}

@Injectable({ providedIn: 'root' })
export class ExceptionService {
  private alertSubject = new Subject<AlertData | null>();
  alert$ = this.alertSubject.asObservable();


  handleHttpError(errorType: ErrorTP) {
    const message = ErrorMessages[errorType] || ErrorMessages[ErrorTP.GENERIC_ERROR];
    this.showAlert(message, 'danger', 'Erro de Cadastro');
  }


  showAlert(message: string, type: AlertData['type'] = 'info', title: string = 'Alerta!') {
    this.alertSubject.next({ message, type, title });


    setTimeout(() => this.clear(), 6000);
  }

  clear() {
    this.alertSubject.next(null);
  }
}
