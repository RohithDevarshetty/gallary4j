'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import Image from 'next/image'

interface Media {
  id: string
  filename: string
  thumbnailUrl: string
  previewUrl: string
  originalUrl: string
  width: number
  height: number
}

interface Album {
  id: string
  title: string
  description: string
  mediaCount: number
  allowDownloads: boolean
}

export default function GalleryPage() {
  const params = useParams()
  const slug = params.slug as string

  const [album, setAlbum] = useState<Album | null>(null)
  const [media, setMedia] = useState<Media[]>([])
  const [selectedMedia, setSelectedMedia] = useState<Media | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchAlbum()
    fetchMedia()
  }, [slug])

  const fetchAlbum = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/v1/albums/slug/${slug}`)
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

  const fetchMedia = async () => {
    try {
      const albumResponse = await fetch(`http://localhost:8080/api/v1/albums/slug/${slug}`)
      if (albumResponse.ok) {
        const albumData = await albumResponse.json()

        const mediaResponse = await fetch(
          `http://localhost:8080/api/v1/media/album/${albumData.id}`
        )
        if (mediaResponse.ok) {
          const data = await mediaResponse.json()
          setMedia(data.content || [])
        }
      }
    } catch (error) {
      console.error('Error fetching media:', error)
    }
  }

  const handleDownload = async (mediaItem: Media) => {
    try {
      // Track download
      await fetch(`http://localhost:8080/api/v1/media/${mediaItem.id}/download`, {
        method: 'POST',
      })

      // Download file
      window.open(mediaItem.originalUrl, '_blank')
    } catch (error) {
      console.error('Download error:', error)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">Loading gallery...</p>
      </div>
    )
  }

  if (!album) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">Gallery not found</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <h1 className="text-3xl font-bold text-gray-900">{album.title}</h1>
          {album.description && (
            <p className="mt-2 text-gray-600">{album.description}</p>
          )}
          <p className="mt-2 text-sm text-gray-500">{album.mediaCount} photos</p>
        </div>
      </header>

      {/* Gallery Grid */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {media.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500">No photos available yet</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {media.map((item) => (
              <div
                key={item.id}
                className="relative aspect-square bg-gray-200 rounded-lg overflow-hidden cursor-pointer hover:opacity-90 transition-opacity"
                onClick={() => setSelectedMedia(item)}
              >
                {item.thumbnailUrl && (
                  <Image
                    src={item.thumbnailUrl}
                    alt={item.filename}
                    fill
                    className="object-cover"
                  />
                )}
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Lightbox */}
      {selectedMedia && (
        <div
          className="fixed inset-0 bg-black bg-opacity-90 flex items-center justify-center z-50 p-4"
          onClick={() => setSelectedMedia(null)}
        >
          <div className="relative max-w-7xl max-h-full" onClick={(e) => e.stopPropagation()}>
            <button
              className="absolute top-4 right-4 text-white text-2xl hover:text-gray-300 z-10"
              onClick={() => setSelectedMedia(null)}
            >
              ✕
            </button>

            {selectedMedia.previewUrl && (
              <img
                src={selectedMedia.previewUrl}
                alt={selectedMedia.filename}
                className="max-w-full max-h-screen object-contain"
              />
            )}

            {album.allowDownloads && (
              <button
                className="absolute bottom-4 right-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                onClick={() => handleDownload(selectedMedia)}
              >
                Download
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
