import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../api';
import { Notifier } from '../core/notifier';
import { IdentityFields, createIdentityForm } from './identity-fields';

describe('IdentityFields', () => {
  const auth = { requestVerificationCode: vi.fn(() => of(null)) };
  const notifier = { info: vi.fn(), error: vi.fn() };

  function render() {
    TestBed.configureTestingModule({
      imports: [IdentityFields],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Notifier, useValue: notifier },
      ],
    });
    const fixture = TestBed.createComponent(IdentityFields);
    const form = createIdentityForm();
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('groupId', 'g-1');
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    return { fixture, form, sendButton: element.querySelector<HTMLButtonElement>('button.send-code')! };
  }

  beforeEach(() => {
    auth.requestVerificationCode.mockClear();
    notifier.info.mockClear();
  });

  it('keeps "Send code" disabled until the email is valid', async () => {
    const { fixture, form, sendButton } = render();
    expect(sendButton.disabled).toBe(true);

    form.controls.email.setValue('a@test.com');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(sendButton.disabled).toBe(false);
  });

  it('requests a code for the group and the entered email', async () => {
    const { fixture, form, sendButton } = render();
    form.controls.email.setValue('a@test.com');
    fixture.detectChanges();

    sendButton.click();
    await fixture.whenStable();

    expect(auth.requestVerificationCode).toHaveBeenCalledWith({
      groupId: 'g-1',
      requestVerificationCodeRequest: { email: 'a@test.com' },
    });
    expect(notifier.info).toHaveBeenCalled();
  });

  it('only accepts a 6-digit code', () => {
    const { form } = render();

    form.controls.code.setValue('12345');
    expect(form.controls.code.valid).toBe(false);
    form.controls.code.setValue('123456');
    expect(form.controls.code.valid).toBe(true);
  });
});
