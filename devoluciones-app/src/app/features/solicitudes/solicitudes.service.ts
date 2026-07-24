import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PageResponse,
  SolicitudResponse,
  SolicitudFilters,
} from './solicitudes.interface';

@Injectable({ providedIn: 'root' })
export class SolicitudesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/solicitudes`;

  getAll(filters: SolicitudFilters = {}): Observable<PageResponse<SolicitudResponse>> {
    let params = new HttpParams()
      .set('page', (filters.page ?? 0).toString())
      .set('size', (filters.size ?? 10).toString());

    if (filters.estado) params = params.set('estado', filters.estado);
    if (filters.rut) params = params.set('rut', filters.rut);
    if (filters.origen) params = params.set('origen', filters.origen);
    if (filters.fechaInicio) params = params.set('fechaInicio', filters.fechaInicio);
    if (filters.fechaFin) params = params.set('fechaFin', filters.fechaFin);

    return this.http.get<PageResponse<SolicitudResponse>>(this.baseUrl, { params });
  }
}
