
import type {
  RegistrationRequest,
  RegistrationResponse,
  RegistrationRole,
} from '../types/registration'

export class RegistrationApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(
    status: number,
    code: string,
    message: string,
  ) {
    super(message)

    this.name = 'RegistrationApiError'
    this.status = status
    this.code = code
  }
}

export async function registerUser(
  role: RegistrationRole,
  data: RegistrationRequest,
): Promise<RegistrationResponse> {
  const endpoint =
    role === 'CLIENT'
      ? '/api/auth/register/client'
      : '/api/auth/register/specialist'

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null)

    if (
      typeof body === 'object' &&
      body !== null &&
      'code' in body &&
      typeof body.code === 'string'
    ) {
      throw new RegistrationApiError(
        response.status,
        body.code,
        'Не удалось выполнить регистрацию',
      )
    }

    throw new RegistrationApiError(
      response.status,
      'UNKNOWN_ERROR',
      'Не удалось выполнить регистрацию',
    )
  }

  return (await response.json()) as RegistrationResponse
}