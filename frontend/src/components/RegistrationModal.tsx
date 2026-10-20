
import { useEffect } from 'react'
import { PsyWebLogo } from './PsyWebLogo'
import type { RegistrationRole } from '../types/registration'
import { RegistrationForm } from './RegistrationForm'

type RegistrationModalProps = {
  role: RegistrationRole
  onRoleChange: (role: RegistrationRole) => void
  onClose: () => void
}

export function RegistrationModal({
  role,
  onRoleChange,
  onClose,
}: RegistrationModalProps) {
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
        className="registration-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="registration-title"
      >
        <button
          className="modal-close"
          type="button"
          aria-label="Закрыть окно регистрации"
          onClick={onClose}
        >
          ×
        </button>

        <PsyWebLogo showSubtitle={false} />

        <h2 id="registration-title">Создайте аккаунт</h2>

        <p className="registration-subtitle">
          Начните свой путь с PsyWeb
        </p>

        <div className="registration-role-switch">
          <button
            type="button"
            className={role === 'CLIENT' ? 'role-active' : ''}
            aria-pressed={role === 'CLIENT'}
            onClick={() => onRoleChange('CLIENT')}
          >
            Я клиент
          </button>

          <button
            type="button"
            className={role === 'SPECIALIST' ? 'role-active' : ''}
            aria-pressed={role === 'SPECIALIST'}
            onClick={() => onRoleChange('SPECIALIST')}
          >
            Я специалист
          </button>
        </div>

        <RegistrationForm role={role} />
      </section>
    </div>
  )
}