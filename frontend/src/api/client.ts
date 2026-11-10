
import {
  AuthApiError,
  ensureValidAccessToken,
  getCsrfToken,
  refreshAfterUnauthorized,
} from './auth'

const CSRF_METHODS = new Set([
  'POST',
  'PUT',
  'PATCH',
  'DELETE',
])

async function withAuthorization(
  options: RequestInit,
  token: string,
): Promise<RequestInit> {
  const headers = new Headers(options.headers)
  const method = (options.method ?? 'GET').toUpperCase()

  headers.set('Authorization', `Bearer ${token}`)

  if (CSRF_METHODS.has(method)) {
    const csrfToken = await getCsrfToken()
    headers.set('X-XSRF-TOKEN', csrfToken)
  }

  return {
    ...options,
    headers,
    credentials: 'include',
  }
}

export async function apiFetch(
  endpoint: string,
  options: RequestInit = {},
): Promise<Response> {
  if (!endpoint.startsWith('/api/')) {
    throw new Error('API endpoint must start with /api/')
  }

  const token = await ensureValidAccessToken()

  if (token === null) {
    throw new AuthApiError(401)
  }

  const response = await fetch(
    endpoint,
    await withAuthorization(options, token),
  )

  if (response.status !== 401) {
    return response
  }

  const refreshedToken =
    await refreshAfterUnauthorized(token)

  if (refreshedToken === null) {
    throw new AuthApiError(401)
  }

  return fetch(
    endpoint,
    await withAuthorization(options, refreshedToken),
  )
}