
import { useState, type FormEvent } from 'react'
import type {
  RegistrationFormData,
  RegistrationRole,
  RegistrationRequest,
} from '../types/registration'
import {
  registerUser,
  RegistrationApiError,
} from '../api/registration'

type FormErrors = Partial<
  Record<keyof RegistrationFormData, string>
>

const initialForm: RegistrationFormData = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
  personalDataConsent: false,
}

type RegistrationFormProps = {
  role: RegistrationRole
}

export function RegistrationForm({ role }: RegistrationFormProps) {
  const [form, setForm] = useState<RegistrationFormData>({
    ...initialForm,
  })

  const [errors, setErrors] = useState<FormErrors>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [registrationSuccess, setRegistrationSuccess] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

  function updateField<K extends keyof RegistrationFormData>(
    field: K,
    value: RegistrationFormData[K],
  ) {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }))

    setErrors((previous) => ({
      ...previous,
      [field]: undefined,
    }))

    setServerError(null)
  }

  function validate(): FormErrors {
    const result: FormErrors = {}

    const firstName = form.firstName.trim()
    const lastName = form.lastName.trim()
    const email = form.email.trim()

    if (firstName.length < 2 || firstName.length > 100) {
      result.firstName = 'Имя должно содержать от 2 до 100 символов'
    }

    if (lastName.length < 2 || lastName.length > 100) {
      result.lastName = 'Фамилия должна содержать от 2 до 100 символов'
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      result.email = 'Введите корректный email'
    }

    const passwordLength = Array.from(form.password).length

    if (passwordLength < 15 || passwordLength > 128) {
      result.password = 'Пароль должен содержать от 15 до 128 символов'
    }

    if (form.confirmPassword !== form.password) {
      result.confirmPassword = 'Пароли не совпадают'
    }

    if (!form.personalDataConsent) {
      result.personalDataConsent = 'Необходимо дать согласие'
    }

    return result
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isSubmitting || registrationSuccess) {
      return
    }

    setServerError(null)

    const validationErrors = validate()
    setErrors(validationErrors)

    if (Object.keys(validationErrors).length > 0) {
      return
    }

    const request: RegistrationRequest = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim(),
      password: form.password,
    }

    setIsSubmitting(true)

    try {
      await registerUser(role, request)
      setRegistrationSuccess(true)
    } catch (error) {
      if (error instanceof RegistrationApiError) {
        if (error.status === 409) {
          setServerError(
            'Пользователь с такой электронной почтой уже зарегистрирован.',
          )
        } else if (error.status === 400) {
          setServerError(
            'Проверьте введенные данные и попробуйте еще раз.',
          )
        } else {
          setServerError(
            'Не удалось зарегистрироваться. Попробуйте позже.',
          )
        }
      } else {
        setServerError(
          'Не удалось связаться с сервером. Проверьте подключение.',
        )
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="registration-form" noValidate onSubmit={handleSubmit}>
      <div className="registration-name-row">
        <div className="registration-field">
          <label htmlFor="registration-first-name">Имя</label>
          <input
            id="registration-first-name"
            type="text"
            autoComplete="given-name"
            value={form.firstName}
            onChange={(event) =>
              updateField('firstName', event.target.value)
            }
            placeholder="Ваше имя"
            aria-invalid={Boolean(errors.firstName)}
          />
          {errors.firstName && (
            <span className="field-error">{errors.firstName}</span>
          )}
        </div>

        <div className="registration-field">
          <label htmlFor="registration-last-name">Фамилия</label>
          <input
            id="registration-last-name"
            type="text"
            autoComplete="family-name"
            value={form.lastName}
            onChange={(event) =>
              updateField('lastName', event.target.value)
            }
            placeholder="Ваша фамилия"
            aria-invalid={Boolean(errors.lastName)}
          />
          {errors.lastName && (
            <span className="field-error">{errors.lastName}</span>
          )}
        </div>
      </div>

      <div className="registration-field">
        <label htmlFor="registration-email">Электронная почта</label>
        <input
          id="registration-email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={(event) =>
            updateField('email', event.target.value)
          }
          placeholder="example@mail.com"
          aria-invalid={Boolean(errors.email)}
        />
        {errors.email && (
          <span className="field-error">{errors.email}</span>
        )}
      </div>

      <div className="registration-field">
        <label htmlFor="registration-password">Пароль</label>
        <input
          id="registration-password"
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={(event) =>
            updateField('password', event.target.value)
          }
          placeholder="Не менее 15 символов"
          aria-invalid={Boolean(errors.password)}
        />
        {errors.password && (
          <span className="field-error">{errors.password}</span>
        )}
      </div>

      <div className="registration-field">
        <label htmlFor="registration-confirm-password">
          Повторите пароль
        </label>
        <input
          id="registration-confirm-password"
          type="password"
          autoComplete="new-password"
          value={form.confirmPassword}
          onChange={(event) =>
            updateField('confirmPassword', event.target.value)
          }
          placeholder="Введите пароль еще раз"
          aria-invalid={Boolean(errors.confirmPassword)}
        />
        {errors.confirmPassword && (
          <span className="field-error">
            {errors.confirmPassword}
          </span>
        )}
      </div>

      <div className="registration-consent">
        <label className="consent-label">
          <input
            type="checkbox"
            checked={form.personalDataConsent}
            onChange={(event) =>
              updateField('personalDataConsent', event.target.checked)
            }
          />
          <span>Я даю согласие на обработку персональных данных</span>
        </label>

        {errors.personalDataConsent && (
          <span className="field-error">
            {errors.personalDataConsent}
          </span>
        )}
      </div>

	  <button
	    className="registration-submit"
	    type="submit"
	    disabled={
	      !form.personalDataConsent ||
	      isSubmitting ||
	      registrationSuccess
	    }
	  >
	    {isSubmitting ? 'Регистрируем...' : 'Зарегистрироваться'}
	  </button>

	  {serverError && (
	    <p className="registration-server-error" role="alert">
	      {serverError}
	    </p>
	  )}

	  {registrationSuccess && (
	    <p className="registration-demo-success" role="status">
	      Аккаунт успешно создан!
	    </p>
	  )}
    </form>
  )
}