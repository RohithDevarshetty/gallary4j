'use client'

import { useEffect, useState, useCallback } from 'react'
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
}

interface Album {
  id: string
  title: string
  description: string
  mediaCount: number
  allowDownloads: boolean
  clientName: string
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

  const selectedMedia = selectedIndex !== null ? media[selectedIndex] : null

  useEffect(() => {
    const load = async () => {
      try {
        const albumRes = await fetch(`http://localhost:8080/api/v1/albums/slug/${slug}`)
        if (!albumRes.ok) { setLoading(false); return }
        const albumData = await albumRes.json()
        setAlbum(albumData)

        const mediaRes = await fetch(`http://localhost:8080/api/v1/media/album/${albumData.id}`)
        if (mediaRes.ok) {
          const mediaData = await mediaRes.json()
          setMedia(mediaData.content || [])
        }
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [slug])

  const openAt = (index: number) => {
    setSelectedIndex(index)
    setImgLoaded(false)
  }

  const close = () => setSelectedIndex(null)

  const prev = useCallback(() => {
    if (selectedIndex === null) return
    setImgLoaded(false)
    setSelectedIndex((selectedIndex - 1 + media.length) % media.length)
  }, [selectedIndex, media.length])

  const next = useCallback(() => {
    if (selectedIndex === null) return
    setImgLoaded(false)
    setSelectedIndex((selectedIndex + 1) % media.length)
  }, [selectedIndex, media.length])

  useEffect(() => {
    if (selectedIndex === null) return
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft')  { e.preventDefault(); prev() }
      if (e.key === 'ArrowRight') { e.preventDefault(); next() }
      if (e.key === 'Escape')     { e.preventDefault(); close() }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [selectedIndex, prev, next])

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
        <div style={{ width: '28px', height: '28px', border: '1px solid var(--border-hi)', borderTopColor: 'var(--accent)', borderRadius: '50%', animation: 'spin 0.7s linear infinite' }} />
      </div>
    )
  }

  // ── Not found ─────────────────────────────────────────────────────────────
  if (!album) {
    return (
      <div style={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '1rem', background: 'var(--bg)' }}>
        <div style={{ fontFamily: 'var(--font-serif)', fontSize: '4rem', color: 'var(--text-dim)', lineHeight: 1 }}>✦</div>
        <p style={{ fontFamily: 'var(--font-serif)', fontSize: '1.5rem', fontWeight: 300, color: 'var(--text-muted)' }}>Gallery not found</p>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-dim)' }}>This link may have expired or the gallery doesn't exist.</p>
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
      <header style={{
        borderBottom: '1px solid var(--border)',
        background: 'rgba(10,10,10,0.9)',
        backdropFilter: 'blur(12px)',
        position: 'sticky', top: 0, zIndex: 10,
      }}>
        <div style={{
          maxWidth: '1400px', margin: '0 auto',
          padding: '0 2rem', height: '56px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
            <span style={{ fontFamily: 'var(--font-serif)', fontSize: '1.2rem', fontStyle: 'italic', color: 'var(--text)' }}>
              Photo<span style={{ color: 'var(--accent)' }}>Vault</span>
            </span>
            <span style={{ color: 'var(--border-hi)' }}>|</span>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>{album.title}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span style={{ fontSize: '0.6875rem', fontWeight: 500, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--text-muted)' }}>
              {media.length} {media.length === 1 ? 'photo' : 'photos'}
            </span>
            {album.allowDownloads && media.length > 0 && (
              <a
                href={`http://localhost:8080/api/v1/media/album/${album.id}/zip`}
                download
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: '0.4rem',
                  padding: '0.4rem 0.875rem',
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

      {/* Album title */}
      <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '3rem 2rem 2rem' }}>
        <div className="anim-up">
          <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: 'clamp(2rem, 4vw, 3.5rem)', fontWeight: 300, lineHeight: 1.1, color: 'var(--text)', marginBottom: '0.5rem' }}>
            {album.title}
          </h1>
          {album.description && (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem', maxWidth: '600px' }}>{album.description}</p>
          )}
        </div>

        {/* Folder tabs — only shown if there are named folders */}
        {hasNamedFolders && (
          <div className="anim-up" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '1.75rem' }}>
            <button
              onClick={() => setActiveFolder(null)}
              style={{
                padding: '0.35rem 0.875rem',
                borderRadius: '99px',
                border: '1px solid',
                borderColor: activeFolder === null ? 'var(--accent)' : 'var(--border-hi)',
                background: activeFolder === null ? 'rgba(200,169,106,0.12)' : 'transparent',
                color: activeFolder === null ? 'var(--accent)' : 'var(--text-muted)',
                fontSize: '0.8125rem', cursor: 'pointer',
                transition: 'all 0.15s',
              }}
            >
              All ({media.length})
            </button>
            {folderKeys.filter(k => k !== '').map(folder => (
              <button
                key={folder}
                onClick={() => setActiveFolder(folder)}
                style={{
                  padding: '0.35rem 0.875rem',
                  borderRadius: '99px',
                  border: '1px solid',
                  borderColor: activeFolder === folder ? 'var(--accent)' : 'var(--border-hi)',
                  background: activeFolder === folder ? 'rgba(200,169,106,0.12)' : 'transparent',
                  color: activeFolder === folder ? 'var(--accent)' : 'var(--text-muted)',
                  fontSize: '0.8125rem', cursor: 'pointer',
                  transition: 'all 0.15s',
                }}
              >
                {folder} ({groups[folder].length})
              </button>
            ))}
            {groups[''] && (
              <button
                onClick={() => setActiveFolder('')}
                style={{
                  padding: '0.35rem 0.875rem',
                  borderRadius: '99px',
                  border: '1px solid',
                  borderColor: activeFolder === '' ? 'var(--accent)' : 'var(--border)',
                  background: activeFolder === '' ? 'rgba(200,169,106,0.08)' : 'transparent',
                  color: activeFolder === '' ? 'var(--accent)' : 'var(--text-dim)',
                  fontSize: '0.8125rem', cursor: 'pointer', fontStyle: 'italic',
                  transition: 'all 0.15s',
                }}
              >
                Other ({groups[''].length})
              </button>
            )}
          </div>
        )}
      </div>

      {/* Photo grid */}
      <main style={{ maxWidth: '1400px', margin: '0 auto', padding: '0 2rem 4rem' }}>
        {media.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '5rem 0', color: 'var(--text-muted)' }}>
            <div style={{ fontFamily: 'var(--font-serif)', fontSize: '3rem', color: 'var(--text-dim)', marginBottom: '1rem' }}>✦</div>
            <p>No photos in this gallery yet.</p>
          </div>
        ) : activeFolder !== null ? (
          // Single folder view
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '3px' }}>
            {displayedMedia.map(item => {
              const globalIndex = media.indexOf(item)
              return (
                <div
                  key={item.id}
                  className="anim-in"
                  onClick={() => openAt(globalIndex)}
                  style={{ position: 'relative', aspectRatio: '1', background: 'var(--card)', cursor: 'pointer', overflow: 'hidden' }}
                >
                  <Image
                    src={item.thumbnailUrl || item.originalUrl}
                    alt={item.filename}
                    fill
                    sizes="(max-width: 768px) 50vw, 25vw"
                    style={{ objectFit: 'cover', transition: 'transform 0.4s ease' }}
                    onMouseOver={e => (e.currentTarget.style.transform = 'scale(1.04)')}
                    onMouseOut={e => (e.currentTarget.style.transform = 'scale(1)')}
                  />
                  <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)', opacity: 0, transition: 'opacity 0.2s', display: 'flex', alignItems: 'flex-end', padding: '0.75rem' }}
                    onMouseOver={e => (e.currentTarget.style.opacity = '1')}
                    onMouseOut={e => (e.currentTarget.style.opacity = '0')}
                  >
                    <span style={{ fontSize: '0.6875rem', color: 'rgba(255,255,255,0.7)', fontWeight: 500 }}>
                      {displayedMedia.indexOf(item) + 1} / {displayedMedia.length}
                    </span>
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          // All folders view — grouped sections
          <>
            {folderKeys.map(folderKey => (
              <div key={folderKey} style={{ marginBottom: '3rem' }}>
                {hasNamedFolders && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                    <h2 style={{
                      fontFamily: 'var(--font-serif)', fontWeight: 300,
                      fontSize: '1.25rem',
                      color: folderKey !== '' ? 'var(--text)' : 'var(--text-muted)',
                      fontStyle: folderKey === '' ? 'italic' : 'normal',
                    }}>
                      {folderKey !== '' ? folderKey : 'Unsorted'}
                    </h2>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                      {groups[folderKey].length}
                    </span>
                    <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                    <button
                      onClick={() => setActiveFolder(folderKey)}
                      style={{
                        fontSize: '0.75rem', color: 'var(--accent)', background: 'none',
                        border: 'none', cursor: 'pointer', opacity: 0.7,
                        transition: 'opacity 0.15s',
                      }}
                      onMouseOver={e => (e.currentTarget.style.opacity = '1')}
                      onMouseOut={e => (e.currentTarget.style.opacity = '0.7')}
                    >
                      View all →
                    </button>
                  </div>
                )}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '3px' }}>
                  {groups[folderKey].map(item => {
                    const globalIndex = media.indexOf(item)
                    return (
                      <div
                        key={item.id}
                        className="anim-in"
                        onClick={() => openAt(globalIndex)}
                        style={{
                          position: 'relative', aspectRatio: '1',
                          background: 'var(--card)', cursor: 'pointer', overflow: 'hidden',
                          animationDelay: `${Math.min(globalIndex * 0.03, 0.4)}s`,
                        }}
                      >
                        <Image
                          src={item.thumbnailUrl || item.originalUrl}
                          alt={item.filename}
                          fill
                          sizes="(max-width: 768px) 50vw, 25vw"
                          style={{ objectFit: 'cover', transition: 'transform 0.4s ease' }}
                          onMouseOver={e => (e.currentTarget.style.transform = 'scale(1.04)')}
                          onMouseOut={e => (e.currentTarget.style.transform = 'scale(1)')}
                        />
                        <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)', opacity: 0, transition: 'opacity 0.2s', display: 'flex', alignItems: 'flex-end', padding: '0.75rem' }}
                          onMouseOver={e => (e.currentTarget.style.opacity = '1')}
                          onMouseOut={e => (e.currentTarget.style.opacity = '0')}
                        >
                          <span style={{ fontSize: '0.6875rem', color: 'rgba(255,255,255,0.7)', fontWeight: 500 }}>
                            {globalIndex + 1} / {media.length}
                          </span>
                        </div>
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
          style={{ position: 'fixed', inset: 0, zIndex: 50, background: 'rgba(5,5,5,0.97)', display: 'flex', flexDirection: 'column' }}
          onClick={close}
        >
          {/* Top bar */}
          <div
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 1.5rem', height: '52px', borderBottom: '1px solid rgba(255,255,255,0.06)', flexShrink: 0 }}
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
            {media.length > 1 && (
              <button onClick={e => { e.stopPropagation(); prev() }} title="Previous (←)"
                style={{ position: 'absolute', left: '1.25rem', zIndex: 10, width: '44px', height: '44px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '50%', color: 'rgba(255,255,255,0.7)', fontSize: '1.1rem', cursor: 'pointer', transition: 'background 0.15s, color 0.15s, transform 0.15s', backdropFilter: 'blur(8px)' }}
                onMouseOver={e => { e.currentTarget.style.background = 'rgba(200,169,106,0.15)'; e.currentTarget.style.borderColor = 'rgba(200,169,106,0.35)'; e.currentTarget.style.color = 'var(--accent)'; e.currentTarget.style.transform = 'scale(1.08)' }}
                onMouseOut={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.06)'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)'; e.currentTarget.style.color = 'rgba(255,255,255,0.7)'; e.currentTarget.style.transform = 'scale(1)' }}
              >←</button>
            )}
            <div style={{ flex: 1, height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem 4.5rem' }} onClick={e => e.stopPropagation()}>
              {!imgLoaded && (
                <div style={{ position: 'absolute', width: '24px', height: '24px', border: '1px solid rgba(255,255,255,0.1)', borderTopColor: 'var(--accent)', borderRadius: '50%', animation: 'spin 0.7s linear infinite' }} />
              )}
              <img
                key={selectedMedia.id}
                src={selectedMedia.previewUrl || selectedMedia.optimizedUrl || selectedMedia.thumbnailUrl || selectedMedia.originalUrl}
                alt={selectedMedia.filename}
                onLoad={() => setImgLoaded(true)}
                style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain', opacity: imgLoaded ? 1 : 0, transition: 'opacity 0.25s ease', userSelect: 'none', WebkitUserDrag: 'none' as any }}
              />
            </div>
            {media.length > 1 && (
              <button onClick={e => { e.stopPropagation(); next() }} title="Next (→)"
                style={{ position: 'absolute', right: '1.25rem', zIndex: 10, width: '44px', height: '44px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '50%', color: 'rgba(255,255,255,0.7)', fontSize: '1.1rem', cursor: 'pointer', transition: 'background 0.15s, color 0.15s, transform 0.15s', backdropFilter: 'blur(8px)' }}
                onMouseOver={e => { e.currentTarget.style.background = 'rgba(200,169,106,0.15)'; e.currentTarget.style.borderColor = 'rgba(200,169,106,0.35)'; e.currentTarget.style.color = 'var(--accent)'; e.currentTarget.style.transform = 'scale(1.08)' }}
                onMouseOut={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.06)'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)'; e.currentTarget.style.color = 'rgba(255,255,255,0.7)'; e.currentTarget.style.transform = 'scale(1)' }}
              >→</button>
            )}
          </div>

          {/* Filmstrip */}
          {media.length > 1 && (
            <div
              style={{ height: '72px', flexShrink: 0, borderTop: '1px solid rgba(255,255,255,0.06)', display: 'flex', alignItems: 'center', gap: '3px', padding: '0 1rem', overflowX: 'auto' }}
              onClick={e => e.stopPropagation()}
            >
              {media.map((item, i) => (
                <div
                  key={item.id}
                  onClick={() => openAt(i)}
                  style={{ position: 'relative', width: '52px', height: '52px', flexShrink: 0, borderRadius: '3px', overflow: 'hidden', cursor: 'pointer', border: i === selectedIndex ? '1.5px solid var(--accent)' : '1.5px solid transparent', opacity: i === selectedIndex ? 1 : 0.45, transition: 'opacity 0.15s, border-color 0.15s' }}
                  onMouseOver={e => { if (i !== selectedIndex) (e.currentTarget as HTMLElement).style.opacity = '0.8' }}
                  onMouseOut={e => { if (i !== selectedIndex) (e.currentTarget as HTMLElement).style.opacity = '0.45' }}
                >
                  <Image
                    src={item.thumbnailUrl || item.originalUrl}
                    alt={item.filename}
                    fill
                    sizes="52px"
                    style={{ objectFit: 'cover' }}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
