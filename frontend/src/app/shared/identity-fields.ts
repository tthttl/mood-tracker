import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../api';
import { Notifier } from '../core/notifier';

export function createIdentityForm() {
  return new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    code: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^[0-9]{6}$/)] }),
  });
}

export type IdentityForm = ReturnType<typeof createIdentityForm>;

/** Email + verification code: proves the user owns an email address of the group. */
@Component({
  selector: 'app-identity-fields',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="stack" [formGroup]="form()">
      <div class="row">
        <mat-form-field class="grow">
          <mat-label>Your email</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="email" />
          @if (form().controls.email.invalid) {
            <mat-error>Enter a valid email address</mat-error>
          }
        </mat-form-field>
        <button
          mat-stroked-button
          type="button"
          class="send-code"
          [disabled]="form().controls.email.invalid || sending()"
          (click)="sendCode()"
        >
          Send code
        </button>
      </div>
      <mat-form-field>
        <mat-label>Verification code</mat-label>
        <input matInput formControlName="code" inputmode="numeric" maxlength="6" autocomplete="one-time-code" />
        <mat-hint>6 digits, sent to your email. A code works once.</mat-hint>
        @if (form().controls.code.invalid) {
          <mat-error>Enter the 6-digit code</mat-error>
        }
      </mat-form-field>
    </div>
  `,
  styles: `
    .send-code {
      margin-top: 8px;
    }
  `,
})
export class IdentityFields {
  private readonly auth = inject(AuthService);
  private readonly notifier = inject(Notifier);

  readonly form = input.required<IdentityForm>();
  readonly groupId = input.required<string>();

  protected readonly sending = signal(false);

  protected async sendCode(): Promise<void> {
    this.sending.set(true);
    try {
      await firstValueFrom(
        this.auth.requestVerificationCode({
          groupId: this.groupId(),
          requestVerificationCodeRequest: { email: this.form().controls.email.value },
        }),
      );
      this.notifier.info('If this address belongs to the group, a code is on its way.');
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.sending.set(false);
    }
  }
}
