'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import BrickLogo from '../components/BrickLogo'

export default function LoginPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = await fetch('http://localhost:8080/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      })
      if (!response.ok) throw new Error('Login failed')
      const data = await response.json()
      localStorage.setItem('token', data.token)
      localStorage.setItem('photographerId', data.photographerId)
      router.push('/dashboard')
    } catch {
      setError('Invalid email or password. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', minHeight: '100dvh' }}>
      {/* ── Left panel ── */}
      <div style={{
        flex: '0 0 55%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        padding: '3rem',
        background: 'linear-gradient(160deg, #060708 0%, #0a0c10 100%)',
        borderRight: '1px solid rgba(255,255,255,0.07)',
        position: 'relative',
        overflow: 'hidden',
      }}>
        {/* Decorative circles */}
        <div style={{
          position: 'absolute',
          top: '-180px',
          right: '-180px',
          width: '520px',
          height: '520px',
          borderRadius: '50%',
          border: '1px solid rgba(200,169,106,0.06)',
          pointerEvents: 'none',
        }} />
        <div style={{
          position: 'absolute',
          top: '-100px',
          right: '-100px',
          width: '360px',
          height: '360px',
          borderRadius: '50%',
          border: '1px solid rgba(200,169,106,0.04)',
          pointerEvents: 'none',
        }} />
        {/* Accent line */}
        <div style={{
          position: 'absolute',
          bottom: 0,
          left: '3rem',
          right: '3rem',
          height: '1px',
          background: 'linear-gradient(to right, var(--accent-dim), transparent)',
        }} />

        {/* Brand */}
        <div className="anim-in">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <BrickLogo size={28} />
            <span style={{
              fontFamily: 'var(--font-brand)',
              fontWeight: 800,
              fontSize: '1.3rem',
              letterSpacing: '0.22em',
              textTransform: 'uppercase',
              background: 'linear-gradient(135deg, rgba(255,255,255,0.95) 0%, var(--accent) 100%)',
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}>
              Brick
            </span>
          </div>
        </div>

        {/* Main copy */}
        <div>
          <div className="anim-up d-1" style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            marginBottom: '2rem',
          }}>
            <span style={{ width: '24px', height: '1px', background: 'var(--accent)' }} />
            <span style={{
              fontFamily: 'var(--font-brand)',
              fontSize: '0.6rem',
              fontWeight: 700,
              letterSpacing: '0.22em',
              textTransform: 'uppercase',
              color: 'var(--accent)',
            }}>
              Studio Platform
            </span>
          </div>

          <h1 className="anim-up d-2" style={{
            fontFamily: 'var(--font-brand)',
            fontSize: 'clamp(3rem, 6vw, 5rem)',
            fontWeight: 800,
            lineHeight: 1.0,
            letterSpacing: '-0.02em',
            marginBottom: '1.5rem',
          }}>
            <span style={{
              background: 'linear-gradient(135deg, rgba(255,255,255,0.95) 0%, rgba(255,255,255,0.7) 40%, var(--accent) 100%)',
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              display: 'block',
            }}>
              Build your<br />world,
            </span>
            <span style={{
              background: 'linear-gradient(135deg, var(--accent) 0%, var(--accent-hi) 60%, rgba(255,230,160,0.9) 100%)',
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              display: 'block',
              fontSize: '0.85em',
              letterSpacing: '0.06em',
              fontStyle: 'normal',
            }}>
              glass by glass.
            </span>
          </h1>

          <p className="anim-up d-3" style={{
            fontSize: '0.9375rem',
            color: 'var(--text-muted)',
            lineHeight: 1.7,
            maxWidth: '380px',
          }}>
            Deliver stunning galleries to your clients.
            Every session, beautifully framed.
          </p>
        </div>

        {/* Bottom meta */}
        <div className="anim-up d-4" style={{
          display: 'flex',
          gap: '2.5rem',
        }}>
          {[['100K+', 'Galleries Hosted'], ['99.9%', 'Uptime SLA'], ['5 GB', 'Max File Size']].map(([val, label]) => (
            <div key={label}>
              <div className="stat-value" style={{ fontSize: '1.5rem' }}>{val}</div>
              <div className="stat-label">{label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Right panel ── */}
      <div style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        padding: '3rem 4rem',
        background: 'linear-gradient(145deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0.01) 100%)',
        backdropFilter: 'blur(32px)',
        WebkitBackdropFilter: 'blur(32px)',
      }}>
        <div style={{ maxWidth: '360px', width: '100%' }}>
          <div className="anim-up">
            <h2 style={{
              fontFamily: 'var(--font-serif)',
              fontSize: '2.25rem',
              fontWeight: 400,
              color: 'var(--text)',
              marginBottom: '0.5rem',
            }}>
              Sign in
            </h2>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '2.5rem' }}>
              Welcome back to your studio dashboard.
            </p>
          </div>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
            {error && (
              <div className="alert-error anim-in">{error}</div>
            )}

            <div className="field anim-up d-1">
              <label htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                required
                placeholder="you@studio.com"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>

            <div className="field anim-up d-2">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                required
                placeholder="••••••••"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>

            <div className="anim-up d-3" style={{ marginTop: '0.5rem' }}>
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? <><span className="spinner" /> Signing in…</> : 'Sign In'}
              </button>
            </div>

            <p className="anim-up d-4" style={{
              textAlign: 'center',
              fontSize: '0.875rem',
              color: 'var(--text-muted)',
            }}>
              No account?{' '}
              <a href="/register" style={{ color: 'var(--accent)', fontWeight: 500 }}>
                Start your free trial
              </a>
            </p>
          </form>
        </div>
      </div>
    </div>
  )
}
