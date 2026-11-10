
import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('./auth', () => ({
  AuthApiError: class AuthApiError extends Error {
    readonly status: number

    constructor(status: number) {
      super('Authentication request failed')
      this.status = status
    }
  },
  ensureValidAccessToken: vi.fn(),
  getCsrfToken: vi.fn(),
  refreshAfterUnauthorized: vi.fn(),
}))

import {
  ensureValidAccessToken,
  getCsrfToken,
  refreshAfterUnauthorized,
} from './auth'
import { apiFetch } from './client'

const ensureTokenMock = vi.mocked(ensureValidAccessToken)
const csrfTokenMock = vi.mocked(getCsrfToken)
const refreshMock = vi.mocked(refreshAfterUnauthorized)

describe('apiFetch', () => {
  beforeEach(() => {
    vi.resetAllMocks()

    ensureTokenMock.mockResolvedValue('access-token')
    csrfTokenMock.mockResolvedValue('csrf-token')
  })

  it('adds Bearer token to GET requests', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(null, { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/example')

    const [endpoint, options] = fetchMock.mock.calls[0]
    const headers = new Headers(options.headers)

    expect(endpoint).toBe('/api/example')
    expect(headers.get('Authorization')).toBe(
      'Bearer access-token',
    )
    expect(headers.has('X-XSRF-TOKEN')).toBe(false)
    expect(options.credentials).toBe('include')
    expect(csrfTokenMock).not.toHaveBeenCalled()
  })

  it('adds CSRF token to POST requests', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(null, { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/example', {
      method: 'POST',
      body: JSON.stringify({ value: 1 }),
    })

    const [, options] = fetchMock.mock.calls[0]
    const headers = new Headers(options.headers)

    expect(headers.get('Authorization')).toBe(
      'Bearer access-token',
    )
    expect(headers.get('X-XSRF-TOKEN')).toBe(
      'csrf-token',
    )
  })

  it('refreshes token and retries after 401', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(
        new Response(null, { status: 401 }),
      )
      .mockResolvedValueOnce(
        new Response(null, { status: 200 }),
      )

    vi.stubGlobal('fetch', fetchMock)
    refreshMock.mockResolvedValue('new-access-token')

    const response = await apiFetch('/api/example')

    expect(response.status).toBe(200)
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(refreshMock).toHaveBeenCalledWith(
      'access-token',
    )

    const [, retryOptions] = fetchMock.mock.calls[1]
    const headers = new Headers(retryOptions.headers)

    expect(headers.get('Authorization')).toBe(
      'Bearer new-access-token',
    )
  })

  it('does not retry indefinitely after another 401', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(null, { status: 401 }),
    )

    vi.stubGlobal('fetch', fetchMock)
    refreshMock.mockResolvedValue('new-access-token')

    const response = await apiFetch('/api/example')

    expect(response.status).toBe(401)
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(refreshMock).toHaveBeenCalledTimes(1)
  })

  it('does not send a request without an access token', async () => {
    ensureTokenMock.mockResolvedValue(null)

    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      apiFetch('/api/example'),
    ).rejects.toMatchObject({
      status: 401,
    })

    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('rejects endpoints outside /api/', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      apiFetch('https://example.com'),
    ).rejects.toThrow(
      'API endpoint must start with /api/',
    )

    expect(fetchMock).not.toHaveBeenCalled()
  })
})