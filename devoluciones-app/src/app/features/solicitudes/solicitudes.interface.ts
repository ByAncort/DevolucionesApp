export type Estado =
  | 'BORRADOR'
  | 'EN_REVISION'
  | 'APROBADA'
  | 'RECHAZADA'
  | 'PAGADA'
  | 'ANULADA';

export type Origen = 'MANUAL' | 'CARGA_MASIVA';

export interface SolicitudResponse {
  id: number;
  folio: string;
  rutCliente: string;
  nombreCliente: string;
  monto: number;
  bancoDestinoId: number;
  bancoDestinoNombre: string;
  cuentaDestino: string;
  estado: Estado;
  motivoRechazo: string | null;
  creadoPorNombre: string;
  actualizadoPorNombre: string;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface SolicitudFilters {
  estado?: Estado;
  rut?: string;
  origen?: Origen;
  fechaInicio?: string;
  fechaFin?: string;
  page?: number;
  size?: number;
}

export const ESTADO_COLORS: Record<Estado, { bg: string; text: string }> = {
  BORRADOR: { bg: 'bg-gray-100', text: 'text-gray-700' },
  EN_REVISION: { bg: 'bg-blue-100', text: 'text-blue-700' },
  APROBADA: { bg: 'bg-green-100', text: 'text-green-700' },
  RECHAZADA: { bg: 'bg-red-100', text: 'text-red-700' },
  PAGADA: { bg: 'bg-emerald-100', text: 'text-emerald-700' },
  ANULADA: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
};

export const ESTADO_LABELS: Record<Estado, string> = {
  BORRADOR: 'Borrador',
  EN_REVISION: 'En Revision',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  PAGADA: 'Pagada',
  ANULADA: 'Anulada',
};
