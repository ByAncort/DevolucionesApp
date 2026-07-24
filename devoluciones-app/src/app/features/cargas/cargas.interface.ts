export type CargaEstado = 'PROCESANDO' | 'COMPLETADA' | 'CON_ERRORES';

export interface CargaResponse {
  id: number;
  nombreArchivo: string;
  totalFilas: number;
  filasOk: number;
  filasRechazadas: number;
  estado: CargaEstado;
  fechaCreacion: string;
}

export interface CargaError {
  numFila: number;
  campo: string;
  motivo: string;
}

export interface CargaDetalle {
  carga: CargaResponse;
  errores: CargaError[];
}

export const CARGA_ESTADO_COLORS: Record<CargaEstado, { bg: string; text: string }> = {
  PROCESANDO: { bg: 'bg-blue-100', text: 'text-blue-700' },
  COMPLETADA: { bg: 'bg-green-100', text: 'text-green-700' },
  CON_ERRORES: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
};

export const CARGA_ESTADO_LABELS: Record<CargaEstado, string> = {
  PROCESANDO: 'Procesando',
  COMPLETADA: 'Completada',
  CON_ERRORES: 'Completada con errores',
};
