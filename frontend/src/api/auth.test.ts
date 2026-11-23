import { authorizationHeader, setSessionRole } from './auth';

describe('dev token of the signed-in persona (US-02)', () => {
  afterEach(() => setSessionRole(null));

  it('sends no token without a session', () => {
    expect(authorizationHeader()).toEqual({});
  });

  it("sends the dev token of the persona's role", () => {
    setSessionRole('OPERATOR');

    expect(authorizationHeader()).toEqual({ Authorization: `Bearer ${__DEV_TOKENS__.OPERATOR}` });
    expect(__DEV_TOKENS__.OPERATOR).toBeTruthy();
  });

  it('never sends the token that has every role', () => {
    expect(Object.keys(__DEV_TOKENS__)).not.toContain('ALL');
  });
});
