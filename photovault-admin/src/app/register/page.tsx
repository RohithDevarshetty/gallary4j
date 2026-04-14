'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import BrickLogo from '../../components/BrickLogo'

export default function RegisterPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({ email: '', password: '', studioName: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = await fetch('http://localhost:8080/api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      })
      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Registration failed')
      }
      const data = await response.json()
      localStorage.setItem('token', data.token)
      localStorage.setItem('photographerId', data.photographerId)
      router.push('/dashboard')
    } catch (err: any) {
      setError(err.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', minHeight: '100dvh' }}>
      {/* ── Left panel ── */}
      <div style={{
        flex: '0 0 42%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        padding: '3rem',
        background: 'linear-gradient(160deg, var(--bg) 0%, #0c0e0a 100%)',
        borderRight: '1px solid var(--border)',
        position: 'relative',
        overflow: 'hidden',
      }}>
        {/* Decorative circles */}
        <div style={{
          position: 'absolute',
          bottom: '-200px',
          left: '-200px',
          width: '480px',
          height: '480px',
          borderRadius: '50%',
          border: '1px solid rgba(200,169,106,0.06)',
          pointerEvents: 'none',
        }} />
        <div style={{
          position: 'absolute',
          bottom: '-140px',
          left: '-140px',
          width: '320px',
          height: '320px',
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

        <div className="anim-in">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <BrickLogo size={26} />
            <span style={{
              fontFamily: 'var(--font-brand)',
              fontWeight: 800,
              fontSize: '1.2rem',
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
              Free Trial
            </span>
          </div>
          <h2 className="anim-up d-2" style={{
            fontFamily: 'var(--font-serif)',
            fontSize: 'clamp(2rem, 4vw, 3.25rem)',
            fontWeight: 300,
            lineHeight: 1.15,
            color: 'var(--text)',
            marginBottom: '1.5rem',
          }}>
            Built for<br />
            <em style={{ color: 'var(--accent)', fontStyle: 'italic' }}>serious</em><br />
            photographers.
          </h2>
          <p className="anim-up d-3" style={{
            fontSize: '0.9rem',
            color: 'var(--text-muted)',
            lineHeight: 1.7,
          }}>
            14-day free trial. No credit card required.
            Full access to all Studio features from day one.
          </p>
        </div>

        {/* Plan features */}
        <div className="anim-up d-4" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {[
            'Unlimited photo galleries',
            'Client delivery & favorites',
            'Custom branding & domain',
            'Analytics & download tracking',
          ].map((feat) => (
            <div key={feat} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <span style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                background: 'var(--accent)',
                flexShrink: 0,
              }} />
              <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>{feat}</span>
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
        <div style={{ maxWidth: '380px', width: '100%' }}>
          <div className="anim-up">
            <h2 style={{
              fontFamily: 'var(--font-serif)',
              fontSize: '2.25rem',
              fontWeight: 400,
              color: 'var(--text)',
              marginBottom: '0.5rem',
            }}>
              Create account
            </h2>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '2.5rem' }}>
              Start your 14-day free trial today.
            </p>
          </div>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
            {error && (
              <div className="alert-error anim-in">{error}</div>
            )}

            <div className="field anim-up d-1">
              <label htmlFor="studioName">Studio name</label>
              <input
                id="studioName"
                type="text"
                required
                placeholder="Aperture Studio"
                value={formData.studioName}
                onChange={(e) => setFormData({ ...formData, studioName: e.target.value })}
              />
            </div>

            <div className="field anim-up d-2">
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

            <div className="field anim-up d-3">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                required
                minLength={6}
                placeholder="min. 6 characters"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>

            <div className="anim-up d-4" style={{ marginTop: '0.5rem' }}>
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? <><span className="spinner" /> Creating account…</> : 'Create Account'}
              </button>
            </div>

            <p className="anim-up d-5" style={{
              textAlign: 'center',
              fontSize: '0.875rem',
              color: 'var(--text-muted)',
            }}>
              Already have an account?{' '}
              <a href="/" style={{ color: 'var(--accent)', fontWeight: 500 }}>Sign in</a>
            </p>
          </form>
        </div>
      </div>
    </div>
  )
}
