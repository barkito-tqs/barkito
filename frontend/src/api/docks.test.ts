import { ApiError, getState, loadDocks, registerDock, type DockResponse } from './store';

const FISH_MARKET: DockResponse = {
  id: 'd0c0a000-0000-4000-8000-0000000000aa',
  name: 'Fish Market',
  district: null,
  latitude: 40.641817,
  longitude: -8.66465,
  berthCount: 4,
  berthsInService: 3,
};

function reply(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('docks from the REST API (US-04)', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('loads the docks and places them on the map', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply(200, [FISH_MARKET])));

    await loadDocks();

    expect(getState().docks).toEqual([
      {
        id: FISH_MARKET.id,
        name: 'Fish Market',
        district: '—',
        latitude: 40.641817,
        longitude: -8.66465,
        x: 190,
        y: 250,
        berths: 4,
        berthsInService: 3,
      },
    ]);
    expect(getState().docksError).toBeNull();
  });

  it('keeps the docks it has and records the error when the API cannot be reached', async () => {
    const before = getState().docks;
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    await loadDocks();

    expect(getState().docks).toBe(before);
    expect(getState().docksError).toBe('The server cannot be reached.');
  });

  it('registers a dock and adds it to the list', async () => {
    const fetchMock = vi.fn().mockResolvedValue(reply(201, { ...FISH_MARKET, id: 'new' }));
    vi.stubGlobal('fetch', fetchMock);

    const dock = await registerDock({
      name: 'Fish Market',
      district: ' ',
      latitude: 40.641817,
      longitude: -8.66465,
      berths: 4,
    });

    expect(dock.id).toBe('new');
    expect(getState().docks.at(-1)?.id).toBe('new');
    expect(JSON.parse(fetchMock.mock.calls[0][1].body as string)).toEqual({
      name: 'Fish Market',
      district: null,
      latitude: 40.641817,
      longitude: -8.66465,
      berthCount: 4,
    });
  });

  it('passes on a refusal from the API', async () => {
    const detail = 'A dock has from 1 to 10 berths; got 11.';
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply(422, { detail })));
    const count = getState().docks.length;

    const error = await registerDock({
      name: 'Too Big',
      district: '',
      latitude: 40.64,
      longitude: -8.65,
      berths: 11,
    }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 422, message: detail });
    expect(getState().docks).toHaveLength(count);
  });
});
