import { Routes } from '@angular/router';
import { HomePage } from './home/home-page';

export const routes: Routes = [
  { path: '', component: HomePage },
  { path: 'groups/:groupId', loadComponent: () => import('./group/group-page').then((m) => m.GroupPage) },
  { path: '**', redirectTo: '' },
];
