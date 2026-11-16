import axios from 'axios';

export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
export const BACKEND_ORIGIN = API_BASE.replace(/\/api\/?$/, '');

export const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  // The frontend (5173) and backend (8080) are cross-origin for axios, so it needs this to send the CSRF header.
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

let csrfPromise = null;

/** Makes sure the XSRF-TOKEN cookie exists before any write. */
export function ensureCsrf(force = false) {
  const has = typeof document !== 'undefined' && document.cookie.includes('XSRF-TOKEN=');
  if (has && !force) return Promise.resolve();
  if (!csrfPromise) {
    csrfPromise = api.get('/auth/csrf').finally(() => {
      csrfPromise = null;
    });
  }
  return csrfPromise;
}

api.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toLowerCase();
  if (method !== 'get' && method !== 'head' && method !== 'options') await ensureCsrf();
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    if (status === 401 && !url.startsWith('/auth/')) {
      window.dispatchEvent(new CustomEvent('edusphere:unauthorized'));
    }
    return Promise.reject(error);
  },
);

/** Turns any Axios error into { kind, status, message, fieldErrors }. */
export function parseError(err) {
  if (!err) return { kind: 'generic', status: 0, message: 'Something went wrong', fieldErrors: {} };
  if (err.code === 'ERR_CANCELED') return { kind: 'canceled', status: 0, message: 'Cancelled', fieldErrors: {} };
  if (!err.response) {
    return {
      kind: 'network',
      status: 0,
      message: 'Cannot reach the server. Check your connection and that the backend is running.',
      fieldErrors: {},
    };
  }
  const { status, data } = err.response;
  const message = (data && typeof data === 'object' && data.message) || 'Something went wrong. Please try again.';
  const fieldErrors = (data && typeof data === 'object' && data.fieldErrors) || {};
  let kind = 'generic';
  if (status === 401) kind = 'unauthorized';
  else if (status === 403) kind = 'forbidden';
  else if (status === 404) kind = 'notfound';
  else if (status === 409) kind = 'conflict';
  else if (status === 429) kind = 'ratelimit';
  else if (status === 400 || status === 422) kind = 'validation';
  else if (status >= 500) kind = 'server';
  return { kind, status, message, fieldErrors };
}

/** Maps backend field errors onto React Hook Form fields; returns true when something was mapped. */
export function applyFieldErrors(err, setError) {
  const { fieldErrors } = parseError(err);
  const entries = Object.entries(fieldErrors || {});
  entries.forEach(([field, message]) => setError(field, { type: 'server', message }));
  return entries.length > 0;
}

export function resourceUrl(id, download = false) {
  return `${API_BASE}/resources/${id}${download ? '?download=true' : ''}`;
}

export function coverUrl(subjectId) {
  return `${API_BASE}/subjects/${subjectId}/cover`;
}
