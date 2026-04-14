'use client'
import { useRouter, usePathname } from 'next/navigation'
import Link from 'next/link'
import BrickLogo from './BrickLogo'

interface AdminShellProps {
  children: React.ReactNode
}

export default function AdminShell({ children }: AdminShellProps) {
  const router = useRouter()
  const pathname = usePathname()

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('photographerId')
    router.push('/')
  }

  return (
    <div style={{ display: 'flex' }}>
      <aside className="sidebar">
        {/* Brand */}
        <div className="sidebar-brand">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <BrickLogo size={22} />
            <span style={{
              fontFamily: 'var(--font-brand)',
              fontWeight: 800,
              fontSize: '1.05rem',
              letterSpacing: '0.2em',
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

        {/* Nav */}
        <nav className="sidebar-nav">
          <Link href="/dashboard" className={`sidebar-item${pathname === '/dashboard' ? ' active' : ''}`}>
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <rect x="1" y="1" width="5" height="5" rx="1" fill="currentColor" opacity="0.8"/>
              <rect x="8" y="1" width="5" height="5" rx="1" fill="currentColor" opacity="0.8"/>
              <rect x="1" y="8" width="5" height="5" rx="1" fill="currentColor" opacity="0.8"/>
              <rect x="8" y="8" width="5" height="5" rx="1" fill="currentColor" opacity="0.4"/>
            </svg>
            Dashboard
          </Link>
          <Link href="/albums/create" className="sidebar-item" style={{ marginTop: '0.25rem' }}>
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <rect x="1" y="1" width="12" height="12" rx="2" stroke="currentColor" strokeWidth="1.2" opacity="0.7"/>
              <path d="M7 4.5V9.5M4.5 7H9.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
            </svg>
            New Album
          </Link>
        </nav>

        {/* Footer */}
        <div className="sidebar-footer">
          <button className="sidebar-item" onClick={handleLogout} style={{ color: 'var(--text-dim)' }}>
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M5 2H2.5C1.95 2 1.5 2.45 1.5 3v8c0 .55.45 1 1 1H5M9.5 10L12 7l-2.5-3M5.5 7H12" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Sign out
          </button>
        </div>
      </aside>

      <div className="shell-content" style={{ flex: 1 }}>
        {children}
      </div>
    </div>
  )
}
