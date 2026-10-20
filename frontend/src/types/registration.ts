export type RegistrationRole = 'CLIENT' | 'SPECIALIST'

export type RegistrationFormData = {
  firstName: string
  lastName: string
  email: string
  password: string
  confirmPassword: string
  personalDataConsent: boolean
}

export type RegistrationRequest = {
  firstName: string
  lastName: string
  email: string
  password: string
}

export type RegistrationResponse = {
  userId: number
  role: RegistrationRole
}