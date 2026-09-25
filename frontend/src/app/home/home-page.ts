import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { CreateGroupForm } from './create-group-form';

@Component({
  selector: 'app-home-page',
  host: { class: 'stack' },
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, CreateGroupForm],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Create a group</mat-card-title>
        <mat-card-subtitle>Measure your team's mood without anyone seeing individual answers</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <app-create-group-form />
      </mat-card-content>
    </mat-card>

    <mat-card>
      <mat-card-header>
        <mat-card-title>Open an existing group</mat-card-title>
        <mat-card-subtitle>Or use the address someone shared with you</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <form class="row" (ngSubmit)="open()">
          <mat-form-field class="grow">
            <mat-label>Group id</mat-label>
            <input matInput [formControl]="groupId" />
          </mat-form-field>
          <button mat-stroked-button type="submit" class="open" [disabled]="groupId.invalid">Open</button>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .open {
      margin-top: 8px;
    }
  `,
})
export class HomePage {
  private readonly router = inject(Router);

  protected readonly groupId = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/)] });

  protected open(): void {
    if (this.groupId.valid) {
      void this.router.navigate(['/groups', this.groupId.value.trim()]);
    }
  }
}
