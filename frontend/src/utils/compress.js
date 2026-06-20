import imageCompression from 'browser-image-compression'

const ALLOWED_IMAGE_TYPES = [
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/gif',
  'image/webp',
  'image/bmp',
]

const ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp']

const MAX_FILE_SIZE = 10 * 1024 * 1024

export function validateImageFile(file) {
  if (!file) {
    return { valid: false, message: '未选择文件' }
  }

  const originalName = file.name || ''
  const lowerName = originalName.toLowerCase()
  const hasValidExt = ALLOWED_EXTENSIONS.some((ext) => lowerName.endsWith(ext))
  if (!hasValidExt) {
    return {
      valid: false,
      message: `不支持的文件扩展名，仅允许：${ALLOWED_EXTENSIONS.join('、')}`,
    }
  }

  if (file.type && !ALLOWED_IMAGE_TYPES.includes(file.type.toLowerCase())) {
    return {
      valid: false,
      message: `不支持的文件类型，仅允许上传图片文件`,
    }
  }

  if (file.size > MAX_FILE_SIZE) {
    return {
      valid: false,
      message: `文件大小超过限制（最大 ${MAX_FILE_SIZE / 1024 / 1024}MB）`,
    }
  }

  return { valid: true }
}

export async function compressImage(file, options = {}) {
  const validation = validateImageFile(file)
  if (!validation.valid) {
    throw new Error(validation.message)
  }
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
    const validation = validateImageFile(file)
    if (!validation.valid) {
      return Promise.reject(new Error(validation.message))
    }
    if (file.type.startsWith('image/')) {
      return compressImage(file, options)
    }
    return Promise.resolve(file)
  })
  return Promise.all(promises)
}
