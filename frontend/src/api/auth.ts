
export type AuthenticationResponse = {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export type AuthStatus =
  | 'loading'
  | 'authenticated'
  | 'guest'

  export class AuthApiError extends Error {
    readonly status: number

    constructor(status: number) {
      super('Authentication request failed')
      this.name = 'AuthApiError'
      this.status = status
    }
  }
  
type AuthListener = () => void

const REFRESH_BUFFER_MS = 60_000

const REFRESH_RETRY_DELAY_MS = 30_000

const REFRESH_LOCK_NAME = 'psyweb-refresh'

let refreshTimer: ReturnType<typeof setTimeout> | null = null

function cancelRefreshTimer(): void {
  if (refreshTimer !== null) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }
}


function scheduleRefresh(): void {
  cancelRefreshTimer()

  if (accessToken === null) return

  const delay = Math.max(
    0,
    accessTokenExpiresAt - Date.now() - REFRESH_BUFFER_MS,
  )

  refreshTimer = setTimeout(() => {
    void ensureValidAccessToken().catch(() => {
      scheduleRefreshRetry()
    })
  }, delay)
}

function scheduleRefreshRetry(): void {
  cancelRefreshTimer()

  if (
    accessToken === null ||
    authStatus !== 'authenticated'
  ) {
    return
  }

  refreshTimer = setTimeout(() => {
    void ensureValidAccessToken().catch(() => {
      scheduleRefreshRetry()
    })
  }, REFRESH_RETRY_DELAY_MS)
}

const authChannel =
typeof BroadcastChannel !== 'undefined'
  ? new BroadcastChannel('psyweb-auth')
  : null

let accessToken: string | null = null
let accessTokenExpiresAt = 0
let authStatus: AuthStatus = 'loading'
let refreshPromise: Promise<boolean> | null = null
let sessionVersion = 0

const listeners = new Set<AuthListener>()

function notifyListeners(): void {
  for (const listener of listeners) {
    listener()
  }
}

function setAuthStatus(status: AuthStatus): void {
  if (authStatus === status) return

  authStatus = status
  notifyListeners()
}

export function getAuthStatus(): AuthStatus {
  return authStatus
}

export function subscribeToAuth(
  listener: AuthListener,
): () => void {
  listeners.add(listener)

  return () => {
    listeners.delete(listener)
  }
}

export function getAccessToken(): string | null {
  return accessToken
}

export function clearAccessToken(): void {
  cancelRefreshTimer()
  accessToken = null
  accessTokenExpiresAt = 0
}

function applyAuthentication(
  result: AuthenticationResponse,
): void {
  accessToken = result.accessToken
  accessTokenExpiresAt =
    Date.now() + result.expiresIn * 1000

  setAuthStatus('authenticated')
  scheduleRefresh()
}

function clearSession(): void {
  sessionVersion++
  clearAccessToken()
  setAuthStatus('guest')
}

authChannel?.addEventListener('message', (event) => {
  if (event.data === 'logout') {
    clearSession()
  }

  if (event.data === 'login') {
    void refreshSession().catch(() => {
    })
  }
})

export async function getCsrfToken(): Promise<string> {
  const response = await fetch('/api/auth/csrf', {
    credentials: 'include',
    cache: 'no-store',
  })

  if (!response.ok) {
    throw new AuthApiError(response.status)
  }

  const cookie = document.cookie
    .split('; ')
    .find((value) => value.startsWith('XSRF-TOKEN='))

  if (!cookie) {
    throw new AuthApiError(403)
  }

  return decodeURIComponent(
    cookie.substring('XSRF-TOKEN='.length),
  )
}

async function authPost(
  endpoint: string,
  body?: unknown,
): Promise<Response> {
  const csrfToken = await getCsrfToken()

  return fetch(endpoint, {
    method: 'POST',
    credentials: 'include',
    cache: 'no-store',
    headers: {
      'X-XSRF-TOKEN': csrfToken,
      ...(body === undefined
        ? {}
        : { 'Content-Type': 'application/json' }),
    },
    ...(body === undefined
      ? {}
      : { body: JSON.stringify(body) }),
  })
}

export async function login(
  email: string,
  password: string,
): Promise<void> {
  const version = ++sessionVersion

  const response = await authPost('/api/auth/login', {
    email: email.trim(),
    password,
  })

  if (!response.ok) {
    throw new AuthApiError(response.status)
  }

  const result: AuthenticationResponse =
    await response.json()

	if (version !== sessionVersion) return

	applyAuthentication(result)
	authChannel?.postMessage('login')
}


async function performRefresh(
  expectedVersion: number,
): Promise<boolean> {
  if (expectedVersion !== sessionVersion) {
    return false
  }

  try {
    const response = await authPost('/api/auth/refresh')

    if (expectedVersion !== sessionVersion) {
      return false
    }

    if (response.status === 401) {
      clearSession()
      return false
    }

    if (!response.ok) {
      throw new AuthApiError(response.status)
    }

    const result: AuthenticationResponse =
      await response.json()

    if (expectedVersion !== sessionVersion) {
      return false
    }

    applyAuthentication(result)
    return true
  } catch (error) {
    if (expectedVersion !== sessionVersion) {
      return false
    }

    throw error
  }
}


async function performCoordinatedRefresh(
  expectedVersion: number,
): Promise<boolean> {
  if (expectedVersion !== sessionVersion) {
    return false
  }

  if (!navigator.locks) {
    return performRefresh(expectedVersion)
  }

  return navigator.locks.request(
    REFRESH_LOCK_NAME,
    { mode: 'exclusive' },
    async () => {
      if (expectedVersion !== sessionVersion) {
        return false
      }

      return performRefresh(expectedVersion)
    },
  )
}


export function refreshSession(): Promise<boolean> {
  if (refreshPromise !== null) {
    return refreshPromise
  }

  const pending = performCoordinatedRefresh(sessionVersion)
  refreshPromise = pending

  void pending.then(
    () => {
      if (refreshPromise === pending) {
        refreshPromise = null
      }
    },
    () => {
      if (refreshPromise === pending) {
        refreshPromise = null
      }
    },
  )

  return pending
}

export async function restoreSession(): Promise<void> {
  try {
    await refreshSession()
  } catch {
    if (authStatus === 'loading') {
      setAuthStatus('guest')
    }
  }
}

export async function ensureValidAccessToken():
  Promise<string | null> {
  if (
    accessToken !== null &&
    Date.now() <
      accessTokenExpiresAt - REFRESH_BUFFER_MS
  ) {
    return accessToken
  }

  const refreshed = await refreshSession()

  return refreshed ? accessToken : null
}

export async function refreshAfterUnauthorized(
  failedToken: string,
): Promise<string | null> {
  if (
    accessToken !== null &&
    accessToken !== failedToken
  ) {
    return accessToken
  }

  const refreshed = await refreshSession()

  return refreshed ? accessToken : null
}

export async function logout(): Promise<void> {
  clearSession()
  authChannel?.postMessage('logout')

  const response = await authPost('/api/auth/logout')

  if (!response.ok) {
    throw new AuthApiError(response.status)
  }
}

document.addEventListener('visibilitychange', () => {
  if (
    document.visibilityState === 'visible' &&
    authStatus === 'authenticated'
  ) {
    void ensureValidAccessToken().catch(() => {
      scheduleRefreshRetry()
    })
  }
})