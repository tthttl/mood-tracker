import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatSliderModule } from '@angular/material/slider';
import { firstValueFrom } from 'rxjs';
import { RoundStatusResponseStatusEnum, RoundsService } from '../api';
import { Notifier } from '../core/notifier';
import { IdentityFields, createIdentityForm } from '../shared/identity-fields';

@Component({
  selector: 'app-submit-mood-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatSliderModule, IdentityFields],
  template: `
    <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
      <app-identity-fields [form]="form.controls.identity" [groupId]="groupId()" />
      <div class="stack">
        <label id="mood-label">Your mood: {{ form.controls.value.value }} (from -{{ moodRange() }} to +{{ moodRange() }})</label>
        <mat-slider [min]="-moodRange()" [max]="moodRange()" step="1" discrete showTickMarks>
          <input matSliderThumb formControlName="value" aria-labelledby="mood-label" />
        </mat-slider>
      </div>
      <div>
        <button mat-flat-button type="submit" [disabled]="busy()">Submit mood</button>
      </div>
    </form>
  `,
})
export class SubmitMoodForm {
  private readonly rounds = inject(RoundsService);
  private readonly notifier = inject(Notifier);

  readonly groupId = input.required<string>();
  readonly moodRange = input.required<number>();
  readonly submitted = output<void>();

  protected readonly busy = signal(false);
  protected readonly form = new FormGroup({
    identity: createIdentityForm(),
    value: new FormControl(0, { nonNullable: true }),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { identity, value } = this.form.getRawValue();
    this.busy.set(true);
    try {
      const round = await firstValueFrom(
        this.rounds.submitMood({
          groupId: this.groupId(),
          submitMoodRequest: { email: identity.email, code: identity.code, value },
        }),
      );
      if (round.status === RoundStatusResponseStatusEnum.Closed && round.result) {
        const { min, max, average } = round.result;
        this.notifier.info(`Round complete: min ${min}, max ${max}, average ${average.toFixed(2)}.`);
      } else {
        this.notifier.info('Mood submitted. The result appears once everyone has submitted.');
      }
      this.form.reset();
      this.submitted.emit();
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}
