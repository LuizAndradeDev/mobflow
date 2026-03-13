import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { SignupRequest } from '../../../../core/models/auth.model';
import { ExceptionService } from '../../../../core/services/exception.service';
import { ErrorResponseDTO } from '../../../../core/models/exception.model';
import { ExceptionComponent } from '../../../../shared/components/alert/exception.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ExceptionComponent,
  ],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  registerForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private exceptionService: ExceptionService,
  ) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      const payload = this.registerForm.value as SignupRequest;

      this.authService.register(payload).subscribe({
        next: (response) => {
          // Opcional: mostrar sucesso
          this.exceptionService.showAlert('Cadastro realizado!', 'success', 'Sucesso');
        },
        error: (err) => {
          // 2. Extrair o DTO que veio do seu Spring Boot
          const errorBody: ErrorResponseDTO = err.error;

          if (errorBody && errorBody.errorType) {
            // 3. Chamar a lógica que traduz o enum (EMAIL_ALREADY_EXIST, etc)
            this.exceptionService.handleHttpError(errorBody.errorType);
          } else {
            this.exceptionService.showAlert('Erro inesperado.', 'danger', 'Erro');
          }
        },
      });
    }
  }
}
