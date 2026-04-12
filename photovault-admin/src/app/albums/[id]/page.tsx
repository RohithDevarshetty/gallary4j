'use client'

import { useEffect, useState, useRef, useCallback } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Image from 'next/image'

interface Media {
  id: string
  filename: string
  originalFilename: string
  thumbnailUrl: string
  processingStatus: string
  uploadedAt: string
  folderPath: string | null
}

interface ProcessingJob {
  id: string
  mediaId: string
  filename: string
  mimeType: string
  jobType: string
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'RETRYING'
  attemptCount: number
  maxAttempts: number
  queuedAt: string
  startedAt: string | null
  completedAt: string | null
  errorMessage: string | null
  durationMs: number | null
}

const JOB_STATUS_COLOR: Record<string, string> = {
  QUEUED:     'var(--text-dim)',
  PROCESSING: 'var(--accent)',
  COMPLETED:  '#6abf7b',
  FAILED:     '#e05555',
  RETRYING:   '#e0a455',
}

const JOB_STATUS_ICON: Record<string, string> = {
  QUEUED:     '○',
  PROCESSING: '◌',
  COMPLETED:  '●',
  FAILED:     '✕',
  RETRYING:   '↺',
}

// Group media by folderPath; null/empty → root ''
function groupByFolder(items: Media[]): Record<string, Media[]> {
  const groups: Record<string, Media[]> = {}
  for (const item of items) {
    const key = item.folderPath?.trim() || ''
    if (!groups[key]) groups[key] = []
    groups[key].push(item)
  }
  return groups
}

export default function AlbumDetailPage() {
  const router = useRouter()
  const params = useParams()
  const albumId = params.id as string
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [album, setAlbum] = useState<any>(null)
  const [media, setMedia] = useState<Media[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploadStatus, setUploadStatus] = useState('')

  // Folder modal state
  const [showFolderModal, setShowFolderModal] = useState(false)
  const [pendingFolder, setPendingFolder] = useState('')

  // Delete confirmation
  const [deletingId, setDeletingId] = useState<string | null>(null)

  // Processing job queue
  const [jobs, setJobs] = useState<ProcessingJob[]>([])
  const [showJobPanel, setShowJobPanel] = useState(false)
  const jobPollRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const retryingJobId = useRef<string | null>(null)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) { router.push('/'); return }
    Promise.all([fetchAlbum(token), fetchMedia(token)]).finally(() => setLoading(false))
  }, [albumId])

  // Poll jobs while any are active; stop when all terminal
  const fetchJobs = useCallback(async () => {
    const token = localStorage.getItem('token')
    if (!token) return
    try {
      const res = await fetch(`http://localhost:8080/api/v1/jobs/albums/${albumId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
      })
      if (!res.ok) return
      const data: ProcessingJob[] = await res.json()
      setJobs(data)
      // Stop polling when all jobs are in terminal state
      const active = data.some(j => j.status === 'QUEUED' || j.status === 'PROCESSING' || j.status === 'RETRYING')
      if (!active && jobPollRef.current) {
        clearInterval(jobPollRef.current)
        jobPollRef.current = null
      }
    } catch (e) { /* silent */ }
  }, [albumId])

  const startJobPolling = useCallback(() => {
    if (jobPollRef.current) return
    fetchJobs()
    jobPollRef.current = setInterval(fetchJobs, 3000)
  }, [fetchJobs])

  useEffect(() => () => {
    if (jobPollRef.current) clearInterval(jobPollRef.current)
  }, [])

  const handleRetryJob = async (jobId: string) => {
    const token = localStorage.getItem('token')
    if (!token) return
    retryingJobId.current = jobId
    try {
      await fetch(`http://localhost:8080/api/v1/jobs/${jobId}/retry`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
      })
      startJobPolling()
    } catch (e) { console.error('Retry failed:', e) }
    finally { retryingJobId.current = null }
  }

  const fetchAlbum = async (token: string) => {
    try {
      const res = await fetch(`http://localhost:8080/api/v1/albums/${albumId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
      })
      if (res.ok) setAlbum(await res.json())
    } catch (e) { console.error(e) }
  }

  const fetchMedia = async (token: string) => {
    try {
      const res = await fetch(`http://localhost:8080/api/v1/media/album/${albumId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
      })
      if (res.ok) {
        const data = await res.json()
        setMedia(data.content || [])
      }
    } catch (e) { console.error(e) }
  }

  // Step 1: clicking "Upload" opens the folder modal
  const handleUploadClick = () => {
    setPendingFolder('')
    setShowFolderModal(true)
  }

  // Step 2: folder confirmed → open file picker
  const handleFolderConfirm = () => {
    setShowFolderModal(false)
    fileInputRef.current?.click()
  }

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files || files.length === 0) return

    setUploading(true)
    setUploadProgress(0)
    const token = localStorage.getItem('token')
    const totalFiles = files.length
    let uploadedFiles = 0

    for (const file of Array.from(files)) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('albumId', albumId)
      if (pendingFolder.trim()) formData.append('folderPath', pendingFolder.trim())
      setUploadStatus(`Uploading ${file.name}…`)
      try {
        const res = await fetch('http://localhost:8080/api/v1/media/upload', {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` },
          body: formData,
        })
        if (res.ok) {
          uploadedFiles++
          setUploadProgress(Math.round((uploadedFiles / totalFiles) * 100))
        }
      } catch (e) { console.error('Upload error:', e) }
    }

    setUploading(false)
    setUploadProgress(0)
    setUploadStatus('')
    // reset so same files can be uploaded again with different folder
    e.target.value = ''
    await fetchMedia(token!)
    // Start polling for processing jobs
    setShowJobPanel(true)
    startJobPolling()
  }

  const handleDelete = async (mediaId: string) => {
    const token = localStorage.getItem('token')
    try {
      const res = await fetch(`http://localhost:8080/api/v1/media/${mediaId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` },
      })
      if (res.ok) {
        setMedia(prev => prev.filter(m => m.id !== mediaId))
      }
    } catch (e) { console.error('Delete error:', e) }
    finally { setDeletingId(null) }
  }

  if (loading) {
    return (
      <div style={{ minHeight: '100dvh', background: 'var(--bg)', display: 'flex', flexDirection: 'column' }}>
        <nav className="nav"><div className="nav-inner"><span className="nav-brand">Photo<span>Vault</span></span></div></nav>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
            <div style={{ width: '32px', height: '32px', border: '1px solid var(--border-hi)', borderTopColor: 'var(--accent)', borderRadius: '50%', animation: 'spin 0.7s linear infinite', margin: '0 auto 1rem' }} />
            Loading gallery…
          </div>
        </div>
      </div>
    )
  }

  const groups = groupByFolder(media)
  // Sort folders: root first, then alphabetical
  const folderKeys = Object.keys(groups).sort((a, b) => {
    if (a === '' && b !== '') return -1
    if (b === '' && a !== '') return 1
    return a.localeCompare(b)
  })

  return (
    <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>

      {/* Folder modal */}
      {showFolderModal && (
        <div
          style={{
            position: 'fixed', inset: 0, zIndex: 100,
            background: 'rgba(0,0,0,0.75)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
          onClick={() => setShowFolderModal(false)}
        >
          <div
            style={{
              background: 'var(--card)',
              border: '1px solid var(--border-hi)',
              borderRadius: 'var(--radius)',
              padding: '2rem',
              width: '400px',
              maxWidth: '90vw',
            }}
            onClick={e => e.stopPropagation()}
          >
            <h3 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.4rem', fontWeight: 300, marginBottom: '0.375rem' }}>
              Upload to folder
            </h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
              Optionally organise these photos into a folder. Leave blank to add to the root.
            </p>
            <div className="field" style={{ marginBottom: '1.25rem' }}>
              <label className="label">Folder name</label>
              <input
                className="input"
                type="text"
                placeholder="e.g. Ceremony, Reception, Behind the scenes"
                value={pendingFolder}
                onChange={e => setPendingFolder(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleFolderConfirm()}
                autoFocus
              />
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
              <button className="btn-ghost" onClick={() => setShowFolderModal(false)}>
                Cancel
              </button>
              <button className="btn-accent-outline" onClick={handleFolderConfirm}>
                Choose files →
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      {deletingId && (
        <div
          style={{
            position: 'fixed', inset: 0, zIndex: 100,
            background: 'rgba(0,0,0,0.75)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
          onClick={() => setDeletingId(null)}
        >
          <div
            style={{
              background: 'var(--card)',
              border: '1px solid var(--border-hi)',
              borderRadius: 'var(--radius)',
              padding: '2rem',
              width: '360px',
              maxWidth: '90vw',
              textAlign: 'center',
            }}
            onClick={e => e.stopPropagation()}
          >
            <div style={{ fontFamily: 'var(--font-serif)', fontSize: '2.5rem', color: 'var(--text-dim)', marginBottom: '0.75rem' }}>✦</div>
            <p style={{ fontFamily: 'var(--font-serif)', fontSize: '1.1rem', fontWeight: 300, marginBottom: '0.5rem' }}>
              Remove this photo?
            </p>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
              This cannot be undone.
            </p>
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <button className="btn-ghost" onClick={() => setDeletingId(null)}>Keep it</button>
              <button
                style={{
                  padding: '0.5rem 1.25rem', borderRadius: 'var(--radius)',
                  background: 'rgba(220,60,60,0.12)', border: '1px solid rgba(220,60,60,0.3)',
                  color: '#e05555', fontSize: '0.8125rem', fontWeight: 500, cursor: 'pointer',
                  transition: 'background 0.15s',
                }}
                onClick={() => handleDelete(deletingId)}
              >
                Remove
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Nav */}
      <nav className="nav">
        <div className="nav-inner">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <button className="btn-ghost" onClick={() => router.push('/dashboard')} style={{ padding: '0.5rem 0.875rem' }}>
              ← Back
            </button>
            <span style={{ color: 'var(--border-hi)' }}>|</span>
            <span className="nav-brand">Photo<span>Vault</span></span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {album?.slug ? (
              <a href={`http://localhost:3001/gallery/${album.slug}`} target="_blank" rel="noopener noreferrer" className="btn-ghost">
                Preview ↗
              </a>
            ) : (
              <span className="btn-ghost" style={{ opacity: 0.35, cursor: 'not-allowed' }}>No URL</span>
            )}
            {jobs.length > 0 && (
              <button
                className="btn-ghost"
                style={{ position: 'relative' }}
                onClick={() => { setShowJobPanel(p => !p); if (!showJobPanel) startJobPolling() }}
              >
                Queue
                {jobs.some(j => j.status === 'QUEUED' || j.status === 'PROCESSING' || j.status === 'RETRYING') && (
                  <span style={{
                    position: 'absolute', top: '4px', right: '4px',
                    width: '6px', height: '6px', borderRadius: '50%',
                    background: 'var(--accent)',
                  }} />
                )}
                {jobs.some(j => j.status === 'FAILED') && (
                  <span style={{
                    position: 'absolute', top: '4px', right: '4px',
                    width: '6px', height: '6px', borderRadius: '50%',
                    background: '#e05555',
                  }} />
                )}
              </button>
            )}
            <button className="btn-accent-outline" onClick={handleUploadClick} disabled={uploading}>
              {uploading ? `${uploadProgress}%` : '+ Upload'}
            </button>
            <input ref={fileInputRef} type="file" multiple accept="image/*,video/*" onChange={handleFileUpload} style={{ display: 'none' }} />
          </div>
        </div>
      </nav>

      {/* Upload progress bar */}
      {uploading && (
        <div style={{ position: 'fixed', top: '56px', left: 0, right: 0, zIndex: 99 }}>
          <div className="progress-bar" style={{ borderRadius: 0 }}>
            <div className="progress-fill" style={{ width: `${uploadProgress}%` }} />
          </div>
        </div>
      )}

      <div className="page">

        {/* Processing queue panel */}
        {showJobPanel && jobs.length > 0 && (() => {
          const active   = jobs.filter(j => j.status === 'QUEUED' || j.status === 'PROCESSING' || j.status === 'RETRYING')
          const completed = jobs.filter(j => j.status === 'COMPLETED').length
          const failed   = jobs.filter(j => j.status === 'FAILED')
          const total    = jobs.length
          const pct      = total === 0 ? 0 : Math.round((completed / total) * 100)
          const allDone  = active.length === 0

          return (
            <div
              className="anim-up"
              style={{
                background: 'var(--card)',
                border: '1px solid var(--border-hi)',
                borderRadius: 'var(--radius)',
                padding: '1.25rem 1.5rem',
                marginBottom: '2rem',
              }}
            >
              {/* Panel header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  {!allDone && (
                    <div style={{
                      width: '12px', height: '12px',
                      border: '1.5px solid var(--border-hi)',
                      borderTopColor: 'var(--accent)',
                      borderRadius: '50%',
                      animation: 'spin 0.7s linear infinite',
                      flexShrink: 0,
                    }} />
                  )}
                  <span style={{ fontFamily: 'var(--font-serif)', fontSize: '1.05rem', fontWeight: 300 }}>
                    {allDone ? 'Processing complete' : 'Processing queue'}
                  </span>
                  <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                    {completed}/{total} done
                    {failed.length > 0 && <span style={{ color: '#e05555', marginLeft: '0.5rem' }}>· {failed.length} failed</span>}
                  </span>
                </div>
                <button
                  className="btn-ghost"
                  style={{ fontSize: '0.75rem', padding: '0.25rem 0.75rem' }}
                  onClick={() => setShowJobPanel(false)}
                >
                  Dismiss
                </button>
              </div>

              {/* Progress bar */}
              <div className="progress-bar" style={{ marginBottom: '1rem' }}>
                <div
                  className="progress-fill"
                  style={{
                    width: `${pct}%`,
                    background: allDone && failed.length > 0
                      ? 'linear-gradient(90deg, #6abf7b, #e0a455)'
                      : undefined,
                  }}
                />
              </div>

              {/* Per-file rows */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem', maxHeight: '240px', overflowY: 'auto' }}>
                {jobs.map(job => (
                  <div
                    key={job.id}
                    style={{
                      display: 'flex', alignItems: 'center', gap: '0.75rem',
                      padding: '0.4rem 0.75rem',
                      background: 'var(--bg)',
                      borderRadius: '4px',
                      fontSize: '0.8125rem',
                    }}
                  >
                    {/* Status icon */}
                    <span
                      style={{
                        fontSize: '0.75rem',
                        color: JOB_STATUS_COLOR[job.status] || 'var(--text-dim)',
                        width: '16px', textAlign: 'center', flexShrink: 0,
                        animation: job.status === 'PROCESSING' ? 'pulse 1.4s ease-in-out infinite' : 'none',
                      }}
                    >
                      {JOB_STATUS_ICON[job.status] || '?'}
                    </span>

                    {/* Filename */}
                    <span style={{ flex: 1, color: 'var(--text)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {job.filename || job.mediaId}
                    </span>

                    {/* Type badge */}
                    <span className="badge" style={{ fontSize: '0.6875rem', padding: '0.1rem 0.4rem' }}>
                      {job.jobType}
                    </span>

                    {/* Duration */}
                    {job.durationMs != null && (
                      <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', whiteSpace: 'nowrap' }}>
                        {(job.durationMs / 1000).toFixed(1)}s
                      </span>
                    )}

                    {/* Error + retry */}
                    {job.status === 'FAILED' && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        {job.errorMessage && (
                          <span
                            title={job.errorMessage}
                            style={{ color: '#e05555', fontSize: '0.75rem', maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                          >
                            {job.errorMessage}
                          </span>
                        )}
                        {job.attemptCount < job.maxAttempts && (
                          <button
                            className="btn-ghost"
                            style={{ fontSize: '0.6875rem', padding: '0.15rem 0.5rem' }}
                            onClick={() => handleRetryJob(job.id)}
                          >
                            Retry
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )
        })()}

        {/* Album header */}
        <div className="anim-up" style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '1rem' }}>
            <div>
              <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: 'clamp(1.75rem, 3vw, 2.5rem)', fontWeight: 300, color: 'var(--text)', marginBottom: '0.5rem' }}>
                {album?.title || 'Album'}
              </h1>
              {album?.description && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '0.5rem' }}>{album.description}</p>
              )}
              <div style={{ display: 'flex', gap: '1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                <span>{media.length} items</span>
                {album?.clientName && <span>Client: {album.clientName}</span>}
                <span>{album?.viewCount || 0} views</span>
                {folderKeys.filter(k => k !== '').length > 0 && (
                  <span>{folderKeys.filter(k => k !== '').length} folders</span>
                )}
              </div>
            </div>
            {uploading && (
              <div style={{ padding: '0.75rem 1rem', background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 'var(--radius)', fontSize: '0.8125rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                {uploadStatus || `${uploadProgress}% uploaded`}
              </div>
            )}
          </div>
        </div>

        {/* Empty state */}
        {media.length === 0 ? (
          <div
            className="anim-up d-2 drop-zone"
            onClick={handleUploadClick}
            role="button"
            tabIndex={0}
            onKeyDown={e => e.key === 'Enter' && handleUploadClick()}
          >
            <div style={{ fontFamily: 'var(--font-serif)', fontSize: '3rem', lineHeight: 1, color: 'var(--text-dim)', marginBottom: '1rem' }}>✦</div>
            <p style={{ fontFamily: 'var(--font-serif)', fontSize: '1.5rem', fontWeight: 300, color: 'var(--text-muted)', marginBottom: '0.5rem' }}>No photos yet</p>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '1.5rem' }}>Click here or drag & drop to upload your first photos or videos.</p>
            <span className="badge">JPG · PNG · HEIC · MP4 · MOV</span>
          </div>
        ) : (
          <>
            {/* Upload drop zone strip */}
            <div
              className="anim-up d-1"
              onClick={handleUploadClick}
              style={{
                display: 'flex', alignItems: 'center', gap: '1rem',
                padding: '0.875rem 1.25rem',
                background: 'var(--card)', border: '1px dashed var(--border-hi)',
                borderRadius: 'var(--radius)', marginBottom: '2rem',
                cursor: 'pointer', transition: 'border-color 0.2s, background-color 0.2s',
              }}
              onMouseEnter={e => {
                (e.currentTarget as HTMLElement).style.borderColor = 'var(--accent-dim)'
                ;(e.currentTarget as HTMLElement).style.backgroundColor = 'rgba(200,169,106,0.03)'
              }}
              onMouseLeave={e => {
                (e.currentTarget as HTMLElement).style.borderColor = 'var(--border-hi)'
                ;(e.currentTarget as HTMLElement).style.backgroundColor = 'var(--card)'
              }}
            >
              <span style={{ color: 'var(--accent)', fontSize: '1.1rem' }}>↑</span>
              <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Add more photos or videos</span>
              <span className="badge" style={{ marginLeft: 'auto' }}>Upload</span>
            </div>

            {/* Grouped folders */}
            {folderKeys.map(folderKey => (
              <div key={folderKey} className="anim-in" style={{ marginBottom: '2.5rem' }}>
                {/* Folder header — only show if there are named folders */}
                {(folderKey !== '' || folderKeys.some(k => k !== '')) && (
                  <div style={{
                    display: 'flex', alignItems: 'center', gap: '0.75rem',
                    marginBottom: '1rem',
                  }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--accent)', opacity: 0.7 }}>
                      {folderKey !== '' ? '▸' : '◦'}
                    </span>
                    <span style={{
                      fontFamily: 'var(--font-serif)',
                      fontSize: '1.1rem', fontWeight: 300,
                      color: folderKey !== '' ? 'var(--text)' : 'var(--text-muted)',
                      fontStyle: folderKey === '' ? 'italic' : 'normal',
                    }}>
                      {folderKey !== '' ? folderKey : 'Unsorted'}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                      {groups[folderKey].length} {groups[folderKey].length === 1 ? 'photo' : 'photos'}
                    </span>
                    <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                  </div>
                )}

                {/* Grid */}
                <div className="media-grid">
                  {groups[folderKey].map(item => (
                    <div
                      key={item.id}
                      className="media-thumb"
                      style={{ position: 'relative' }}
                    >
                      {item.thumbnailUrl ? (
                        <Image
                          src={item.thumbnailUrl}
                          alt={item.filename}
                          fill
                          sizes="(max-width: 768px) 50vw, 20vw"
                          style={{ objectFit: 'cover' }}
                        />
                      ) : (
                        <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                          <div style={{
                            width: '20px', height: '20px',
                            border: '1px solid var(--border-hi)',
                            borderTopColor: item.processingStatus === 'PROCESSING' ? 'var(--accent)' : 'var(--border-hi)',
                            borderRadius: '50%',
                            animation: item.processingStatus === 'PROCESSING' ? 'spin 0.7s linear infinite' : 'none',
                          }} />
                          <span style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                            {item.processingStatus === 'PROCESSING' ? 'Processing' : 'Pending'}
                          </span>
                        </div>
                      )}

                      {/* Delete overlay */}
                      <div
                        className="delete-overlay"
                        style={{
                          position: 'absolute', inset: 0,
                          background: 'rgba(0,0,0,0)',
                          transition: 'background 0.2s',
                          display: 'flex', alignItems: 'flex-start', justifyContent: 'flex-end',
                          padding: '6px',
                        }}
                        onMouseEnter={e => (e.currentTarget.style.background = 'rgba(0,0,0,0.35)')}
                        onMouseLeave={e => (e.currentTarget.style.background = 'rgba(0,0,0,0)')}
                      >
                        <button
                          onClick={e => { e.stopPropagation(); setDeletingId(item.id) }}
                          title="Remove photo"
                          style={{
                            width: '24px', height: '24px',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            background: 'rgba(0,0,0,0.6)',
                            border: '1px solid rgba(255,255,255,0.15)',
                            borderRadius: '4px',
                            color: 'rgba(255,255,255,0.7)',
                            fontSize: '0.7rem', cursor: 'pointer',
                            opacity: 0, transition: 'opacity 0.15s',
                          }}
                          className="delete-btn"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </>
        )}
      </div>

      <style>{`
        .media-thumb:hover .delete-btn { opacity: 1 !important; }
        .media-thumb:hover .delete-overlay { background: rgba(0,0,0,0.2) !important; }
      `}</style>
    </div>
  )
}
