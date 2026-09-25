import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { GroupsService } from '../api';
import { Notifier } from '../core/notifier';
import { CreateGroupForm, parseEmails } from './create-group-form';

describe('parseEmails', () => {
  it('splits on lines, commas, semicolons and whitespace and drops empty entries', () => {
    expect(parseEmails('a@x.com\nb@x.com, c@x.com;  d@x.com\n\n')).toEqual(['a@x.com', 'b@x.com', 'c@x.com', 'd@x.com']);
  });
});

describe('CreateGroupForm', () => {
  const groups = { createGroup: vi.fn() };
  const router = { navigate: vi.fn(() => Promise.resolve(true)) };

  function render() {
    TestBed.configureTestingModule({
      imports: [CreateGroupForm],
      providers: [
        { provide: GroupsService, useValue: groups },
        { provide: Router, useValue: router },
        { provide: Notifier, useValue: { info: vi.fn(), error: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(CreateGroupForm);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const fill = (selector: string, value: string) => {
      const input = element.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const submit = async () => {
      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      await fixture.whenStable();
    };
    return { fill, submit };
  }

  beforeEach(() => {
    groups.createGroup.mockReset();
    router.navigate.mockClear();
  });

  it('creates the group with the parsed members and opens its page', async () => {
    groups.createGroup.mockReturnValue(of({ id: 'g-1' }));
    const { fill, submit } = render();

    fill('input[formControlName=name]', '  Team Rocket ');
    fill('input[formControlName=moodRange]', '2');
    fill('textarea', 'a@test.com\nb@test.com');
    await submit();

    expect(groups.createGroup).toHaveBeenCalledWith({
      createGroupRequest: { name: 'Team Rocket', moodRange: 2, memberEmails: ['a@test.com', 'b@test.com'] },
    });
    expect(router.navigate).toHaveBeenCalledWith(['/groups', 'g-1']);
  });

  it('does not call the server while the form is invalid', async () => {
    const { fill, submit } = render();

    fill('input[formControlName=name]', 'Team');
    fill('textarea', 'not-an-email');
    await submit();

    expect(groups.createGroup).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('requires at least one member', async () => {
    const { fill, submit } = render();

    fill('input[formControlName=name]', 'Team');
    await submit();

    expect(groups.createGroup).not.toHaveBeenCalled();
  });
});
