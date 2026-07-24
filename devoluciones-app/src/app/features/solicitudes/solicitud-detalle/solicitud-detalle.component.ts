import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/services/auth.service';
import { SolicitudesService } from '../solicitudes.service';
import {
  SolicitudResponse,
  EventoHistorial,
  AccionEstado,
  Estado,
  ESTADO_COLORS,
  ESTADO_LABELS,
  ACCIONES_LABELS,
  ACCIONES_COLORS,
  ACCIONES_POR_ESTADO,
  ACCIONES_POR_ROL,
} from '../solicitudes.interface';

@Component({
  selector: 'app-solicitud-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './solicitud-detalle.component.html',
  styleUrl: './solicitud-detalle.component.css',
})
export class SolicitudDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly solicitudesService = inject(SolicitudesService);
  readonly authService = inject(AuthService);

  solicitud = signal<SolicitudResponse | null>(null);
  historial = signal<EventoHistorial[]>([]);
  loading = signal(true);
  accionesLoading = signal(false);
  error = signal<string | null>(null);

  mostrarRechazoModal = signal(false);
  motivoRechazo = signal('');
  accionPendiente = signal<AccionEstado | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/solicitudes']);
      return;
    }
    this.cargar(id);
  }

  cargar(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.solicitudesService.getById(id).subscribe({
      next: (sol) => {
        this.solicitud.set(sol);
        this.cargarHistorial(id);
      },
      error: () => {
        this.error.set('No se pudo cargar la solicitud.');
        this.loading.set(false);
      },
    });
  }

  cargarHistorial(id: number): void {
    this.solicitudesService.getHistorial(id).subscribe({
      next: (evts) => {
        this.historial.set(evts);
        this.loading.set(false);
      },
      error: () => {
        this.historial.set([]);
        this.loading.set(false);
      },
    });
  }

  accionesDisponibles(): AccionEstado[] {
    const sol = this.solicitud();
    if (!sol) return [];
    const porEstado = ACCIONES_POR_ESTADO[sol.estado] ?? [];
    const roles = this.authService.getUserRoles();
    const porRol = roles.flatMap((r) => ACCIONES_POR_ROL[r] ?? []);
    return porEstado.filter((a) => porRol.includes(a));
  }

  ejecutarAccion(accion: AccionEstado): void {
    if (accion === 'rechazar') {
      this.accionPendiente.set(accion);
      this.mostrarRechazoModal.set(true);
      return;
    }
    this.confirmarAccion(accion);
  }

  confirmarAccion(accion: AccionEstado): void {
    const sol = this.solicitud();
    if (!sol) return;

    this.accionesLoading.set(true);
    this.mostrarRechazoModal.set(false);

    const motivo = accion === 'rechazar' ? this.motivoRechazo() : undefined;

    this.solicitudesService.accion(sol.id, accion, motivo).subscribe({
      next: (updated) => {
        this.solicitud.set(updated);
        this.cargarHistorial(updated.id);
        this.accionesLoading.set(false);
        this.motivoRechazo.set('');
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Error al ejecutar la accion.';
        this.error.set(msg);
        this.accionesLoading.set(false);
      },
    });
  }

  cancelarRechazo(): void {
    this.mostrarRechazoModal.set(false);
    this.accionPendiente.set(null);
    this.motivoRechazo.set('');
  }

  colorEstado(estado: Estado) {
    return ESTADO_COLORS[estado];
  }

  labelEstado(estado: Estado): string {
    return ESTADO_LABELS[estado];
  }

  labelAccion(accion: AccionEstado): string {
    return ACCIONES_LABELS[accion];
  }

  colorAccion(accion: AccionEstado): string {
    return ACCIONES_COLORS[accion];
  }

  formatoMonto(monto: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(monto);
  }

  formatoFecha(fecha: string): string {
    return new Date(fecha).toLocaleDateString('es-CL', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  trackById(_index: number, item: EventoHistorial): number {
    return item.id;
  }
}
