import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/services/auth.service';
import { SolicitudesService } from './solicitudes.service';
import {
  SolicitudResponse,
  SolicitudFilters,
  Estado,
  ESTADO_COLORS,
  ESTADO_LABELS,
} from './solicitudes.interface';

@Component({
  selector: 'app-solicitudes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './solicitudes.component.html',
  styleUrl: './solicitudes.component.css',
})
export class SolicitudesComponent implements OnInit {
  private readonly solicitudesService = inject(SolicitudesService);
  readonly authService = inject(AuthService);

  solicitudes = signal<SolicitudResponse[]>([]);
  loading = signal(false);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  pageSize = signal(10);

  filtroEstado = signal<Estado | ''>('');
  filtroRut = signal('');
  filtroFechaInicio = signal('');
  filtroFechaFin = signal('');

  readonly estados: Estado[] = [
    'BORRADOR',
    'EN_REVISION',
    'APROBADA',
    'RECHAZADA',
    'PAGADA',
    'ANULADA',
  ];

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    const filters: SolicitudFilters = {
      page: this.currentPage(),
      size: this.pageSize(),
    };
    const estado = this.filtroEstado();
    if (estado) filters.estado = estado;
    const rut = this.filtroRut().trim();
    if (rut) filters.rut = rut;
    const fi = this.filtroFechaInicio();
    if (fi) filters.fechaInicio = fi;
    const ff = this.filtroFechaFin();
    if (ff) filters.fechaFin = ff;

    this.solicitudesService.getAll(filters).subscribe({
      next: (res) => {
        this.solicitudes.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.currentPage.set(res.number);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  aplicarFiltros(): void {
    this.currentPage.set(0);
    this.cargar();
  }

  limpiarFiltros(): void {
    this.filtroEstado.set('');
    this.filtroRut.set('');
    this.filtroFechaInicio.set('');
    this.filtroFechaFin.set('');
    this.currentPage.set(0);
    this.cargar();
  }

  pagina(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.cargar();
  }

  paginas(): number[] {
    const total = this.totalPages();
    const curr = this.currentPage();
    const pages: number[] = [];
    let start = Math.max(0, curr - 2);
    const end = Math.min(total, start + 5);
    start = Math.max(0, end - 5);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  }

  colorEstado(estado: Estado) {
    return ESTADO_COLORS[estado];
  }

  labelEstado(estado: Estado): string {
    return ESTADO_LABELS[estado];
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
    });
  }

  mathMin(a: number, b: number): number {
    return Math.min(a, b);
  }

  trackById(_index: number, item: SolicitudResponse): number {
    return item.id;
  }
}
