import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'solicitudes/:id',
    loadComponent: () =>
      import('./features/solicitudes/solicitud-detalle/solicitud-detalle.component').then(m => m.SolicitudDetalleComponent),
    canActivate: [authGuard],
  },
  {
    path: 'solicitudes',
    loadComponent: () =>
      import('./features/solicitudes/solicitudes.component').then(m => m.SolicitudesComponent),
    canActivate: [authGuard],
  },
  {
    path: '',
    redirectTo: 'solicitudes',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'solicitudes',
  },
];
