import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'PhotoVault — Studio Admin',
  description: 'Enterprise photo gallery platform for photographers',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="grain">{children}</body>
    </html>
  )
}
