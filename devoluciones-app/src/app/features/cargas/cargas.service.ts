import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CargaResponse, CargaDetalle } from './cargas.interface';

@Injectable({ providedIn: 'root' })
export class CargasService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cargas`;

  upload(file: File): Observable<CargaResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CargaResponse>(this.baseUrl, formData);
  }

  getById(id: number): Observable<CargaDetalle> {
    return this.http.get<CargaDetalle>(`${this.baseUrl}/${id}`);
  }
}
