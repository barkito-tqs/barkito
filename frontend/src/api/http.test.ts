import { setSessionRole } from './auth';
import { ApiError, http } from './http';

function reply(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('REST client', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('sends JSON under /api/v1 and returns the body', async () => {
    const fetchMock = vi.fn().mockResolvedValue(reply(201, { id: 'd1' }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(http.post('/docks', { name: 'Fish Market' })).resolves.toEqual({ id: 'd1' });
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/docks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{"name":"Fish Market"}',
    });
  });

  it('sends the dev token of the signed-in persona', async () => {
    const fetchMock = vi.fn().mockResolvedValue(reply(200, []));
    vi.stubGlobal('fetch', fetchMock);
    setSessionRole('OPERATOR');

    await http.get('/docks');

    expect(fetchMock.mock.calls[0][1].headers).toEqual({
      Authorization: `Bearer ${__DEV_TOKENS__.OPERATOR}`,
    });
    setSessionRole(null);
  });

  it.each([409, 422])('throws the Problem Details detail of a %i', async (status) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply(status, { detail: 'Refused.' })));

    const error = await http.get('/docks').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status, message: 'Refused.' });
  });

  it('falls back to a generic message when the error has no detail', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 400 })));

    await expect(http.get('/docks')).rejects.toMatchObject({
      status: 400,
      message: 'Check the fields and try again.',
    });
  });

  it('reports an unreachable server as 503', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    await expect(http.get('/docks')).rejects.toMatchObject({ status: 503 });
  });
});
