import { message } from 'ant-design-vue'

interface CompressOptions {
  maxWidth?: number
  maxHeight?: number
  quality?: number
  maxSize?: number
  format?: 'webp' | 'jpeg' | 'auto'
}

export class ImageCompressor {
  private static readonly DEFAULT_OPTIONS: Required<CompressOptions> = {
    maxWidth: 1920,
    maxHeight: 1080,
    quality: 0.85,
    maxSize: 1024 * 1024, // 1MB
    format: 'auto'
  }

  /**
   * 检查浏览器是否支持 WebP 格式
   */
  private static isWebPSupported(): boolean {
    const canvas = document.createElement('canvas')
    if (canvas.toDataURL) {
      return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0
    }
    return false
  }

  /**
   * 压缩单个图片文件
   * @param file 原始文件
   * @param options 压缩选项
   * @returns Promise<File>
   */
  public static async compress(file: File, options?: CompressOptions): Promise<File> {
    const finalOptions = { ...this.DEFAULT_OPTIONS, ...options }

    try {
      // 如果文件小于最大尺寸限制，直接返回
      if (file.size <= finalOptions.maxSize) {
        return file
      }

      return await new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.readAsDataURL(file)

        reader.onload = (e) => {
          const img = new Image()
          img.src = e.target?.result as string

          img.onload = () => {
            const canvas = document.createElement('canvas')
            let { width, height } = img

            // 计算缩放比例
            const aspectRatio = width / height
            if (width > finalOptions.maxWidth || height > finalOptions.maxHeight) {
              if (aspectRatio > 1) {
                width = finalOptions.maxWidth
                height = Math.round(width / aspectRatio)
              } else {
                height = finalOptions.maxHeight
                width = Math.round(height * aspectRatio)
              }
            }

            canvas.width = width
            canvas.height = height

            const ctx = canvas.getContext('2d', { alpha: false })
            if (!ctx) {
              reject(new Error('无法创建canvas上下文'))
              return
            }

            // 优化渲染质量
            ctx.imageSmoothingEnabled = true
            ctx.imageSmoothingQuality = 'high'
            ctx.drawImage(img, 0, 0, width, height)

            // 确定输出格式
            let outputFormat = finalOptions.format
            if (outputFormat === 'auto') {
              outputFormat = this.isWebPSupported() ? 'webp' : 'jpeg'
            }

            // 递归压缩，直到文件大小合适或达到最低质量
            const compressWithQuality = (currentQuality: number): Promise<Blob> => {
              return new Promise((resolveBlob) => {
                canvas.toBlob(
                  (blob) => {
                    if (!blob) {
                      reject(new Error('图片压缩失败'))
                      return
                    }

                    if (blob.size <= finalOptions.maxSize || currentQuality <= 0.5) {
                      resolveBlob(blob)
                      return
                    }

                    // 继续压缩
                    compressWithQuality(currentQuality - 0.1)
                      .then(resolveBlob)
                      .catch(reject)
                  },
                  `image/${outputFormat}`,
                  currentQuality
                )
              })
            }

            // 开始压缩
            compressWithQuality(finalOptions.quality)
              .then((finalBlob) => {
                // 生成新文件名
                const extension = outputFormat === 'webp' ? '.webp' : '.jpg'
                const newFileName = file.name.replace(/\.[^/.]+$/, '') + extension

                const compressedFile = new File([finalBlob], newFileName, {
                  type: `image/${outputFormat}`,
                  lastModified: Date.now()
                })

                resolve(compressedFile)
              })
              .catch(reject)
          }

          img.onerror = () => reject(new Error('图片加载失败'))
        }

        reader.onerror = () => reject(new Error('文件读取失败'))
      })
    } catch (error) {
      console.error('图片压缩失败:', error)
      message.error('图片压缩失败')
      throw error
    }
  }

  /**
   * 批量压缩图片文件
   * @param files 文件数组
   * @param options 压缩选项
   * @returns Promise<File[]>
   */
  public static async compressAll(files: File[], options?: CompressOptions): Promise<File[]> {
    try {
      const compressPromises = files.map(file => {
        if (file.type.startsWith('image/')) {
          return this.compress(file, options)
        }
        return Promise.resolve(file)
      })

      return await Promise.all(compressPromises)
    } catch (error) {
      console.error('批量压缩失败:', error)
      message.error('批量压缩失败')
      throw error
    }
  }
} 