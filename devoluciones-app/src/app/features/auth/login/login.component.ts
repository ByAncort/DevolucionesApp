import { Component, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/auth/services/auth.service';
import { AuthErrorResponse } from '../../../core/auth/interfaces/auth.interface';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    public authService: AuthService,
    private router: Router,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });

    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/solicitudes']);
    }
  }

  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.router.navigate(['/solicitudes']);
      },
      error: (err: HttpErrorResponse) => {
        const body = err.error as AuthErrorResponse;
        const msg =
          typeof body === 'object' && body?.message
            ? body.message
            : 'Error al iniciar sesion. Intente de nuevo.';
        this.errorMessage.set(msg);
      },
    });
  }

  getErrorText(field: string): string {
    const ctrl = this.loginForm.get(field);
    if (!ctrl?.errors || !ctrl.touched) return '';
    if (ctrl.errors['required']) return `${field === 'email' ? 'El email' : 'La contrasena'} es requerido.`;
    if (ctrl.errors['email']) return 'Ingrese un email valido.';
    if (ctrl.errors['minlength']) return 'Minimo 6 caracteres.';
    return '';
  }
}
