import { HttpErrorResponse } from '@angular/common/http';
import { ErrorResponse } from '../api';

export function apiErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Something went wrong.';
  }
  if (error.status === 0) {
    return 'Cannot reach the server. Is the backend running?';
  }
  const body = error.error as Partial<ErrorResponse> | null;
  if (body?.message) {
    const details = body.fieldErrors?.map((e) => `${e.field}: ${e.message}`).join('; ');
    return details ? `${body.message} (${details})` : body.message;
  }
  return `The request failed (${error.status}).`;
}

export function isNotFound(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 404;
}
