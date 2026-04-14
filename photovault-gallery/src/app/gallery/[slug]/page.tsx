'use client'

import { useEffect, useState, useCallback, useRef } from 'react'
import { useParams } from 'next/navigation'
import Image from 'next/image'

interface Media {
  id: string
  filename: string
  thumbnailUrl: string | null
  previewUrl: string | null
  originalUrl: string
  optimizedUrl: string | null
  width: number
  height: number
  folderPath: string | null
  mimeType: string | null
  videoThumbnailUrl: string | null
  videoDurationSeconds: number | null
}

interface Album {
  id: string
  title: string
  description: string
  mediaCount: number
  allowDownloads: boolean
  clientName: string
  requiresPassword: boolean
}

function groupByFolder(items: Media[]): Record<string, Media[]> {
  const groups: Record<string, Media[]> = {}
  for (const item of items) {
    const key = item.folderPath?.trim() || ''
    if (!groups[key]) groups[key] = []
    groups[key].push(item)
  }
  return groups
}

const isVideo = (item: Media) => !!item.mimeType?.startsWith('video/')

const formatDuration = (secs: number) => {
  const total = Math.round(secs)
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

export default function GalleryPage() {
  const params = useParams()
  const slug = params.slug as string

  const [album, setAlbum] = useState<Album | null>(null)
  const [media, setMedia] = useState<Media[]>([])
  // selectedIndex is a global index into the flat `media` array
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null)
  const [activeFolder, setActiveFolder] = useState<string | null>(null) // null = show all
  const [loading, setLoading] = useState(true)
  const [imgLoaded, setImgLoaded] = useState(false)
  const [rotation, setRotation] = useState(0)
  const videoRef = useRef<HTMLVideoElement>(null)

  // Password gate
  const [needsPassword, setNeedsPassword] = useState(false)
  const [passwordInput, setPasswordInput] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [verifying, setVerifying] = useState(false)

  const selectedMedia = selectedIndex !== null ? media[selectedIndex] : null

  const loadMedia = async (albumId: string) => {
    const mediaRes = await fetch(`http://localhost:8080/api/v1/media/album/${albumId}`)
    if (mediaRes.ok) {
      const mediaData = await mediaRes.json()
      setMedia(mediaData.content || [])
    }
  }

  useEffect(() => {
    const load = async () => {
      try {
        const albumRes = await fetch(`http://localhost:8080/api/v1/albums/slug/${slug}`)
        if (!albumRes.ok) { setLoading(false); return }
        const albumData = await albumRes.json()
        setAlbum(albumData)

        if (albumData.requiresPassword) {
          const stored = sessionStorage.getItem(`pv_access_${slug}`)
          if (stored === '1') {
            await loadMedia(albumData.id)
          } else {
            setNeedsPassword(true)
          }
        } else {
          await loadMedia(albumData.id)
        }
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [slug])

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setPasswordError('')
    setVerifying(true)
    try {
      const res = await fetch(`http://localhost:8080/api/v1/albums/slug/${slug}/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: passwordInput }),
      })
      if (res.ok) {
        sessionStorage.setItem(`pv_access_${slug}`, '1')
        setNeedsPassword(false)
        if (album) await loadMedia(album.id)
      } else {
        setPasswordError('Incorrect password. Please try again.')
      }
    } catch (e) {
      setPasswordError('Something went wrong. Please try again.')
    } finally {
      setVerifying(false)
    }
  }

  const openAt = (index: number) => {
    setSelectedIndex(index)
    setImgLoaded(false)
    setRotation(0)
  }

  const close = () => setSelectedIndex(null)

  const rotate = useCallback(() => {
    setRotation(r => (r + 90) % 360)
  }, [])

  const prev = useCallback(() => {
    if (selectedIndex === null) return
    videoRef.current?.pause()
    setImgLoaded(false)
    setRotation(0)
    setSelectedIndex((selectedIndex - 1 + media.length) % media.length)
  }, [selectedIndex, media.length])

  const next = useCallback(() => {
    if (selectedIndex === null) return
    videoRef.current?.pause()
    setImgLoaded(false)
    setRotation(0)
    setSelectedIndex((selectedIndex + 1) % media.length)
  }, [selectedIndex, media.length])

  useEffect(() => {
    if (selectedIndex === null) return
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft')  { e.preventDefault(); prev() }
      if (e.key === 'ArrowRight') { e.preventDefault(); next() }
      if (e.key === 'Escape')     { e.preventDefault(); close() }
      if (e.key === 'r' || e.key === 'R') { e.preventDefault(); rotate() }
      if (e.key === ' ') {
        e.preventDefault()
        const v = videoRef.current
        if (v) { v.paused ? v.play() : v.pause() }
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [selectedIndex, prev, next, rotate])

  const handleDownload = async (item: Media) => {
    try {
      await fetch(`http://localhost:8080/api/v1/media/${item.id}/download`, { method: 'POST' })
      window.open(item.originalUrl, '_blank')
    } catch (e) {
      console.error(e)
    }
  }

  // ── Loading ───────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div style={{ minHeight: '100dvh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg)' }}>
        <div style={{ width: '32px', height: '32px', border: '1px solid var(--border-hi)', borderTopColor: 'var(--accent)', borderRadius: '50%', animation: 'spin 0.7s linear infinite' }} />
      </div>
    )
  }

  // ── Password gate ─────────────────────────────────────────────────────────
  if (needsPassword && album) {
    return (
      <div style={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: 'var(--bg)', padding: '2rem' }}>
        {/* Decorative light */}
        <div style={{ position: 'fixed', top: '-200px', left: '50%', transform: 'translateX(-50%)', width: '600px', height: '600px', borderRadius: '50%', background: 'radial-gradient(circle, rgba(200,169,106,0.07) 0%, transparent 70%)', pointerEvents: 'none' }} />

        <div style={{ width: '100%', maxWidth: '400px', background: 'var(--card)', border: '1px solid var(--border-hi)', borderRadius: 'var(--radius-lg)', padding: '2.5rem', position: 'relative', overflow: 'hidden' }}>
          {/* Top edge shimmer */}
          <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '1px', background: 'linear-gradient(to right, transparent, rgba(200,169,106,0.5), transparent)' }} />

          {/* Lock icon */}
          <div style={{ marginBottom: '1.5rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: 'rgba(200,169,106,0.1)', border: '1px solid rgba(200,169,106,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.25rem' }}>
              🔒
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontFamily: 'var(--font-serif)', fontSize: '1.5rem', fontWeight: 300, color: 'var(--text)', marginBottom: '0.25rem' }}>
                {album.title}
              </div>
              {album.clientName && (
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>{album.clientName}</div>
              )}
            </div>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-dim)', textAlign: 'center' }}>
              This gallery is password protected.
            </p>
          </div>

          <form onSubmit={handlePasswordSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {passwordError && (
              <div style={{ padding: '0.625rem 0.875rem', background: 'rgba(224,85,85,0.1)', border: '1px solid rgba(224,85,85,0.25)', borderRadius: 'var(--radius-sm)', fontSize: '0.8125rem', color: '#e05555' }}>
                {passwordError}
              </div>
            )}
            <input
              type="password"
              placeholder="Enter password"
              value={passwordInput}
              onChange={e => setPasswordInput(e.target.value)}
              autoFocus
              style={{
                width: '100%', boxSizing: 'border-box',
                padding: '0.75rem 1rem',
                background: 'var(--bg)',
                border: '1px solid var(--border-hi)',
                borderRadius: 'var(--radius-sm)',
                color: 'var(--text)',
                fontSize: '0.9375rem',
                fontFamily: 'var(--font-sans)',
                outline: 'none',
              }}
              onFocus={e => (e.target.style.borderColor = 'var(--accent-dim)')}
              onBlur={e => (e.target.style.borderColor = 'var(--border-hi)')}
            />
            <button
              type="submit"
              disabled={verifying || !passwordInput}
              style={{
                padding: '0.75rem',
                background: verifying || !passwordInput ? 'rgba(200,169,106,0.2)' : 'linear-gradient(135deg, var(--accent) 0%, var(--accent-hi) 100%)',
                border: '1px solid rgba(200,169,106,0.4)',
                borderRadius: 'var(--radius-sm)',
                color: verifying || !passwordInput ? 'var(--accent-dim)' : '#0a0a0b',
                fontSize: '0.875rem',
                fontWeight: 600,
                letterSpacing: '0.04em',
                cursor: verifying || !passwordInput ? 'default' : 'pointer',
                fontFamily: 'var(--font-sans)',
                transition: 'all 0.2s',
              }}
            >
              {verifying ? 'Verifying…' : 'View Gallery →'}
            </button>
          </form>
        </div>

        {/* Brand */}
        <div style={{ marginTop: '2rem', fontSize: '0.75rem', color: 'var(--text-dim)', letterSpacing: '0.06em' }}>
          <span style={{ fontStyle: 'italic' }}>Photo</span>Vault
        </div>
      </div>
    )
  }

  // ── Not found ─────────────────────────────────────────────────────────────
  if (!album) {
    return (
      <div style={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '1rem', background: 'var(--bg)' }}>
        <div style={{ fontFamily: 'var(--font-serif)', fontSize: '4rem', color: 'var(--text-dim)', lineHeight: 1 }}>✦</div>
        <p style={{ fontFamily: 'var(--font-serif)', fontSize: '1.5rem', fontWeight: 300, color: 'var(--text-muted)' }}>Gallery not found</p>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-dim)' }}>This link may have expired or the gallery doesn&apos;t exist.</p>
      </div>
    )
  }

  const groups = groupByFolder(media)
  const folderKeys = Object.keys(groups).sort((a, b) => {
    if (a === '' && b !== '') return -1
    if (b === '' && a !== '') return 1
    return a.localeCompare(b)
  })
  const hasNamedFolders = folderKeys.some(k => k !== '')

  // Which media to display in the grid (filtered by activeFolder)
  const displayedMedia = activeFolder === null
    ? media
    : (groups[activeFolder] ?? [])

  // ── Gallery ───────────────────────────────────────────────────────────────
  return (
    <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>

      {/* Header */}
      <header className="gallery-header">
        <div style={{
          maxWidth: '1440px', margin: '0 auto', width: '100%',
          padding: '0 2.5rem',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <span style={{ fontFamily: 'var(--font-serif)', fontSize: '1.15rem', fontStyle: 'italic', color: 'var(--text)' }}>
            Photo<span style={{ color: 'var(--accent)' }}>Vault</span>
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span style={{ fontSize: '0.6875rem', fontWeight: 500, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--text-muted)' }}>
              {media.length} {media.length === 1 ? 'item' : 'items'}
            </span>
            {album.allowDownloads && media.length > 0 && (
              <a
                href={`http://localhost:8080/api/v1/media/album/${album.id}/zip`}
                download
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: '0.4rem',
                  padding: '0.35rem 0.875rem',
                  background: 'rgba(200,169,106,0.1)',
                  border: '1px solid rgba(200,169,106,0.25)',
                  borderRadius: '4px',
                  color: 'var(--accent)',
                  fontSize: '0.75rem', fontWeight: 500,
                  letterSpacing: '0.06em', textTransform: 'uppercase',
                  textDecoration: 'none',
                  transition: 'background 0.2s, border-color 0.2s',
                }}
                onMouseOver={e => {
                  (e.currentTarget as HTMLElement).style.background = 'rgba(200,169,106,0.18)'
                  ;(e.currentTarget as HTMLElement).style.borderColor = 'rgba(200,169,106,0.45)'
                }}
                onMouseOut={e => {
                  (e.currentTarget as HTMLElement).style.background = 'rgba(200,169,106,0.1)'
                  ;(e.currentTarget as HTMLElement).style.borderColor = 'rgba(200,169,106,0.25)'
                }}
              >
                ↓ Download all
              </a>
            )}
          </div>
        </div>
      </header>

      {/* Hero — album title */}
      <section className="gallery-hero anim-up">
        <h1 className="gallery-hero-title">{album.title}</h1>
        {album.description && (
          <p style={{ color: 'var(--text-muted)', fontSize: '1.0625rem', maxWidth: '560px', lineHeight: 1.65, marginTop: '1rem' }}>
            {album.description}
          </p>
        )}
        {album.clientName && (
          <div className="gallery-hero-meta" style={{ marginTop: '1.5rem' }}>
            {album.clientName}
          </div>
        )}
      </section>

      {/* Folder tabs — only shown if there are named folders */}
      {hasNamedFolders && (
        <div className="folder-tabs anim-up d-1">
          <button
            onClick={() => setActiveFolder(null)}
            className={`folder-tab${activeFolder === null ? ' active' : ''}`}
          >
            All ({media.length})
          </button>
          {folderKeys.filter(k => k !== '').map(folder => (
            <button
              key={folder}
              onClick={() => setActiveFolder(folder)}
              className={`folder-tab${activeFolder === folder ? ' active' : ''}`}
            >
              {folder} ({groups[folder].length})
            </button>
          ))}
          {groups[''] && (
            <button
              onClick={() => setActiveFolder('')}
              className={`folder-tab${activeFolder === '' ? ' active' : ''}`}
              style={{ fontStyle: 'italic' }}
            >
              Other ({groups[''].length})
            </button>
          )}
        </div>
      )}

      {/* Photo grid */}
      <main style={{ paddingBottom: '6rem' }}>
        {media.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '5rem 0', color: 'var(--text-muted)' }}>
            <div style={{ fontFamily: 'var(--font-serif)', fontSize: '3rem', color: 'var(--text-dim)', marginBottom: '1rem' }}>✦</div>
            <p>No photos in this gallery yet.</p>
          </div>
        ) : activeFolder !== null ? (
          // Single folder view
          <div className="photo-grid">
            {displayedMedia.map(item => {
              const globalIndex = media.indexOf(item)
              return (
                <div
                  key={item.id}
                  className="photo-cell anim-in"
                  onClick={() => openAt(globalIndex)}
                  style={{ animationDelay: `${Math.min(globalIndex * 0.02, 0.35)}s` }}
                >
                  <Image
                    src={item.thumbnailUrl || item.videoThumbnailUrl || item.originalUrl}
                    alt={item.filename}
                    fill
                    sizes="(max-width: 768px) 50vw, 25vw"
                    style={{ objectFit: 'cover' }}
                  />
                  <div className="photo-cell-overlay" />
                  {isVideo(item) && (
                    <>
                      <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'none' }}>
                        <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(4px)', border: '1.5px solid rgba(255,255,255,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <span style={{ color: '#fff', fontSize: '1rem', marginLeft: 3 }}>▶</span>
                        </div>
                      </div>
                      {item.videoDurationSeconds != null && (
                        <span style={{ position: 'absolute', bottom: 6, right: 6, fontSize: '0.6875rem', fontWeight: 500, color: '#fff', background: 'rgba(0,0,0,0.65)', borderRadius: 3, padding: '1px 5px', pointerEvents: 'none' }}>
                          {formatDuration(item.videoDurationSeconds)}
                        </span>
                      )}
                    </>
                  )}
                  <span
                    className="photo-index"
                    style={{
                      position: 'absolute', bottom: '0.75rem', left: '0.875rem',
                      fontSize: '0.6875rem', color: 'rgba(255,255,255,0.7)',
                      fontWeight: 500, pointerEvents: 'none',
                    }}
                  >
                    {displayedMedia.indexOf(item) + 1} / {displayedMedia.length}
                  </span>
                </div>
              )
            })}
          </div>
        ) : (
          // All folders view — grouped sections
          <>
            {folderKeys.map(folderKey => (
              <div key={folderKey} style={{ marginBottom: '4rem', paddingTop: '0.5rem' }}>
                {hasNamedFolders && (
                  <div className="folder-section-header">
                    <h2 className="folder-section-title" style={folderKey === '' ? { fontStyle: 'italic', color: 'var(--text-muted)' } : undefined}>
                      {folderKey !== '' ? folderKey : 'Unsorted'}
                    </h2>
                    <span className="folder-section-count">{groups[folderKey].length}</span>
                    <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                    <button
                      onClick={() => setActiveFolder(folderKey)}
                      style={{
                        fontSize: '0.75rem', color: 'var(--accent)', background: 'none',
                        border: 'none', cursor: 'pointer', opacity: 0.7,
                        transition: 'opacity 0.15s', fontFamily: 'var(--font-sans)',
                      }}
                      onMouseOver={e => (e.currentTarget.style.opacity = '1')}
                      onMouseOut={e => (e.currentTarget.style.opacity = '0.7')}
                    >
                      View all →
                    </button>
                  </div>
                )}
                <div className="photo-grid">
                  {groups[folderKey].map(item => {
                    const globalIndex = media.indexOf(item)
                    return (
                      <div
                        key={item.id}
                        className="photo-cell anim-in"
                        onClick={() => openAt(globalIndex)}
                        style={{ animationDelay: `${Math.min(globalIndex * 0.02, 0.35)}s` }}
                      >
                        <Image
                          src={item.thumbnailUrl || item.videoThumbnailUrl || item.originalUrl}
                          alt={item.filename}
                          fill
                          sizes="(max-width: 768px) 50vw, 25vw"
                          style={{ objectFit: 'cover' }}
                        />
                        <div className="photo-cell-overlay" />
                        {isVideo(item) && (
                          <>
                            <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'none' }}>
                              <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(4px)', border: '1.5px solid rgba(255,255,255,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <span style={{ color: '#fff', fontSize: '1rem', marginLeft: 3 }}>▶</span>
                              </div>
                            </div>
                            {item.videoDurationSeconds != null && (
                              <span style={{ position: 'absolute', bottom: 6, right: 6, fontSize: '0.6875rem', fontWeight: 500, color: '#fff', background: 'rgba(0,0,0,0.65)', borderRadius: 3, padding: '1px 5px', pointerEvents: 'none' }}>
                                {formatDuration(item.videoDurationSeconds)}
                              </span>
                            )}
                          </>
                        )}
                        <span
                          className="photo-index"
                          style={{
                            position: 'absolute', bottom: '0.75rem', left: '0.875rem',
                            fontSize: '0.6875rem', color: 'rgba(255,255,255,0.7)',
                            fontWeight: 500, pointerEvents: 'none',
                          }}
                        >
                          {globalIndex + 1} / {media.length}
                        </span>
                      </div>
                    )
                  })}
                </div>
              </div>
            ))}
          </>
        )}
      </main>

      {/* ── Lightbox ──────────────────────────────────────────────────────── */}
      {selectedMedia && selectedIndex !== null && (
        <div
          className="anim-in"
          style={{ position: 'fixed', inset: 0, zIndex: 50, background: 'rgba(4,4,5,0.98)', display: 'flex', flexDirection: 'column' }}
          onClick={close}
        >
          {/* Top bar */}
          <div
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 1.5rem', height: '54px', borderBottom: '1px solid rgba(255,255,255,0.06)', flexShrink: 0 }}
            onClick={e => e.stopPropagation()}
          >
            <span style={{ fontSize: '0.8125rem', color: 'rgba(255,255,255,0.4)', fontWeight: 500, letterSpacing: '0.06em' }}>
              {selectedIndex + 1} <span style={{ opacity: 0.4 }}>/</span> {media.length}
              {selectedMedia.folderPath && (
                <span style={{ marginLeft: '0.75rem', fontSize: '0.75rem', color: 'var(--accent)', opacity: 0.7 }}>
                  {selectedMedia.folderPath}
                </span>
              )}
            </span>
            <span style={{ fontSize: '0.8125rem', color: 'rgba(255,255,255,0.35)', maxWidth: '40%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {selectedMedia.filename}
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                onClick={e => { e.stopPropagation(); rotate() }}
                title="Rotate (R)"
                style={{ width: '32px', height: '32px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '4px', color: 'rgba(255,255,255,0.6)', fontSize: '0.95rem', cursor: 'pointer', transition: 'background 0.15s, color 0.15s, border-color 0.15s' }}
                onMouseOver={e => { e.currentTarget.style.background = 'rgba(200,169,106,0.15)'; e.currentTarget.style.borderColor = 'rgba(200,169,106,0.35)'; e.currentTarget.style.color = 'var(--accent)' }}
                onMouseOut={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.05)'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)'; e.currentTarget.style.color = 'rgba(255,255,255,0.6)' }}
              >
                ⟳
              </button>
              {album.allowDownloads && (
                <button
                  onClick={e => { e.stopPropagation(); handleDownload(selectedMedia) }}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.45rem 0.875rem', background: 'rgba(200,169,106,0.12)', border: '1px solid rgba(200,169,106,0.3)', borderRadius: '4px', color: 'var(--accent)', fontSize: '0.75rem', fontWeight: 500, letterSpacing: '0.06em', textTransform: 'uppercase', cursor: 'pointer', fontFamily: 'var(--font-sans)', transition: 'background 0.2s, border-color 0.2s' }}
                  onMouseOver={e => { e.currentTarget.style.background = 'rgba(200,169,106,0.2)'; e.currentTarget.style.borderColor = 'rgba(200,169,106,0.5)' }}
                  onMouseOut={e => { e.currentTarget.style.background = 'rgba(200,169,106,0.12)'; e.currentTarget.style.borderColor = 'rgba(200,169,106,0.3)' }}
                >
                  ↓ Download
                </button>
              )}
              <button
                onClick={e => { e.stopPropagation(); close() }}
                style={{ width: '32px', height: '32px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '4px', color: 'rgba(255,255,255,0.5)', fontSize: '1rem', cursor: 'pointer', transition: 'background 0.15s, color 0.15s', fontFamily: 'monospace' }}
                onMouseOver={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.1)'; e.currentTarget.style.color = '#fff' }}
                onMouseOut={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.05)'; e.currentTarget.style.color = 'rgba(255,255,255,0.5)' }}
                title="Close (Esc)"
              >
                ✕
              </button>
            </div>
          </div>

          {/* Image area */}
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', position: 'relative', overflow: 'hidden' }} onClick={close}>
            {media.length > 1 && (() => {
              const prevItem = media[(selectedIndex - 1 + media.length) % media.length]
              return (
                <div
                  onClick={e => { e.stopPropagation(); prev() }}
                  title="Previous (←)"
                  style={{ position: 'absolute', left: 0, top: 0, bottom: '0', width: '12vw', maxWidth: '180px', minWidth: '90px', display: 'flex', alignItems: 'center', justifyContent: 'flex-start', paddingLeft: '0.75rem', cursor: 'pointer', zIndex: 5 }}
                  onMouseOver={e => { const el = e.currentTarget.querySelector('.peek-img') as HTMLElement | null; if (el) { el.style.opacity = '0.85'; el.style.transform = 'translateX(6px)' } }}
                  onMouseOut={e => { const el = e.currentTarget.querySelector('.peek-img') as HTMLElement | null; if (el) { el.style.opacity = '0.45'; el.style.transform = 'translateX(0)' } }}
                >
                  <div className="peek-img" style={{ position: 'relative', width: '100%', height: '60%', borderRadius: '6px', overflow: 'hidden', border: '1px solid rgba(255,255,255,0.1)', opacity: 0.45, transition: 'opacity 0.2s ease, transform 0.2s ease', boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}>
                    <Image src={prevItem.thumbnailUrl || prevItem.videoThumbnailUrl || prevItem.originalUrl} alt={prevItem.filename} fill sizes="180px" style={{ objectFit: 'cover' }} />
                    <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to right, rgba(0,0,0,0.55), rgba(0,0,0,0.15))' }} />
                    <div style={{ position: 'absolute', top: '50%', left: '0.5rem', transform: 'translateY(-50%)', width: '32px', height: '32px', borderRadius: '50%', background: 'rgba(0,0,0,0.55)', border: '1px solid rgba(255,255,255,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '0.95rem', backdropFilter: 'blur(6px)' }}>←</div>
                  </div>
                </div>
              )
            })()}
            <div style={{ flex: 1, height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem 14vw' }} onClick={e => e.stopPropagation()}>
              {isVideo(selectedMedia) ? (
                <video
                  ref={videoRef}
                  key={selectedMedia.id}
                  src={selectedMedia.optimizedUrl || selectedMedia.originalUrl}
                  autoPlay
                  muted
                  controls
                  playsInline
                  style={{ maxWidth: '100%', maxHeight: '100%', outline: 'none', borderRadius: '2px' }}
                />
              ) : (
                <>
                  {!imgLoaded && (
                    <div style={{ position: 'absolute', width: '24px', height: '24px', border: '1px solid rgba(255,255,255,0.1)', borderTopColor: 'var(--accent)', borderRadius: '50%', animation: 'spin 0.7s linear infinite' }} />
                  )}
                  <img
                    key={selectedMedia.id}
                    src={selectedMedia.previewUrl || selectedMedia.optimizedUrl || selectedMedia.thumbnailUrl || selectedMedia.originalUrl}
                    alt={selectedMedia.filename}
                    onLoad={() => setImgLoaded(true)}
                    style={{ maxWidth: rotation % 180 === 0 ? '100%' : '100vh', maxHeight: rotation % 180 === 0 ? '100%' : '100vw', objectFit: 'contain', opacity: imgLoaded ? 1 : 0, transition: 'opacity 0.25s ease, transform 0.3s ease', transform: `rotate(${rotation}deg)`, userSelect: 'none', ...({ WebkitUserDrag: 'none' } as any) }}
                  />
                </>
              )}
            </div>
            {media.length > 1 && (() => {
              const nextItem = media[(selectedIndex + 1) % media.length]
              return (
                <div
                  onClick={e => { e.stopPropagation(); next() }}
                  title="Next (→)"
                  style={{ position: 'absolute', right: 0, top: 0, bottom: '0', width: '12vw', maxWidth: '180px', minWidth: '90px', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', paddingRight: '0.75rem', cursor: 'pointer', zIndex: 5 }}
                  onMouseOver={e => { const el = e.currentTarget.querySelector('.peek-img') as HTMLElement | null; if (el) { el.style.opacity = '0.85'; el.style.transform = 'translateX(-6px)' } }}
                  onMouseOut={e => { const el = e.currentTarget.querySelector('.peek-img') as HTMLElement | null; if (el) { el.style.opacity = '0.45'; el.style.transform = 'translateX(0)' } }}
                >
                  <div className="peek-img" style={{ position: 'relative', width: '100%', height: '60%', borderRadius: '6px', overflow: 'hidden', border: '1px solid rgba(255,255,255,0.1)', opacity: 0.45, transition: 'opacity 0.2s ease, transform 0.2s ease', boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}>
                    <Image src={nextItem.thumbnailUrl || nextItem.videoThumbnailUrl || nextItem.originalUrl} alt={nextItem.filename} fill sizes="180px" style={{ objectFit: 'cover' }} />
                    <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to left, rgba(0,0,0,0.55), rgba(0,0,0,0.15))' }} />
                    <div style={{ position: 'absolute', top: '50%', right: '0.5rem', transform: 'translateY(-50%)', width: '32px', height: '32px', borderRadius: '50%', background: 'rgba(0,0,0,0.55)', border: '1px solid rgba(255,255,255,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '0.95rem', backdropFilter: 'blur(6px)' }}>→</div>
                  </div>
                </div>
              )
            })()}
          </div>

          {/* Filmstrip */}
          {media.length > 1 && (
            <div
              style={{ height: '76px', flexShrink: 0, borderTop: '1px solid rgba(255,255,255,0.06)', display: 'flex', alignItems: 'center', gap: '3px', padding: '0 1rem', overflowX: 'auto' }}
              onClick={e => e.stopPropagation()}
            >
              {media.map((item, i) => (
                <div
                  key={item.id}
                  onClick={() => openAt(i)}
                  style={{ position: 'relative', width: '54px', height: '54px', flexShrink: 0, borderRadius: '3px', overflow: 'hidden', cursor: 'pointer', border: i === selectedIndex ? '1.5px solid var(--accent)' : '1.5px solid transparent', opacity: i === selectedIndex ? 1 : 0.45, transition: 'opacity 0.15s, border-color 0.15s' }}
                  onMouseOver={e => { if (i !== selectedIndex) (e.currentTarget as HTMLElement).style.opacity = '0.8' }}
                  onMouseOut={e => { if (i !== selectedIndex) (e.currentTarget as HTMLElement).style.opacity = '0.45' }}
                >
                  <Image
                    src={item.thumbnailUrl || item.videoThumbnailUrl || item.originalUrl}
                    alt={item.filename}
                    fill
                    sizes="54px"
                    style={{ objectFit: 'cover' }}
                  />
                  {isVideo(item) && (
                    <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'none' }}>
                      <span style={{ color: 'rgba(255,255,255,0.85)', fontSize: '0.7rem', textShadow: '0 1px 3px rgba(0,0,0,0.7)' }}>▶</span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
