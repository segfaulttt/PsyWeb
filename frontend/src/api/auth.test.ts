
// @vitest-environment jsdom

import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const authResponse = (token: string) => ({
  accessToken: token,
  tokenType: 'Bearer',
  expiresIn: 900,
})

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function csrfResponse(): Response {
  document.cookie = 'XSRF-TOKEN=test-csrf; path=/'
  return new Response(null, { status: 200 })
}

async function loadAuth() {
  vi.resetModules()
  return import('./auth')
}

describe('auth session management', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('BroadcastChannel', undefined)
    vi.stubGlobal('navigator', {
      locks: undefined,
    })
    document.cookie = 'XSRF-TOKEN=test-csrf; path=/'
  })

  afterEach(() => {
    vi.clearAllTimers()
    vi.useRealTimers()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('authenticates after successful login', async () => {
    const fetchMock = vi.fn()
      .mockImplementationOnce(async () => csrfResponse())
      .mockImplementationOnce(async () =>
        jsonResponse(authResponse('token-1')),
      )

    vi.stubGlobal('fetch', fetchMock)

    const auth = await loadAuth()

    await auth.login('test@example.com', 'password')

    expect(auth.getAuthStatus()).toBe('authenticated')
    expect(auth.getAccessToken()).toBe('token-1')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('reuses one refresh request for concurrent calls', async () => {
    const fetchMock = vi.fn()
      .mockImplementationOnce(async () => csrfResponse())
      .mockImplementationOnce(async () =>
        jsonResponse(authResponse('refreshed-token')),
      )

    vi.stubGlobal('fetch', fetchMock)

    const auth = await loadAuth()

    const [first, second] = await Promise.all([
      auth.refreshSession(),
      auth.refreshSession(),
    ])

    expect(first).toBe(true)
    expect(second).toBe(true)
    expect(auth.getAccessToken()).toBe('refreshed-token')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('does not restore a stale refresh after logout', async () => {
    let resolveRefresh!: (response: Response) => void

    const fetchMock = vi.fn()
      .mockImplementationOnce(async () => csrfResponse())
      .mockImplementationOnce(
        () =>
          new Promise<Response>((resolve) => {
            resolveRefresh = resolve
          }),
      )
      .mockImplementationOnce(async () => csrfResponse())
      .mockImplementationOnce(async () =>
        new Response(null, { status: 204 }),
      )

    vi.stubGlobal('fetch', fetchMock)

    const auth = await loadAuth()

    const pendingRefresh = auth.refreshSession()

    await vi.waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(2)
    })

    await auth.logout()

    resolveRefresh(jsonResponse(authResponse('stale-token')))

    expect(await pendingRefresh).toBe(false)
    expect(auth.getAuthStatus()).toBe('guest')
    expect(auth.getAccessToken()).toBeNull()
  })

  it('clears session when refresh returns 401', async () => {
    const fetchMock = vi.fn()
      .mockImplementationOnce(async () => csrfResponse())
      .mockImplementationOnce(async () =>
        new Response(null, { status: 401 }),
      )

    vi.stubGlobal('fetch', fetchMock)

    const auth = await loadAuth()

    const refreshed = await auth.refreshSession()

    expect(refreshed).toBe(false)
    expect(auth.getAuthStatus()).toBe('guest')
    expect(auth.getAccessToken()).toBeNull()
  })

  it('does not expose an expired access token', async () => {
    const fetchMock = vi.fn()
      .mockImplementationOnce(async () => csrfResponse())
      .mockImplementationOnce(async () =>
        jsonResponse(authResponse('initial-token')),
      )
      .mockImplementationOnce(async () => csrfResponse())
      .mockImplementationOnce(async () =>
        jsonResponse(authResponse('new-token')),
      )

    vi.stubGlobal('fetch', fetchMock)

    const auth = await loadAuth()

    await auth.login('test@example.com', 'password')

    await vi.advanceTimersByTimeAsync(14 * 60 * 1000)

    expect(auth.getAccessToken()).toBe('new-token')
    expect(auth.getAuthStatus()).toBe('authenticated')
  })
})