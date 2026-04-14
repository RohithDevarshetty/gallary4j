interface BrickLogoProps {
  size?: number
}

/**
 * Glass-brick mark — 5 frosted bricks in two staggered rows,
 * each with a specular highlight and warm-to-gold gradient fill.
 */
export default function BrickLogo({ size = 24 }: BrickLogoProps) {
  const h = Math.round(size * 0.72)
  return (
    <svg
      width={size}
      height={h}
      viewBox="0 0 30 22"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        {/* Main warm glass gradient */}
        <linearGradient id="bg-a" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%"   stopColor="rgba(255,255,255,0.38)" />
          <stop offset="55%"  stopColor="rgba(200,169,106,0.28)" />
          <stop offset="100%" stopColor="rgba(140,110,60,0.18)"  />
        </linearGradient>
        {/* Cooler secondary brick */}
        <linearGradient id="bg-b" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%"   stopColor="rgba(255,255,255,0.28)" />
          <stop offset="60%"  stopColor="rgba(180,200,255,0.18)" />
          <stop offset="100%" stopColor="rgba(100,120,180,0.12)" />
        </linearGradient>
        {/* Specular top-highlight strip */}
        <linearGradient id="hi" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%"   stopColor="rgba(255,255,255,0.55)" />
          <stop offset="100%" stopColor="rgba(255,255,255,0)"    />
        </linearGradient>
      </defs>

      {/* ── Row 1 (top) — 3 bricks ── */}
      {/* Brick A */}
      <rect x="0"    y="0" width="9" height="8" rx="1.6"
            fill="url(#bg-a)" stroke="rgba(255,255,255,0.42)" strokeWidth="0.7"/>
      <rect x="0.8"  y="0.8" width="7.4" height="3.2" rx="1"
            fill="url(#hi)" opacity="0.9"/>

      {/* Brick B */}
      <rect x="10.5" y="0" width="9" height="8" rx="1.6"
            fill="url(#bg-b)" stroke="rgba(255,255,255,0.32)" strokeWidth="0.7"/>
      <rect x="11.3" y="0.8" width="7.4" height="3.2" rx="1"
            fill="url(#hi)" opacity="0.7"/>

      {/* Brick C */}
      <rect x="21"   y="0" width="9" height="8" rx="1.6"
            fill="url(#bg-a)" stroke="rgba(255,255,255,0.38)" strokeWidth="0.7"/>
      <rect x="21.8" y="0.8" width="7.2" height="3.2" rx="1"
            fill="url(#hi)" opacity="0.85"/>

      {/* ── Row 2 (bottom) — 2 bricks, offset ── */}
      {/* Brick D */}
      <rect x="5.25" y="10" width="9" height="8" rx="1.6"
            fill="url(#bg-b)" stroke="rgba(255,255,255,0.36)" strokeWidth="0.7"/>
      <rect x="6.05" y="10.8" width="7.4" height="3.2" rx="1"
            fill="url(#hi)" opacity="0.75"/>

      {/* Brick E */}
      <rect x="15.75" y="10" width="9" height="8" rx="1.6"
            fill="url(#bg-a)" stroke="rgba(255,255,255,0.42)" strokeWidth="0.7"/>
      <rect x="16.55" y="10.8" width="7.4" height="3.2" rx="1"
            fill="url(#hi)" opacity="0.9"/>
    </svg>
  )
}
