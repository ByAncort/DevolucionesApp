import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/auth/services/auth.service';

@Component({
  selector: 'app-solicitudes',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50">
      <header class="bg-white shadow-sm border-b">
        <div class="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <h1 class="text-lg font-semibold text-gray-900">Solicitudes</h1>
          <div class="flex items-center gap-4">
            <span class="text-sm text-gray-500">{{ authService.getUserEmail() }}</span>
            <button
              (click)="authService.logout()"
              class="text-sm text-red-600 hover:text-red-800 font-medium"
            >
              Cerrar sesion
            </button>
          </div>
        </div>
      </header>
      <main class="max-w-7xl mx-auto px-4 py-8">
        <p class="text-gray-600">Bandeja de solicitudes - proximamente.</p>
      </main>
    </div>
  `,
})
export class SolicitudesComponent {
  constructor(public authService: AuthService) {}
}
