import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage, isNotFound } from './api-errors';

describe('apiErrorMessage', () => {
  it('uses the message of the backend error body', () => {
    const error = new HttpErrorResponse({
      status: 401,
      error: { status: 401, error: 'INVALID_VERIFICATION_CODE', message: 'Invalid or expired verification code' },
    });

    expect(apiErrorMessage(error)).toBe('Invalid or expired verification code');
  });

  it('appends field errors of a validation failure', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        error: 'VALIDATION_FAILED',
        message: 'Request validation failed',
        fieldErrors: [{ field: 'code', message: 'must match "^[0-9]{6}$"' }],
      },
    });

    expect(apiErrorMessage(error)).toBe('Request validation failed (code: must match "^[0-9]{6}$")');
  });

  it('explains an unreachable server', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Cannot reach the server');
  });

  it('falls back to the status for bodies it does not understand', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 502, error: '<html>' }))).toBe('The request failed (502).');
  });

  it('falls back to a generic text for non-HTTP errors', () => {
    expect(apiErrorMessage(new Error('boom'))).toBe('Something went wrong.');
  });

  it('recognises a 404', () => {
    expect(isNotFound(new HttpErrorResponse({ status: 404 }))).toBe(true);
    expect(isNotFound(new HttpErrorResponse({ status: 500 }))).toBe(false);
  });
});
