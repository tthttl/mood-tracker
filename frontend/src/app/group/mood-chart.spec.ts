import { TestBed } from '@angular/core/testing';
import { RoundResultResponse } from '../api';
import { MoodChart } from './mood-chart';

function round(min: number, max: number, average: number): RoundResultResponse {
  return { roundId: `r-${average}`, closedAt: '2026-09-01T10:00:00Z', min, max, average, submissionCount: 3 };
}

describe('MoodChart', () => {
  function render(rounds: RoundResultResponse[]) {
    const fixture = TestBed.createComponent(MoodChart);
    fixture.componentRef.setInput('rounds', rounds);
    fixture.componentRef.setInput('moodRange', 2);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows an empty state when no round has been completed', () => {
    const element = render([]);

    expect(element.textContent).toContain('No round has been completed yet');
    expect(element.querySelector('svg')).toBeNull();
  });

  it('draws one point and one range bar per round', () => {
    const element = render([round(-1, 1, 0), round(0, 2, 1.5), round(-2, 2, 0.5)]);

    expect(element.querySelectorAll('circle').length).toBe(3);
    expect(element.querySelectorAll('line.range').length).toBe(3);
  });

  it('places higher averages higher up in the chart', () => {
    const element = render([round(-2, 2, -2), round(-2, 2, 2)]);
    const [low, high] = Array.from(element.querySelectorAll('circle')).map((c) => Number(c.getAttribute('cy')));

    expect(high).toBeLessThan(low);
  });

  it('labels the axis from -n to +n', () => {
    const labels = Array.from(render([round(-1, 1, 0)]).querySelectorAll('text.axis')).map((t) => t.textContent?.trim());

    expect(labels).toEqual(expect.arrayContaining(['+2', '0', '-2']));
  });
});
