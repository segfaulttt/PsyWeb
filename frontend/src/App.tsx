
import './App.css'

function App() {
  return (
    <main className="site">
		<div className="hero-visual" aria-hidden="true" />
      <header className="site-header">
        <div className="identity">
          <div className="brand">
            PsyWeb <svg
		    className="brand-flower"
		    viewBox="0 0 48 48"
		    fill="none"
		    aria-hidden="true"
		  >
		    <g fill="#8d9c73">
		      <ellipse cx="24" cy="11" rx="5" ry="10" />
		      <ellipse cx="24" cy="37" rx="5" ry="10" />
		      <ellipse cx="11" cy="24" rx="10" ry="5" />
		      <ellipse cx="37" cy="24" rx="10" ry="5" />
		      <ellipse cx="15" cy="15" rx="5" ry="9" transform="rotate(-45 15 15)" />
		      <ellipse cx="33" cy="33" rx="5" ry="9" transform="rotate(-45 33 33)" />
		    </g>
		  </svg>
          </div>
          <span className="brand-subtitle">
            пространство для заботы о себе
          </span>
        </div>

        <nav className="navigation" aria-label="Основная навигация">
          <span>О платформе</span>
          <span>Специалисты</span>
          <span>Статьи</span>
          <span>Вопросы</span>
        </nav>

        <div className="header-actions">
          <span className="login-label">Войти</span>
          <span className="signup-label">Регистрация</span>
        </div>
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
              <span className="audience-action">
                Найти специалиста <span>→</span>
              </span>
            </article>

            <article className="audience-card">
              <div className="audience-icon">✳</div>
              <h2>Я специалист</h2>
              <p>
                Развивайте практику, находите новых клиентов
                и становитесь частью профессионального сообщества.
              </p>
              <span className="audience-action audience-action-light">
                Создать профиль <span>→</span>
              </span>
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
    </main>
  )
}

export default App