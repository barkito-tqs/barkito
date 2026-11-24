/**
 * Places a dock on the canal map from its latitude and longitude (US-04). The map shows a fixed
 * box around the canals of Ria Nova; a dock outside it is drawn on the nearest edge.
 */

export const MAP_WIDTH = 1000;
export const MAP_HEIGHT = 600;

/** The area the canal map shows, in decimal degrees. */
export const MAP_BOUNDS = {
  north: 40.6484,
  south: 40.6326,
  west: -8.6713,
  east: -8.6363,
} as const;

export interface MapPoint {
  x: number;
  y: number;
}

function clamp(value: number, max: number): number {
  return Math.min(Math.max(value, 0), max);
}

export function toMapPoint(latitude: number, longitude: number): MapPoint {
  const { north, south, west, east } = MAP_BOUNDS;
  const x = ((longitude - west) / (east - west)) * MAP_WIDTH;
  const y = ((north - latitude) / (north - south)) * MAP_HEIGHT;
  return { x: Math.round(clamp(x, MAP_WIDTH)), y: Math.round(clamp(y, MAP_HEIGHT)) };
}
