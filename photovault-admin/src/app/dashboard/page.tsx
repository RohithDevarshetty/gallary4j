'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'

interface Album {
  id: string
  title: string
  slug: string
  mediaCount: number
  viewCount: number
  createdAt: string
  clientName?: string
}

export default function DashboardPage() {
  const router = useRouter()
  const [albums, setAlbums] = useState<Album[]>([])
  const [loading, setLoading] = useState(true)

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

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('photographerId')
    router.push('/')
  }

  const totalPhotos = albums.reduce((sum, a) => sum + (a.mediaCount || 0), 0)
  const totalViews  = albums.reduce((sum, a) => sum + (a.viewCount || 0), 0)

  return (
    <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>
      {/* Nav */}
      <nav className="nav">
        <div className="nav-inner">
          <span className="nav-brand">Photo<span>Vault</span></span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Link href="/albums/create" className="btn-accent-outline">
              + New Album
            </Link>
            <button className="btn-ghost" onClick={handleLogout}>
              Sign out
            </button>
          </div>
        </div>
      </nav>

      <div className="page">
        {/* Header */}
        <div className="anim-up" style={{ marginBottom: '2.5rem' }}>
          <h1 style={{
            fontFamily: 'var(--font-serif)',
            fontSize: 'clamp(1.75rem, 3vw, 2.5rem)',
            fontWeight: 300,
            color: 'var(--text)',
            marginBottom: '0.5rem',
          }}>
            Your Studio
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Manage your albums and client galleries.
          </p>
        </div>

        {/* Stats row */}
        <div className="anim-up d-1" style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '1px',
          background: 'var(--border)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-lg)',
          overflow: 'hidden',
          marginBottom: '2.5rem',
        }}>
          {[
            [albums.length, 'Total Albums'],
            [totalPhotos, 'Photos Uploaded'],
            [totalViews, 'Gallery Views'],
          ].map(([val, label]) => (
            <div key={String(label)} style={{
              background: 'var(--card)',
              padding: '1.5rem 2rem',
            }}>
              <div className="stat-value">{val}</div>
              <div className="stat-label">{label}</div>
            </div>
          ))}
        </div>

        {/* Albums section */}
        <div className="anim-up d-2" style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1.25rem',
        }}>
          <h2 style={{
            fontSize: '0.6875rem',
            fontWeight: 500,
            letterSpacing: '0.12em',
            textTransform: 'uppercase',
            color: 'var(--text-muted)',
          }}>
            Albums ({albums.length})
          </h2>
        </div>

        {loading ? (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
            gap: '1rem',
          }}>
            {[1,2,3].map(i => (
              <div key={i} style={{
                height: '160px',
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
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
            gap: '1rem',
          }}>
            {albums.map((album, i) => (
              <div
                key={album.id}
                className={`card anim-up`}
                style={{
                  animationDelay: `${0.04 * i}s`,
                  overflow: 'hidden',
                }}
              >
                {/* Cover area */}
                <div style={{
                  height: '120px',
                  background: `linear-gradient(135deg, #161616 0%, #1c1a14 100%)`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderBottom: '1px solid var(--border)',
                  position: 'relative',
                  overflow: 'hidden',
                }}>
                  <span style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: '2.5rem',
                    fontStyle: 'italic',
                    color: 'var(--accent-dim)',
                    opacity: 0.5,
                    letterSpacing: '-0.02em',
                  }}>
                    {album.title.charAt(0)}
                  </span>
                  {/* Decorative corner accent */}
                  <div style={{
                    position: 'absolute',
                    top: '1rem',
                    right: '1rem',
                  }}>
                    <span className="badge">{album.mediaCount || 0} photos</span>
                  </div>
                </div>

                <div style={{ padding: '1.25rem' }}>
                  <h3 style={{
                    fontSize: '1rem',
                    fontWeight: 500,
                    color: 'var(--text)',
                    marginBottom: '0.25rem',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}>
                    {album.title}
                  </h3>
                  <div style={{
                    fontSize: '0.8125rem',
                    color: 'var(--text-muted)',
                    marginBottom: '1rem',
                    display: 'flex',
                    gap: '0.75rem',
                  }}>
                    {album.clientName && <span>{album.clientName}</span>}
                    <span>{album.viewCount || 0} views</span>
                    <span>{new Date(album.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</span>
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Link
                      href={`/albums/${album.id}`}
                      className="btn-accent-outline"
                      style={{ flex: 1, fontSize: '0.75rem', padding: '0.5rem 0.75rem' }}
                    >
                      Manage
                    </Link>
                    {album.slug ? (
                      <a
                        href={`http://localhost:3001/gallery/${album.slug}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn-ghost"
                        style={{ flex: 1, fontSize: '0.75rem', padding: '0.5rem 0.75rem' }}
                      >
                        Preview ↗
                      </a>
                    ) : (
                      <span
                        className="btn-ghost"
                        style={{ flex: 1, fontSize: '0.75rem', padding: '0.5rem 0.75rem', opacity: 0.35, cursor: 'not-allowed' }}
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
  )
}
