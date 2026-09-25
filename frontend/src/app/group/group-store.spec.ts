import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideApi } from '../api';
import { GroupStore } from './group-store';

describe('GroupStore', () => {
  let store: GroupStore;
  let http: HttpTestingController;

  const group = { id: 'g-1', name: 'Team', moodRange: 1, memberEmails: ['a@test.com'] };
  const round = {
    roundId: 'r-1',
    status: 'OPEN',
    startedBy: 'a@test.com',
    startedAt: '2026-09-01T10:00:00Z',
    submittedMemberEmails: [],
    pendingMemberEmails: ['a@test.com'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GroupStore, provideHttpClient(), provideHttpClientTesting(), provideApi('/api/v1')],
    });
    store = TestBed.inject(GroupStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function loadWith(respond: { current: () => void }) {
    const loading = store.load('g-1');
    http.expectOne('/api/v1/groups/g-1').flush(group);
    respond.current();
    http.expectOne('/api/v1/groups/g-1/rounds').flush({ rounds: [] });
    await loading;
  }

  it('loads the group, the open round and the history', async () => {
    await loadWith({ current: () => http.expectOne('/api/v1/groups/g-1/rounds/current').flush(round) });

    expect(store.group()?.name).toBe('Team');
    expect(store.currentRound()?.roundId).toBe('r-1');
    expect(store.history()).toEqual([]);
    expect(store.notFound()).toBe(false);
  });

  it('treats a 404 for the current round as "no open round", not as an error', async () => {
    await loadWith({
      current: () =>
        http.expectOne('/api/v1/groups/g-1/rounds/current').flush({ message: 'none' }, { status: 404, statusText: 'Not Found' }),
    });

    expect(store.currentRound()).toBeNull();
    expect(store.group()?.id).toBe('g-1');
    expect(store.notFound()).toBe(false);
    expect(store.refreshError()).toBeNull();
  });

  it('marks the group as not found when the group itself is 404', async () => {
    const loading = store.load('missing');
    http.expectOne('/api/v1/groups/missing').flush({ message: 'x' }, { status: 404, statusText: 'Not Found' });
    http.expectOne('/api/v1/groups/missing/rounds/current').flush({ message: 'x' }, { status: 404, statusText: 'Not Found' });
    http.expectOne('/api/v1/groups/missing/rounds').flush({ message: 'x' }, { status: 404, statusText: 'Not Found' });
    await loading;

    expect(store.notFound()).toBe(true);
    expect(store.group()).toBeNull();
  });

  it('keeps the data and reports a refresh error when the server is unreachable', async () => {
    await loadWith({ current: () => http.expectOne('/api/v1/groups/g-1/rounds/current').flush(round) });

    const refreshing = store.refresh();
    http.expectOne('/api/v1/groups/g-1').error(new ProgressEvent('error'), { status: 0 });
    http.expectOne('/api/v1/groups/g-1/rounds/current').error(new ProgressEvent('error'), { status: 0 });
    http.expectOne('/api/v1/groups/g-1/rounds').error(new ProgressEvent('error'), { status: 0 });
    await refreshing;

    expect(store.group()?.name).toBe('Team');
    expect(store.refreshError()).toContain('Cannot reach the server');
  });
});
