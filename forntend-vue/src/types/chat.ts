export interface ChatMessage {
  id?: string
  type: number
  content: string
  pictureId: string
  senderId: string
  status: number
  replyId: string | null
  rootId: string | null
  sender?: {
    id: string
    userName: string
    userAvatar: string
  }
  replyMessage?: {
    id: string
    content: string
    sender: {
      id: string
      userName: string
      userAvatar: string
    }
  }
  createTime?: string
}
