import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { GroupsService } from '../api';
import { Notifier } from '../core/notifier';
import { HomePage } from './home-page';

describe('HomePage: open an existing group', () => {
  const router = { navigate: vi.fn(() => Promise.resolve(true)) };

  function render() {
    TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [
        { provide: Router, useValue: router },
        { provide: GroupsService, useValue: {} },
        { provide: Notifier, useValue: { info: vi.fn(), error: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const form = element.querySelector<HTMLFormElement>('form.row')!;
    const input = form.querySelector<HTMLInputElement>('input')!;
    const type = async (value: string) => {
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();
    };
    const submit = async () => {
      form.dispatchEvent(new Event('submit'));
      await fixture.whenStable();
    };
    return { form, type, submit, button: form.querySelector('button')! };
  }

  beforeEach(() => router.navigate.mockClear());

  it('opens the group page for the entered id when the form is submitted', async () => {
    const { type, submit } = render();

    await type('  6787100c-999d-4fb5-9d4c-1612cb3e69c2 ');
    await submit();

    expect(router.navigate).toHaveBeenCalledWith(['/groups', '6787100c-999d-4fb5-9d4c-1612cb3e69c2']);
  });

  it('does not navigate without an id and keeps the button disabled', async () => {
    const { button, type, submit } = render();

    await type('   ');
    await submit();

    expect(button.disabled).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('enables the button once an id is entered', async () => {
    const { button, type } = render();

    await type('abc');

    expect(button.disabled).toBe(false);
  });
});
