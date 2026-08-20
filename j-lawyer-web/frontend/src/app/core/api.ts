/**
 * Base paths of the j-lawyer REST API. Absolute-from-root (not affected by the SPA's
 * <base href>), same-origin with the served app, so cookies flow and there is no CORS
 * (design.md Decision 2c / Decision 5).
 */
export const API_ROOT = '/j-lawyer-io/rest';

/** Public browser-authentication endpoints (login / refresh / logout). */
export const AUTH_BASE = `${API_ROOT}/v8/auth`;
