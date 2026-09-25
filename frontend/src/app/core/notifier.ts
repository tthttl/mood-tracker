import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage } from './api-errors';

@Injectable({ providedIn: 'root' })
export class Notifier {
  private readonly snackBar = inject(MatSnackBar);

  info(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 5000 });
  }

  error(error: unknown): void {
    this.snackBar.open(apiErrorMessage(error), 'OK', { duration: 8000 });
  }
}
