import { AlertType } from '../enum/alert-type.enum';

export interface AlertInterface {
  title: string;
  message: string;
  alertType: AlertType;
}
