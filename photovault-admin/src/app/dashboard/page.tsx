'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import AdminShell from '../../components/AdminShell'

interface Album {
  id: string
  title: string
  slug: string
  mediaCount: number
  viewCount: number
  createdAt: string
  clientName?: string
  clientEmail?: string
  description?: string
  category?: string
}

export default function DashboardPage() {
  const router = useRouter()
  const [albums, setAlbums] = useState<Album[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) { router.push('/'); return }
    fetchAlbums(token)
  }, [])

  const fetchAlbums = async (token: string) => {
    try {
      const response = await fetch('http://localhost:8080/api/v1/albums', {
        headers: { 'Authorization': `Bearer ${token}` },
      })
      if (!response.ok) throw new Error('Failed to fetch albums')
      const data = await response.json()
      setAlbums(data.content || [])
    } catch (error) {
      console.error('Error fetching albums:', error)
    } finally {
      setLoading(false)
    }
  }

  const totalPhotos = albums.reduce((sum, a) => sum + (a.mediaCount || 0), 0)
  const totalViews  = albums.reduce((sum, a) => sum + (a.viewCount || 0), 0)

  const filteredAlbums = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return albums
    return albums.filter(a => {
      const haystack = [a.title, a.description, a.clientName, a.clientEmail, a.category]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return haystack.includes(q)
    })
  }, [albums, search])

  const now = new Date()
  const dateStr = now.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })

  return (
    <AdminShell>
      <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>
        <div className="page">
          {/* Header */}
          <div className="anim-up" style={{ marginBottom: '2.5rem', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
            <div>
              <span style={{
                fontFamily: 'var(--font-brand)',
                fontSize: '0.6rem',
                fontWeight: 700,
                letterSpacing: '0.22em',
                textTransform: 'uppercase',
                color: 'var(--text-muted)',
              }}>Your Studio</span>
              <h1 style={{
                fontFamily: 'var(--font-brand)',
                fontSize: 'clamp(1.75rem, 3vw, 2.75rem)',
                fontWeight: 800,
                letterSpacing: '-0.02em',
                background: 'linear-gradient(135deg, var(--text) 0%, rgba(200,169,106,0.85) 100%)',
                WebkitBackgroundClip: 'text',
                backgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                marginTop: '0.25rem',
              }}>
                Your galleries
              </h1>
            </div>
            <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', paddingBottom: '0.25rem' }}>
              {dateStr}
            </span>
          </div>

          {/* Stats row */}
          <div className="anim-up d-1" style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '0.75rem',
            marginBottom: '2.5rem',
          }}>
            {[
              { val: albums.length, label: 'Total Albums' },
              { val: totalPhotos,   label: 'Photos Uploaded' },
              { val: totalViews,    label: 'Gallery Views' },
            ].map(({ val, label }) => (
              <div key={label} className="glass-panel" style={{ padding: '1.5rem 1.75rem' }}>
                <div style={{
                  fontFamily: 'var(--font-serif)',
                  fontSize: '2rem',
                  fontWeight: 400,
                  lineHeight: 1,
                  background: 'linear-gradient(135deg, var(--text) 0%, var(--accent) 100%)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundClip: 'text',
                }}>
                  {val}
                </div>
                <div className="stat-label">{label}</div>
              </div>
            ))}
          </div>

          {/* Albums section header */}
          <div className="anim-up d-2" style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1rem',
            marginBottom: '1.25rem',
          }}>
            <h2 style={{
              fontSize: '0.6875rem',
              fontWeight: 500,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              whiteSpace: 'nowrap',
            }}>
              Albums ({search ? `${filteredAlbums.length} of ${albums.length}` : albums.length})
            </h2>
            {albums.length > 0 && (
              <div style={{ position: 'relative', maxWidth: '320px', width: '100%' }}>
                <input
                  type="text"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  placeholder="Search by name, email, description…"
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '0.5rem 0.875rem 0.5rem 2rem',
                    background: 'var(--card)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-sm)',
                    color: 'var(--text)',
                    fontSize: '0.8125rem',
                    fontFamily: 'var(--font-sans)',
                    outline: 'none',
                  }}
                  onFocus={e => (e.target.style.borderColor = 'var(--accent-dim)')}
                  onBlur={e => (e.target.style.borderColor = 'var(--border)')}
                />
                <span style={{
                  position: 'absolute',
                  left: '0.7rem',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  fontSize: '0.8125rem',
                  color: 'var(--text-dim)',
                  pointerEvents: 'none',
                }}>⌕</span>
                {search && (
                  <button
                    onClick={() => setSearch('')}
                    style={{
                      position: 'absolute',
                      right: '0.5rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-dim)',
                      cursor: 'pointer',
                      fontSize: '0.875rem',
                      padding: '0.25rem',
                    }}
                    title="Clear"
                  >
                    ✕
                  </button>
                )}
              </div>
            )}
          </div>

          {loading ? (
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '0.75rem',
            }}>
              {[1,2,3].map(i => (
                <div key={i} style={{
                  height: '140px',
                  background: 'var(--card)',
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-lg)',
                  animation: 'pulse 1.5s ease-in-out infinite',
                }} />
              ))}
            </div>
          ) : albums.length === 0 ? (
            <div className="anim-up d-3 drop-zone" style={{ maxWidth: '500px' }}>
              <div style={{
                fontFamily: 'var(--font-serif)',
                fontSize: '3rem',
                lineHeight: 1,
                color: 'var(--text-dim)',
                marginBottom: '1rem',
              }}>✦</div>
              <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                No albums yet. Create your first gallery to get started.
              </p>
              <Link href="/albums/create" className="btn-accent-outline" style={{ width: 'auto', display: 'inline-flex' }}>
                Create your first album
              </Link>
            </div>
          ) : filteredAlbums.length === 0 ? (
            <div style={{
              padding: '3rem 1rem',
              textAlign: 'center',
              border: '1px dashed var(--border)',
              borderRadius: 'var(--radius-lg)',
              color: 'var(--text-muted)',
              fontSize: '0.875rem',
            }}>
              No albums match “{search}”.
            </div>
          ) : (
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '0.75rem',
            }}>
              {filteredAlbums.map((album, i) => (
                <div
                  key={album.id}
                  className="glass-card anim-up"
                  style={{ animationDelay: `${0.04 * i}s` }}
                >

                  <div style={{ padding: '1.25rem' }}>
                    {/* Title row */}
                    <div style={{ marginBottom: '0.75rem' }}>
                      <h3 style={{
                        fontSize: '0.9375rem',
                        fontWeight: 500,
                        color: 'var(--text)',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        marginBottom: '0.2rem',
                      }}>
                        {album.title}
                      </h3>
                      {album.clientName && (
                        <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                          {album.clientName}
                        </div>
                      )}
                    </div>

                    {/* Stats row */}
                    <div style={{
                      display: 'flex',
                      gap: '0.75rem',
                      fontSize: '0.75rem',
                      color: 'var(--text-dim)',
                      marginBottom: '1.25rem',
                    }}>
                      <span>{album.mediaCount || 0} photos</span>
                      <span>·</span>
                      <span>{album.viewCount || 0} views</span>
                      <span>·</span>
                      <span>{new Date(album.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</span>
                    </div>

                    {/* Actions */}
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <Link
                        href={`/albums/${album.id}`}
                        className="btn-accent-outline"
                        style={{ flex: 1, fontSize: '0.75rem', padding: '0.45rem 0.75rem' }}
                      >
                        Manage
                      </Link>
                      {album.slug ? (
                        <a
                          href={`http://localhost:3001/gallery/${album.slug}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="btn-ghost"
                          style={{ flex: 1, fontSize: '0.75rem', padding: '0.45rem 0.75rem' }}
                        >
                          Preview ↗
                        </a>
                      ) : (
                        <span
                          className="btn-ghost"
                          style={{ flex: 1, fontSize: '0.75rem', padding: '0.45rem 0.75rem', opacity: 0.35, cursor: 'not-allowed' }}
                          title="No public URL yet"
                        >
                          No URL
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </AdminShell>
  )
}
