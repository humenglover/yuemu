// 预设的一些高质量图片，避免每次都请求API
const DEFAULT_IMAGES = [
  'https://images.pexels.com/photos/1714208/pexels-photo-1714208.jpeg',
  'https://images.pexels.com/photos/1762851/pexels-photo-1762851.jpeg',
  'https://images.pexels.com/photos/3052361/pexels-photo-3052361.jpeg',
  'https://images.pexels.com/photos/1762851/pexels-photo-1762851.jpeg',
  'https://images.pexels.com/photos/1762851/pexels-photo-1762851.jpeg'
]

const PEXELS_API_KEY = 'your_pexels_api_key'

// 获取随机封面图
export const getRandomCoverImage = () => {
  const randomIndex = Math.floor(Math.random() * DEFAULT_IMAGES.length)
  return DEFAULT_IMAGES[randomIndex]
}

// 获取帖子封面图
export const getPostCoverImage = (post: API.Post) => {
  // 如果帖子有附件图片，使用第一张附件图片
  if (post?.attachments?.[0]?.url) {
    return post.attachments[0].url
  }
  // 否则返回随机封面图
  return getRandomCoverImage()
}

export const getRandomPexelsImage = async () => {
  try {
    const response = await fetch(
      'https://api.pexels.com/v1/search?query=landscape&per_page=1&page=' + Math.floor(Math.random() * 100),
      {
        headers: {
          Authorization: PEXELS_API_KEY
        }
      }
    )
    const data = await response.json()
    return data.photos[0].src.large
  } catch (error) {
    console.error('Failed to fetch Pexels image:', error)
    return getRandomCoverImage() // 失败时使用预设图片
  }
}
