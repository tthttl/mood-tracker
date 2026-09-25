import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { GroupsService } from '../api';
import { Notifier } from '../core/notifier';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/;

export function parseEmails(text: string): string[] {
  return text
    .split(/[\s,;]+/)
    .map((email) => email.trim())
    .filter((email) => email.length > 0);
}

function membersValidator(control: AbstractControl<string>): ValidationErrors | null {
  const emails = parseEmails(control.value);
  if (emails.length === 0) {
    return { required: true };
  }
  return emails.every((email) => EMAIL_PATTERN.test(email)) ? null : { email: true };
}

@Component({
  selector: 'app-create-group-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
      <mat-form-field>
        <mat-label>Group name</mat-label>
        <input matInput formControlName="name" maxlength="100" />
        @if (form.controls.name.invalid) {
          <mat-error>Enter a name</mat-error>
        }
      </mat-form-field>
      <mat-form-field>
        <mat-label>Mood range (n)</mat-label>
        <input matInput type="number" formControlName="moodRange" min="1" step="1" />
        <mat-hint>Moods are submitted from -n to +n</mat-hint>
        @if (form.controls.moodRange.invalid) {
          <mat-error>Enter a whole number of at least 1</mat-error>
        }
      </mat-form-field>
      <mat-form-field>
        <mat-label>Members' email addresses</mat-label>
        <textarea matInput formControlName="members" rows="4"></textarea>
        <mat-hint>One address per line</mat-hint>
        @if (form.controls.members.hasError('required')) {
          <mat-error>Add at least one email address</mat-error>
        } @else if (form.controls.members.hasError('email')) {
          <mat-error>Every entry must be a valid email address</mat-error>
        }
      </mat-form-field>
      <div>
        <button mat-flat-button type="submit" [disabled]="busy()">Create group</button>
      </div>
    </form>
  `,
})
export class CreateGroupForm {
  private readonly groups = inject(GroupsService);
  private readonly router = inject(Router);
  private readonly notifier = inject(Notifier);

  protected readonly busy = signal(false);
  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/)] }),
    moodRange: new FormControl(1, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.pattern(/^[0-9]+$/)],
    }),
    members: new FormControl('', { nonNullable: true, validators: [membersValidator] }),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, moodRange, members } = this.form.getRawValue();
    this.busy.set(true);
    try {
      const group = await firstValueFrom(
        this.groups.createGroup({
          createGroupRequest: { name: name.trim(), moodRange, memberEmails: parseEmails(members) },
        }),
      );
      await this.router.navigate(['/groups', group.id]);
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}
