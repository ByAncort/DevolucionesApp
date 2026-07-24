import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/services/auth.service';
import { CargasService } from '../cargas.service';
import {
  CargaDetalle,
  CARGA_ESTADO_COLORS,
  CARGA_ESTADO_LABELS,
} from '../cargas.interface';

@Component({
  selector: 'app-carga-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './carga-detalle.component.html',
  styleUrl: './carga-detalle.component.css',
})
export class CargaDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cargasService = inject(CargasService);
  readonly authService = inject(AuthService);

  detalle = signal<CargaDetalle | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/cargas']);
      return;
    }
    this.cargar(id);
  }

  cargar(id: number): void {
    this.loading.set(true);
    this.cargasService.getById(id).subscribe({
      next: (res) => {
        this.detalle.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el detalle de la carga.');
        this.loading.set(false);
      },
    });
  }

  colorEstado(estado: string) {
    return CARGA_ESTADO_COLORS[estado as keyof typeof CARGA_ESTADO_COLORS] ?? { bg: 'bg-gray-100', text: 'text-gray-700' };
  }

  labelEstado(estado: string): string {
    return CARGA_ESTADO_LABELS[estado as keyof typeof CARGA_ESTADO_LABELS] ?? estado;
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

  porcentajeOk(): number {
    const c = this.detalle()?.carga;
    if (!c || c.totalFilas === 0) return 0;
    return Math.round((c.filasOk / c.totalFilas) * 100);
  }
}
