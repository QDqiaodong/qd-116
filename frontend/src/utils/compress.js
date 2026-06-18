import imageCompression from 'browser-image-compression'

export async function compressImage(file, options = {}) {
  const defaultOptions = {
    maxSizeMB: 0.5,
    maxWidthOrHeight: 800,
    useWebWorker: true,
    initialQuality: 0.7,
  }
  const opts = { ...defaultOptions, ...options }
  try {
    const compressedFile = await imageCompression(file, opts)
    return compressedFile
  } catch (error) {
    console.error('图片压缩失败:', error)
    return file
  }
}

export async function batchCompressImages(files, options = {}) {
  const promises = Array.from(files).map((file) => {
    if (file.type.startsWith('image/')) {
      return compressImage(file, options)
    }
    return Promise.resolve(file)
  })
  return Promise.all(promises)
}
