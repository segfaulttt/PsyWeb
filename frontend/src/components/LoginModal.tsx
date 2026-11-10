import { useEffect, useState, type FormEvent } from 'react'
import {
  AuthApiError,
  getAuthStatus,
  login,
} from '../api/auth'

type LoginModalProps = {
  onClose: () => void
  onSuccess: () => void
}

export function LoginModal({
  onClose,
  onSuccess,
}: LoginModalProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [onClose])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isSubmitting) return

    setError(null)

    if (!email.trim() || !password) {
      setError('Заполните электронную почту и пароль')
      return
    }

    setIsSubmitting(true)

	try {
	  await login(email, password)

	if (getAuthStatus() === 'authenticated') {
	  onSuccess()
	  }
    } catch (cause) {
      if (cause instanceof AuthApiError) {
        if (cause.status === 401) {
          setError('Неверная электронная почта или пароль')
        } else if (cause.status === 429) {
          setError('Слишком много попыток. Попробуйте позже')
        } else if (cause.status === 403) {
          setError('Не удалось подтвердить запрос. Попробуйте ещё раз')
        } else {
          setError('Не удалось войти. Попробуйте позже')
        }
      } else {
        setError('Не удалось связаться с сервером')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="modal-overlay"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose()
        }
      }}
    >
      <section
        className="login-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-title"
      >
        <button
          className="modal-close"
          type="button"
          aria-label="Закрыть окно входа"
          onClick={onClose}
        >
          ×
        </button>

        <h2 id="login-title">Вход в аккаунт</h2>

        <form className="login-form" onSubmit={handleSubmit}>
          <div className="registration-field">
            <label htmlFor="login-email">
              Электронная почта
            </label>
            <input
              id="login-email"
              type="email"
              autoComplete="email"
              placeholder="example@mail.com"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value)
                setError(null)
              }}
              required
              autoFocus
            />
          </div>

          <div className="registration-field">
            <label htmlFor="login-password">Пароль</label>

            <div className="login-password-wrapper">
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="Введите пароль"
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value)
                  setError(null)
                }}
                required
              />

              <button
                className="login-password-toggle"
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={
                  showPassword ? 'Скрыть пароль' : 'Показать пароль'
                }
                aria-pressed={showPassword}
              >
                {showPassword ? 'Скрыть' : 'Показать'}
              </button>
            </div>

            <button
              className="login-forgot-password"
              type="button"
              disabled
              title="Восстановление пароля пока недоступно"
            >
              Забыли пароль?
            </button>
          </div>

          {error && (
            <p className="registration-server-error" role="alert">
              {error}
            </p>
          )}

          <button
            className="registration-submit"
            type="submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Входим...' : 'Войти'}
          </button>
        </form>
      </section>
    </div>
  )
}