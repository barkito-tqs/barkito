import { MAP_BOUNDS, MAP_HEIGHT, MAP_WIDTH, toMapPoint } from './geo';

describe('placing a dock on the canal map (US-04)', () => {
  it('puts the north-west corner at the origin and the south-east corner at the far end', () => {
    expect(toMapPoint(MAP_BOUNDS.north, MAP_BOUNDS.west)).toEqual({ x: 0, y: 0 });
    expect(toMapPoint(MAP_BOUNDS.south, MAP_BOUNDS.east)).toEqual({ x: MAP_WIDTH, y: MAP_HEIGHT });
  });

  it('places the Old Market where the map has always shown it', () => {
    expect(toMapPoint(40.641817, -8.66465)).toEqual({ x: 190, y: 250 });
  });

  it('keeps a dock outside the map on its nearest edge', () => {
    expect(toMapPoint(41.0, -9.0)).toEqual({ x: 0, y: 0 });
    expect(toMapPoint(40.0, -8.0)).toEqual({ x: MAP_WIDTH, y: MAP_HEIGHT });
    expect(toMapPoint(40.64, -9.0).x).toBe(0);
  });
});
