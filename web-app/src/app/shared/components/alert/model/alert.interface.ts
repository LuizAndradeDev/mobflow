import { AlertType } from '../enum/alert-type.enum';

export interface AlertInterface {
  id: string;
  title: string;
  message: string;
  alertType: AlertType;
}
