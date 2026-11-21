import { ApiError, activeTripOf, getState, reserve, unlock } from './store';

/** US-12 acceptance criteria against the mock API, at 10:00 so the operating hours (BR6) allow it. */
describe('unlock a boat with its QR code (US-12)', () => {
  beforeAll(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 20, 10, 0));
  });

  afterAll(() => {
    vi.useRealTimers();
  });

  function expectRefused(action: () => unknown, status: number) {
    try {
      action();
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError);
      expect((e as ApiError).status).toBe(status);
      return;
    }
    throw new Error('expected the request to be refused');
  }

  const codeOf = (boatId: string) => getState().boats.find((b) => b.id === boatId)!.unlockCode;

  it('refuses an unknown code and sends no unlock', () => {
    expectRefused(() => unlock('u-francisca', '000000', 1), 404);
    expect(activeTripOf('u-francisca')).toBeUndefined();
  });

  it('refuses a boat reserved by someone else and keeps it RESERVED', () => {
    getState().users.push({ ...getState().users[0], id: 'u-other', name: 'Other User' });
    reserve('u-other', 'B-04');
    expectRefused(() => unlock('u-francisca', codeOf('B-04'), 1), 409);
    expect(getState().boats.find((b) => b.id === 'B-04')!.state).toBe('RESERVED');
  });

  it('lets the user who reserved the boat unlock it by scanning, and adds the hold fee', () => {
    vi.setSystemTime(new Date(2026, 10, 20, 10, 6));
    const trip = unlock('u-other', codeOf('B-04'), 4);
    expect(trip.holdSeconds).toBe(6 * 60);
    expect(getState().boats.find((b) => b.id === 'B-04')!.state).toBe('IN_USE');
  });

  it('refuses more passengers than the capacity: Standard 6 refused, 5 accepted (BR4)', () => {
    expectRefused(() => unlock('u-francisca', codeOf('B-05'), 6), 409);
    expect(unlock('u-francisca', codeOf('B-05'), 5).passengers).toBe(5);
  });

  it('refuses a second boat while one is active (BR3)', () => {
    expectRefused(() => unlock('u-francisca', codeOf('B-10'), 1), 409);
  });
});
