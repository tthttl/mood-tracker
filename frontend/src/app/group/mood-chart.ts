import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RoundResultResponse } from '../api';

const WIDTH = 600;
const HEIGHT = 240;
const PLOT = { left: 44, right: 16, top: 16, bottom: 28 };

/** The group's mood over time: per closed round the min-max range and the average. */
@Component({
  selector: 'app-mood-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (rounds().length === 0) {
      <p>No round has been completed yet.</p>
    } @else {
      <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" role="img" aria-label="Group mood per round">
        @for (tick of ticks(); track tick.value) {
          <line [attr.x1]="plot.left" [attr.x2]="width - plot.right" [attr.y1]="tick.y" [attr.y2]="tick.y" class="grid" />
          <text [attr.x]="plot.left - 8" [attr.y]="tick.y" class="axis" text-anchor="end" dominant-baseline="middle">
            {{ tick.value > 0 ? '+' + tick.value : tick.value }}
          </text>
        }
        <polyline [attr.points]="line()" class="average" />
        @for (point of points(); track point.round) {
          <line [attr.x1]="point.x" [attr.x2]="point.x" [attr.y1]="point.maxY" [attr.y2]="point.minY" class="range" />
          <circle [attr.cx]="point.x" [attr.cy]="point.averageY" r="4" class="point">
            <title>Round {{ point.round }}: average {{ point.average.toFixed(2) }} (min {{ point.min }}, max {{ point.max }})</title>
          </circle>
          <text [attr.x]="point.x" [attr.y]="height - 8" class="axis" text-anchor="middle">{{ point.round }}</text>
        }
      </svg>
    }
  `,
  styles: `
    svg {
      display: block;
      width: 100%;
      max-width: 720px;
      height: auto;
      margin: 0 auto;
    }
    .grid {
      stroke: var(--mat-sys-outline-variant);
    }
    .axis {
      fill: var(--mat-sys-on-surface-variant);
      font-size: 12px;
    }
    .range {
      stroke: var(--mat-sys-primary);
      stroke-width: 6;
      stroke-linecap: round;
      opacity: 0.25;
    }
    .average {
      fill: none;
      stroke: var(--mat-sys-primary);
      stroke-width: 2;
    }
    .point {
      fill: var(--mat-sys-primary);
    }
  `,
})
export class MoodChart {
  readonly rounds = input.required<RoundResultResponse[]>();
  readonly moodRange = input.required<number>();

  protected readonly width = WIDTH;
  protected readonly height = HEIGHT;
  protected readonly plot = PLOT;

  protected readonly ticks = computed(() => {
    const n = this.moodRange();
    return [n, 0, -n].map((value) => ({ value, y: this.y(value) }));
  });

  protected readonly points = computed(() => {
    const rounds = this.rounds();
    return rounds.map((r, index) => ({
      round: index + 1,
      x: this.x(index, rounds.length),
      averageY: this.y(r.average),
      minY: this.y(r.min),
      maxY: this.y(r.max),
      average: r.average,
      min: r.min,
      max: r.max,
    }));
  });

  protected readonly line = computed(() => this.points().map((p) => `${p.x},${p.averageY}`).join(' '));

  private x(index: number, count: number): number {
    const plotWidth = WIDTH - PLOT.left - PLOT.right;
    return count === 1 ? PLOT.left + plotWidth / 2 : PLOT.left + (index / (count - 1)) * plotWidth;
  }

  private y(value: number): number {
    const n = this.moodRange();
    const plotHeight = HEIGHT - PLOT.top - PLOT.bottom;
    return PLOT.top + ((n - value) / (2 * n)) * plotHeight;
  }
}
