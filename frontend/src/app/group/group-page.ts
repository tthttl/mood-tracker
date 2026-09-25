import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { timer } from 'rxjs';
import { AddMemberForm } from './add-member-form';
import { GroupStore } from './group-store';
import { MoodChart } from './mood-chart';
import { StartRoundForm } from './start-round-form';
import { SubmitMoodForm } from './submit-mood-form';

const REFRESH_INTERVAL_MS = 5000;

@Component({
  selector: 'app-group-page',
  host: { class: 'stack' },
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [GroupStore],
  imports: [
    DatePipe,
    RouterLink,
    MatCardModule,
    MatChipsModule,
    MatExpansionModule,
    MatProgressBarModule,
    MatTableModule,
    AddMemberForm,
    MoodChart,
    StartRoundForm,
    SubmitMoodForm,
  ],
  template: `
    @if (store.notFound()) {
      <mat-card>
        <mat-card-header>
          <mat-card-title>Group not found</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>There is no group with the id "{{ groupId() }}".</p>
          <a routerLink="/">Back to start</a>
        </mat-card-content>
      </mat-card>
    } @else if (store.group(); as group) {
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ group.name }}</mat-card-title>
          <mat-card-subtitle>Mood range -{{ group.moodRange }} to +{{ group.moodRange }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content class="stack">
          <p>Share this page's address with your group so everyone can take part.</p>
          <mat-chip-set aria-label="Members">
            @for (email of group.memberEmails; track email) {
              <mat-chip>{{ email }}</mat-chip>
            }
          </mat-chip-set>
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>Add a member</mat-panel-title>
            </mat-expansion-panel-header>
            <app-add-member-form [groupId]="group.id" (added)="store.refresh()" />
          </mat-expansion-panel>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>Current round</mat-card-title>
        </mat-card-header>
        <mat-card-content class="stack">
          @if (store.currentRound(); as round) {
            <p>Started by {{ round.startedBy }} on {{ round.startedAt | date: 'medium' }}.</p>
            <div>
              <h3>Submitted</h3>
              @if (round.submittedMemberEmails.length === 0) {
                <span>Nobody yet</span>
              } @else {
                <mat-chip-set aria-label="Members who have submitted">
                  @for (email of round.submittedMemberEmails; track email) {
                    <mat-chip>{{ email }}</mat-chip>
                  }
                </mat-chip-set>
              }
            </div>
            <div>
              <h3>Waiting for</h3>
              <mat-chip-set aria-label="Members who have not submitted yet">
                @for (email of round.pendingMemberEmails; track email) {
                  <mat-chip>{{ email }}</mat-chip>
                }
              </mat-chip-set>
            </div>
            <h3>Submit your mood</h3>
            <app-submit-mood-form [groupId]="group.id" [moodRange]="group.moodRange" (submitted)="store.refresh()" />
          } @else {
            <p>No round is open. Anyone in the group can start one.</p>
            <app-start-round-form [groupId]="group.id" (started)="store.refresh()" />
          }
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>History</mat-card-title>
        </mat-card-header>
        <mat-card-content class="stack">
          <app-mood-chart [rounds]="store.history()" [moodRange]="group.moodRange" />
          @if (store.history().length > 0) {
            <table mat-table [dataSource]="rows()">
              <ng-container matColumnDef="round">
                <th mat-header-cell *matHeaderCellDef>Round</th>
                <td mat-cell *matCellDef="let row">{{ row.number }}</td>
              </ng-container>
              <ng-container matColumnDef="closedAt">
                <th mat-header-cell *matHeaderCellDef>Closed</th>
                <td mat-cell *matCellDef="let row">{{ row.closedAt | date: 'medium' }}</td>
              </ng-container>
              <ng-container matColumnDef="min">
                <th mat-header-cell *matHeaderCellDef>Lowest</th>
                <td mat-cell *matCellDef="let row">{{ row.min }}</td>
              </ng-container>
              <ng-container matColumnDef="max">
                <th mat-header-cell *matHeaderCellDef>Highest</th>
                <td mat-cell *matCellDef="let row">{{ row.max }}</td>
              </ng-container>
              <ng-container matColumnDef="average">
                <th mat-header-cell *matHeaderCellDef>Average</th>
                <td mat-cell *matCellDef="let row">{{ row.average.toFixed(2) }}</td>
              </ng-container>
              <ng-container matColumnDef="submissionCount">
                <th mat-header-cell *matHeaderCellDef>Submissions</th>
                <td mat-cell *matCellDef="let row">{{ row.submissionCount }}</td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="columns"></tr>
              <tr mat-row *matRowDef="let row; columns: columns"></tr>
            </table>
          }
        </mat-card-content>
      </mat-card>
      @if (store.refreshError(); as error) {
        <p role="alert">Could not refresh: {{ error }}</p>
      }
    } @else {
      <mat-progress-bar mode="indeterminate" />
      @if (store.refreshError(); as error) {
        <p role="alert">{{ error }}</p>
      }
    }
  `,
})
export class GroupPage {
  protected readonly store = inject(GroupStore);

  readonly groupId = input.required<string>();

  protected readonly columns = ['round', 'closedAt', 'min', 'max', 'average', 'submissionCount'];

  protected readonly rows = computed(() =>
    this.store
      .history()
      .map((round, index) => ({ ...round, number: index + 1 }))
      .reverse(),
  );

  constructor() {
    effect(() => {
      const groupId = this.groupId();
      untracked(() => this.store.load(groupId));
    });
    timer(REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.store.refresh());
  }
}
