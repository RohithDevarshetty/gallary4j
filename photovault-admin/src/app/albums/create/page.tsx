'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

export default function CreateAlbumPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    clientName: '',
    clientEmail: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const token = localStorage.getItem('token')
      if (!token) { router.push('/'); return }
      const response = await fetch('http://localhost:8080/api/v1/albums', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      })
      if (!response.ok) throw new Error('Failed to create album')
      const album = await response.json()
      router.push(`/albums/${album.id}`)
    } catch (err: any) {
      setError(err.message || 'Failed to create album')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>
      {/* Nav */}
      <nav className="nav">
        <div className="nav-inner">
          <span className="nav-brand">Photo<span>Vault</span></span>
          <button
            className="btn-ghost"
            onClick={() => router.push('/dashboard')}
          >
            ← Dashboard
          </button>
        </div>
      </nav>

      <div className="page" style={{ maxWidth: '680px' }}>
        {/* Header */}
        <div className="anim-up" style={{ marginBottom: '2.5rem' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            marginBottom: '1rem',
          }}>
            <span style={{ width: '20px', height: '1px', background: 'var(--accent)' }} />
            <span style={{
              fontSize: '0.6875rem',
              fontWeight: 500,
              letterSpacing: '0.14em',
              textTransform: 'uppercase',
              color: 'var(--accent)',
            }}>New Album</span>
          </div>
          <h1 style={{
            fontFamily: 'var(--font-serif)',
            fontSize: 'clamp(1.75rem, 3vw, 2.5rem)',
            fontWeight: 300,
            color: 'var(--text)',
          }}>
            Create a gallery
          </h1>
        </div>

        <form onSubmit={handleSubmit}>
          {error && (
            <div className="alert-error anim-in" style={{ marginBottom: '1.5rem' }}>{error}</div>
          )}

          {/* Album info */}
          <div className="card anim-up d-1" style={{ padding: '2rem', marginBottom: '1rem' }}>
            <h3 style={{
              fontSize: '0.6875rem',
              fontWeight: 500,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              marginBottom: '1.5rem',
            }}>Album Details</h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
              <div className="field">
                <label htmlFor="title">Album title *</label>
                <input
                  id="title"
                  type="text"
                  required
                  placeholder="Wedding at Willow Creek"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                />
              </div>

              <div className="field">
                <label htmlFor="description">Description</label>
                <textarea
                  id="description"
                  placeholder="A brief description of this gallery…"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>
            </div>
          </div>

          {/* Client info */}
          <div className="card anim-up d-2" style={{ padding: '2rem', marginBottom: '2rem' }}>
            <h3 style={{
              fontSize: '0.6875rem',
              fontWeight: 500,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              marginBottom: '1.5rem',
            }}>Client Information</h3>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
              <div className="field">
                <label htmlFor="clientName">Client name</label>
                <input
                  id="clientName"
                  type="text"
                  placeholder="Jane & John Smith"
                  value={formData.clientName}
                  onChange={(e) => setFormData({ ...formData, clientName: e.target.value })}
                />
              </div>

              <div className="field">
                <label htmlFor="clientEmail">Client email</label>
                <input
                  id="clientEmail"
                  type="email"
                  placeholder="client@email.com"
                  value={formData.clientEmail}
                  onChange={(e) => setFormData({ ...formData, clientEmail: e.target.value })}
                />
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="anim-up d-3" style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
            <button
              type="button"
              className="btn-ghost"
              onClick={() => router.push('/dashboard')}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={loading}
              style={{ width: 'auto', padding: '0.875rem 2rem' }}
            >
              {loading ? <><span className="spinner" style={{
                borderColor: 'rgba(10,10,10,0.2)',
                borderTopColor: '#0a0a0a',
              }} /> Creating…</> : 'Create Album →'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
