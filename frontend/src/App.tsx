
import './App.css'
import { useEffect, useSyncExternalStore, useState } from 'react'
import { RegistrationModal } from './components/RegistrationModal'
import type { RegistrationRole } from './types/registration'
import { PsyWebLogo } from './components/PsyWebLogo'
import { LoginModal } from './components/LoginModal'
import {
  getAuthStatus,
  subscribeToAuth,
  restoreSession,
  logout,
} from './api/auth'

function App() {
	const [registrationRole, setRegistrationRole] =
		useState<RegistrationRole | null>(null)


		const [isLoginOpen, setIsLoginOpen] = useState(false)

		const authStatus = useSyncExternalStore(
		  subscribeToAuth,
		  getAuthStatus,
		)

		const [logoutError, setLogoutError] = useState<string | null>(null)

		useEffect(() => {
		  void restoreSession()
		}, [])

		async function handleLogout() {
		  setLogoutError(null)

		  try {
		    await logout()
		  } catch {
		    setLogoutError(
		      'Не удалось завершить сессию на сервере. Попробуйте еще раз.',
		    )
		  }
		}

  return (
    <main className="site">
		<div className="hero-visual" aria-hidden="true" />

		<header className="site-header">
        <PsyWebLogo />

        <nav className="navigation" aria-label="Основная навигация">
          <span>О платформе</span>
          <span>Специалисты</span>
          <span>Статьи</span>
          <span>Вопросы</span>
        </nav>

        <div className="header-actions">


		{authStatus === 'loading' ? (
		  <span className="login-label">Проверяем сессию...</span>
		) : authStatus === 'authenticated' ? (
		  <button
		    className="login-label"
		    type="button"
		    onClick={() => void handleLogout()}
		  >
		    Выйти
		  </button>
		) : (
		  <button
		    className="login-label"
		    type="button"
		    onClick={() => setIsLoginOpen(true)}
		  >
		    Войти
		  </button>
		)}

		{authStatus === 'guest' && (
		  <button
		    className="signup-label"
		    type="button"
		    onClick={() => setRegistrationRole('CLIENT')}
		  >
		    Регистрация
		  </button>
		)}
        </div>

		{logoutError && (
		  <p role="alert" className="registration-server-error">
		    {logoutError}
		  </p>
		)}

      </header>

      <section className="hero">

        <div className="hero-content">
          <span className="eyebrow">
            ПОДДЕРЖКА · ПОНИМАНИЕ · РАЗВИТИЕ
          </span>

          <h1>
            Забота о себе
            <br />
            начинается с
            <br />
            маленького шага
          </h1>

          <p className="hero-description">
            Находите специалистов, выбирайте удобный формат
            и записывайтесь на консультацию — онлайн или очно.
            PsyWeb — пространство, где можно начать изменения
            в комфортном для себя темпе.
          </p>

          <div className="audience">
            <article className="audience-card">
              <div className="audience-icon">♡</div>
              <h2>Я клиент</h2>
              <p>
                Найдите специалиста, который подходит именно вам,
                и сделайте первый шаг к позитивным изменениям.
              </p>
			  <button
			    className="audience-action"
			    type="button"
			    onClick={() => setRegistrationRole('CLIENT')}
			  >
			    Найти специалиста <span>{'\u2192'}</span>
			  </button>
            </article>

            <article className="audience-card">
              <div className="audience-icon">✳</div>
              <h2>Я специалист</h2>
              <p>
                Развивайте практику, находите новых клиентов
                и становитесь частью профессионального сообщества.
              </p>
			  <button
			    className="audience-action audience-action-light"
			    type="button"
			    onClick={() => setRegistrationRole('SPECIALIST')}
			  >
			    Создать профиль <span>{'\u2192'}</span>
			  </button>
            </article>
          </div>
        </div>
      </section>

      <section className="benefits" aria-label="Преимущества PsyWeb">
        <div className="benefit">
          <span className="benefit-icon">✧</span>
          <div>
            <h3>Проверенные специалисты</h3>
            <p>Внимательный подход к выбору профессионалов</p>
          </div>
        </div>

        <div className="benefit">
          <span className="benefit-icon">◷</span>
          <div>
            <h3>Удобная запись</h3>
            <p>Выбирайте время и формат консультации</p>
          </div>
        </div>

        <div className="benefit">
          <span className="benefit-icon">♡</span>
          <div>
            <h3>Личное пространство</h3>
            <p>Забота о конфиденциальности и комфорте</p>
          </div>
        </div>
      </section>
	  {registrationRole !== null && (
	    <RegistrationModal
	      role={registrationRole}
	      onRoleChange={setRegistrationRole}
	      onClose={() => setRegistrationRole(null)}
	    />
	  )}

	  {isLoginOpen && (
		<LoginModal
		  onClose={() => setIsLoginOpen(false)}
		  onSuccess={() => {
		    setLogoutError(null)
		    setIsLoginOpen(false)
		  }}
		/>
	  )}

    </main>
  )
}

export default App