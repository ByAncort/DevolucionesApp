import { Component, signal, inject, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/services/auth.service';
import { CargasService } from '../cargas.service';

@Component({
  selector: 'app-carga-upload',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './carga-upload.component.html',
  styleUrl: './carga-upload.component.css',
})
export class CargaUploadComponent {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  private readonly cargasService = inject(CargasService);
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  archivo = signal<File | null>(null);
  arrastrando = signal(false);
  subiendo = signal(false);
  error = signal<string | null>(null);

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.arrastrando.set(true);
  }

  onDragLeave(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.arrastrando.set(false);
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.arrastrando.set(false);
    const file = e.dataTransfer?.files?.[0];
    if (file) this.seleccionarArchivo(file);
  }

  onFileSelect(e: Event): void {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.seleccionarArchivo(file);
  }

  seleccionarArchivo(file: File): void {
    this.error.set(null);
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.error.set('Solo se permiten archivos CSV.');
      return;
    }
    this.archivo.set(file);
  }

  abrirSelector(): void {
    this.fileInput.nativeElement.click();
  }

  limpiar(): void {
    this.archivo.set(null);
    this.error.set(null);
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  subir(): void {
    const file = this.archivo();
    if (!file) return;

    this.subiendo.set(true);
    this.error.set(null);

    this.cargasService.upload(file).subscribe({
      next: (res) => {
        this.subiendo.set(false);
        this.router.navigate(['/cargas', res.id]);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Error al subir el archivo.';
        this.error.set(msg);
        this.subiendo.set(false);
      },
    });
  }

  formatoTamano(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
