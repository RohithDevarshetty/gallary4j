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
}

export default function DashboardPage() {
  const router = useRouter()
  const [albums, setAlbums] = useState<Album[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) {
      router.push('/')
      return
    }

    fetchAlbums(token)
  }, [])

  const fetchAlbums = async (token: string) => {
    try {
      const response = await fetch('http://localhost:8080/api/v1/albums', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch albums')
      }

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

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold text-gray-900">PhotoVault</h1>
            </div>
            <div className="flex items-center space-x-4">
              <Link
                href="/albums/create"
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Create Album
              </Link>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-gray-700 hover:text-gray-900"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">My Albums</h2>

        {loading ? (
          <div className="text-center py-12">
            <p className="text-gray-500">Loading albums...</p>
          </div>
        ) : albums.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow">
            <p className="text-gray-500 mb-4">No albums yet</p>
            <Link
              href="/albums/create"
              className="inline-block px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Create Your First Album
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {albums.map((album) => (
              <div
                key={album.id}
                className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow"
              >
                <div className="p-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    {album.title}
                  </h3>
                  <div className="text-sm text-gray-500 space-y-1">
                    <p>{album.mediaCount} photos</p>
                    <p>{album.viewCount} views</p>
                    <p className="text-xs">
                      Created: {new Date(album.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="mt-4 flex space-x-2">
                    <Link
                      href={`/albums/${album.id}`}
                      className="flex-1 text-center px-3 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
                    >
                      View
                    </Link>
                    <a
                      href={`http://localhost:3001/gallery/${album.slug}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex-1 text-center px-3 py-2 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300"
                    >
                      Share
                    </a>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
