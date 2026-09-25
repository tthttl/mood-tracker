import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { GroupsService } from '../api';
import { Notifier } from '../core/notifier';
import { IdentityFields, createIdentityForm } from '../shared/identity-fields';

@Component({
  selector: 'app-add-member-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, IdentityFields],
  template: `
    <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
      <p>An existing member has to confirm the new member with their own code.</p>
      <app-identity-fields [form]="form.controls.identity" [groupId]="groupId()" />
      <mat-form-field>
        <mat-label>New member's email</mat-label>
        <input matInput type="email" formControlName="newMemberEmail" />
        @if (form.controls.newMemberEmail.invalid) {
          <mat-error>Enter a valid email address</mat-error>
        }
      </mat-form-field>
      <div>
        <button mat-flat-button type="submit" [disabled]="busy()">Add member</button>
      </div>
    </form>
  `,
})
export class AddMemberForm {
  private readonly groups = inject(GroupsService);
  private readonly notifier = inject(Notifier);

  readonly groupId = input.required<string>();
  readonly added = output<void>();

  protected readonly busy = signal(false);
  protected readonly form = new FormGroup({
    identity: createIdentityForm(),
    newMemberEmail: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { identity, newMemberEmail } = this.form.getRawValue();
    this.busy.set(true);
    try {
      await firstValueFrom(
        this.groups.addMember({
          groupId: this.groupId(),
          addMemberRequest: { requesterEmail: identity.email, code: identity.code, newMemberEmail },
        }),
      );
      this.notifier.info(`${newMemberEmail} is now a member of the group.`);
      this.form.reset();
      this.added.emit();
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}
