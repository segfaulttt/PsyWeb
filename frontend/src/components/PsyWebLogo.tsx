
type PsyWebLogoProps = {
  showSubtitle?: boolean
}

export function PsyWebLogo({
  showSubtitle = true,
}: PsyWebLogoProps) {
  return (
    <div className="identity">
      <div className="brand">
        PsyWeb
        <svg
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
            <ellipse
              cx="15"
              cy="15"
              rx="5"
              ry="9"
              transform="rotate(-45 15 15)"
            />
            <ellipse
              cx="33"
              cy="33"
              rx="5"
              ry="9"
              transform="rotate(-45 33 33)"
            />
          </g>
        </svg>
      </div>

      {showSubtitle && (
        <span className="brand-subtitle">
          пространство для заботы о себе
        </span>
      )}
    </div>
  )
}