export default function HomePage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">PhotoVault Gallery</h1>
        <p className="text-gray-600">
          Enter your gallery URL to view your photos
        </p>
        <p className="text-sm text-gray-500 mt-4">
          Format: /gallery/[album-slug]
        </p>
      </div>
    </div>
  )
}
