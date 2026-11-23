/**
 * The feature "location" (FEATURES.md, "Location"): where the people of a room
 * were last seen. The form of its entries, distances, and the address a
 * device opens in its own map app. Nothing here touches the page or the
 * stone; Rune shows no map, as it loads nothing from elsewhere.
 */
export interface PositionBody { v: 1; lat: number; lon: number; acc?: number; at: string; note?: string }

/** What this device reports to a room: nothing, the latest position only, a track too, or positions which aren't kept. */
export type Reporting = 'off' | 'latest' | 'track' | 'passing';

/** A position as an entry has it, from what the device measured; coordinates to six decimals, about 0.1 m. */
export function positionBody(lat: number, lon: number, accuracy: number | null | undefined, at: Date, note?: string): PositionBody {
  const body: PositionBody = { v: 1, lat: Number(lat.toFixed(6)), lon: Number(lon.toFixed(6)), at: at.toISOString() };
  if (typeof accuracy === 'number' && Number.isFinite(accuracy)) {
    body.acc = Math.round(accuracy);
  }
  if (note) {
    body.note = note;
  }
  return body;
}

/** True if the object is a position which can be shown. */
export function isPosition(value: unknown): value is PositionBody {
  const body = value as PositionBody | null;
  return !!body && typeof body.lat === 'number' && typeof body.lon === 'number'
    && Math.abs(body.lat) <= 90 && Math.abs(body.lon) <= 180 && typeof body.at === 'string' && !Number.isNaN(Date.parse(body.at));
}

/** The distance between two places in metres, along the surface of the earth (haversine). */
export function distance(a: { lat: number; lon: number }, b: { lat: number; lon: number }): number {
  const radians = (degrees: number) => (degrees * Math.PI) / 180;
  const dLat = radians(b.lat - a.lat);
  const dLon = radians(b.lon - a.lon);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(radians(a.lat)) * Math.cos(radians(b.lat)) * Math.sin(dLon / 2) ** 2;
  return 2 * 6371008.8 * Math.asin(Math.min(1, Math.sqrt(h)));
}

/** The address of a place for the device's own map app (RFC 5870). */
export function geoLink(position: { lat: number; lon: number; acc?: number }): string {
  return `geo:${position.lat},${position.lon}${position.acc ? `;u=${position.acc}` : ''}`;
}

/** A place as numbers: "52.52000° N, 13.40500° E". */
export function coordinates(position: { lat: number; lon: number }): string {
  const part = (value: number, positive: string, negative: string) => `${Math.abs(value).toFixed(5)}° ${value >= 0 ? positive : negative}`;
  return `${part(position.lat, 'N', 'S')}, ${part(position.lon, 'E', 'W')}`;
}
