import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { RoundsService } from '../api';
import { Notifier } from '../core/notifier';
import { IdentityFields, createIdentityForm } from '../shared/identity-fields';

@Component({
  selector: 'app-start-round-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, IdentityFields],
  template: `
    <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
      <app-identity-fields [form]="form.controls.identity" [groupId]="groupId()" />
      <div>
        <button mat-flat-button type="submit" [disabled]="busy()">Start round</button>
      </div>
    </form>
  `,
})
export class StartRoundForm {
  private readonly rounds = inject(RoundsService);
  private readonly notifier = inject(Notifier);

  readonly groupId = input.required<string>();
  readonly started = output<void>();

  protected readonly busy = signal(false);
  protected readonly form = new FormGroup({ identity: createIdentityForm() });

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, code } = this.form.controls.identity.getRawValue();
    this.busy.set(true);
    try {
      await firstValueFrom(this.rounds.startRound({ groupId: this.groupId(), startRoundRequest: { email, code } }));
      this.notifier.info('Round started. Everyone in the group can now submit a mood.');
      this.form.reset();
      this.started.emit();
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}
