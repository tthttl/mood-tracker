import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { GroupResponse, GroupsService, RoundResultResponse, RoundStatusResponse, RoundsService } from '../api';
import { apiErrorMessage, isNotFound } from '../core/api-errors';

/** Read-side state of one group page: the group, its open round and the round history. */
@Injectable()
export class GroupStore {
  private readonly groups = inject(GroupsService);
  private readonly rounds = inject(RoundsService);

  readonly group = signal<GroupResponse | null>(null);
  readonly currentRound = signal<RoundStatusResponse | null>(null);
  readonly history = signal<RoundResultResponse[]>([]);
  readonly notFound = signal(false);
  readonly refreshError = signal<string | null>(null);

  private groupId = '';

  async load(groupId: string): Promise<void> {
    this.groupId = groupId;
    this.group.set(null);
    this.currentRound.set(null);
    this.history.set([]);
    this.notFound.set(false);
    await this.refresh();
  }

  async refresh(): Promise<void> {
    if (!this.groupId) {
      return;
    }
    const groupId = this.groupId;
    try {
      const [group, currentRound, history] = await Promise.all([
        firstValueFrom(this.groups.getGroup({ groupId })),
        this.loadCurrentRound(groupId),
        firstValueFrom(this.rounds.getRoundHistory({ groupId })),
      ]);
      if (groupId !== this.groupId) {
        return;
      }
      this.group.set(group);
      this.currentRound.set(currentRound);
      this.history.set(history.rounds);
      this.refreshError.set(null);
    } catch (error) {
      if (groupId !== this.groupId) {
        return;
      }
      if (isNotFound(error)) {
        this.notFound.set(true);
      } else {
        this.refreshError.set(apiErrorMessage(error));
      }
    }
  }

  // "No open round" is answered with 404 by the API; for the UI that is a normal state.
  private async loadCurrentRound(groupId: string): Promise<RoundStatusResponse | null> {
    try {
      return await firstValueFrom(this.rounds.getCurrentRound({ groupId }));
    } catch (error) {
      if (isNotFound(error)) {
        return null;
      }
      throw error;
    }
  }
}
