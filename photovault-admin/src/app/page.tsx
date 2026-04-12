'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

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
        background: 'linear-gradient(160deg, #0d0d0d 0%, #111008 100%)',
        borderRight: '1px solid var(--border)',
        position: 'relative',
        overflow: 'hidden',
      }}>
        {/* Decorative circle */}
        <div style={{
          position: 'absolute',
          top: '-180px',
          right: '-180px',
          width: '520px',
          height: '520px',
          borderRadius: '50%',
          border: '1px solid rgba(200,169,106,0.08)',
          pointerEvents: 'none',
        }} />
        <div style={{
          position: 'absolute',
          top: '-100px',
          right: '-100px',
          width: '360px',
          height: '360px',
          borderRadius: '50%',
          border: '1px solid rgba(200,169,106,0.06)',
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
          <div style={{
            fontFamily: 'var(--font-serif)',
            fontSize: '1.4rem',
            fontStyle: 'italic',
            fontWeight: 400,
            color: 'var(--text)',
            letterSpacing: '0.02em',
          }}>
            Photo<span style={{ color: 'var(--accent)' }}>Vault</span>
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
              fontSize: '0.6875rem',
              fontWeight: 500,
              letterSpacing: '0.14em',
              textTransform: 'uppercase',
              color: 'var(--accent)',
            }}>
              Studio Platform
            </span>
          </div>

          <h1 className="anim-up d-2" style={{
            fontFamily: 'var(--font-serif)',
            fontSize: 'clamp(2.5rem, 5vw, 4rem)',
            fontWeight: 300,
            lineHeight: 1.1,
            color: 'var(--text)',
            letterSpacing: '-0.01em',
            marginBottom: '1.5rem',
          }}>
            Your gallery,<br />
            <em style={{ color: 'var(--accent)', fontStyle: 'italic' }}>your vision.</em>
          </h1>

          <p className="anim-up d-3" style={{
            fontSize: '0.9375rem',
            color: 'var(--text-muted)',
            lineHeight: 1.7,
            maxWidth: '380px',
          }}>
            Deliver stunning galleries to your clients. Enterprise-grade platform
            built for photographers who demand more.
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
        background: 'var(--surface)',
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
