'use client'

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Image from 'next/image'

interface Media {
  id: string
  filename: string
  thumbnailUrl: string
  processingStatus: string
  uploadedAt: string
}

export default function AlbumDetailPage() {
  const router = useRouter()
  const params = useParams()
  const albumId = params.id as string

  const [album, setAlbum] = useState<any>(null)
  const [media, setMedia] = useState<Media[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) {
      router.push('/')
      return
    }

    fetchAlbum(token)
    fetchMedia(token)
  }, [albumId])

  const fetchAlbum = async (token: string) => {
    try {
      const response = await fetch(`http://localhost:8080/api/v1/albums/${albumId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
      })
      if (response.ok) {
        const data = await response.json()
        setAlbum(data)
      }
    } catch (error) {
      console.error('Error fetching album:', error)
    } finally {
      setLoading(false)
    }
  }

  const fetchMedia = async (token: string) => {
    try {
      const response = await fetch(`http://localhost:8080/api/v1/media/album/${albumId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
      })
      if (response.ok) {
        const data = await response.json()
        setMedia(data.content || [])
      }
    } catch (error) {
      console.error('Error fetching media:', error)
    }
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

      try {
        const response = await fetch('http://localhost:8080/api/v1/media/upload', {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` },
          body: formData,
        })

        if (response.ok) {
          uploadedFiles++
          setUploadProgress(Math.round((uploadedFiles / totalFiles) * 100))
        }
      } catch (error) {
        console.error('Upload error:', error)
      }
    }

    setUploading(false)
    setUploadProgress(0)
    fetchMedia(token!)
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-500">Loading...</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <button
                onClick={() => router.push('/dashboard')}
                className="text-sm text-gray-600 hover:text-gray-900"
              >
                ← Back to Dashboard
              </button>
            </div>
            <div className="flex items-center">
              <label className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 cursor-pointer">
                Upload Photos/Videos
                <input
                  type="file"
                  multiple
                  accept="image/*,video/*"
                  onChange={handleFileUpload}
                  className="hidden"
                  disabled={uploading}
                />
              </label>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h2 className="text-2xl font-bold text-gray-900">{album?.title}</h2>
          {album?.description && (
            <p className="text-gray-600 mt-2">{album.description}</p>
          )}
          <div className="mt-2 text-sm text-gray-500">
            {media.length} items · {album?.viewCount || 0} views
          </div>
        </div>

        {uploading && (
          <div className="mb-6 bg-white rounded-lg shadow p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">Uploading...</span>
              <span className="text-sm text-gray-500">{uploadProgress}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-600 h-2 rounded-full transition-all"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
          </div>
        )}

        {media.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow">
            <p className="text-gray-500 mb-4">No media yet</p>
            <label className="inline-block px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 cursor-pointer">
              Upload Your First Photo/Video
              <input
                type="file"
                multiple
                accept="image/*,video/*"
                onChange={handleFileUpload}
                className="hidden"
              />
            </label>
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {media.map((item) => (
              <div key={item.id} className="relative aspect-square bg-gray-200 rounded-lg overflow-hidden">
                {item.thumbnailUrl ? (
                  <Image
                    src={item.thumbnailUrl}
                    alt={item.filename}
                    fill
                    className="object-cover"
                  />
                ) : (
                  <div className="flex items-center justify-center h-full">
                    <p className="text-sm text-gray-500">
                      {item.processingStatus === 'PROCESSING' ? 'Processing...' : 'Pending'}
                    </p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
