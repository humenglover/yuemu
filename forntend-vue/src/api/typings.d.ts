declare namespace API {
  type Activity = {
    activityExpired?: boolean
    commentCount?: number
    content?: string
    coverUrl?: string
    createTime?: string
    expireTime?: string
    id?: number
    isDelete?: number
    isExpired?: number
    isLiked?: number
    isShared?: number
    likeCount?: number
    reviewMessage?: string
    shareCount?: number
    spaceId?: number
    status?: number
    title?: string
    updateTime?: string
    user?: UserVO
    userId?: number
    viewCount?: number
  }

  type ActivityAddRequest = {
    content?: string
    coverUrl?: string
    expireTime?: string
    spaceId?: number
    title?: string
  }

  type ActivityQueryRequest = {
    current?: number
    isPublic?: boolean
    notExpired?: boolean
    pageSize?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    spaceId?: number
    status?: number
  }

  type addCategoryUsingPOSTParams = {
    /** categoryName */
    categoryName: string
    /** type */
    type: number
  }

  type AddMessage = {
    content?: string
    ip?: string
  }

  type addTagUsingPOSTParams = {
    /** tagName */
    tagName?: string
  }

  type addTimeAlbumUsingPOSTParams = {
    /** loveBoardId */
    loveBoardId: number
  }

  type AiChat = {
    content?: string
    createTime?: string
    id?: number
    isDeleted?: number
    role?: string
    sessionId?: number
    userId?: number
  }

  type AiChatVO = {
    content?: string
    createTime?: string
    role?: string
  }

  type aiTagUsingPOSTParams = {
    /** pictureId */
    pictureId: number
  }

  type AppVersion = {
    apkPath?: string
    apkSize?: number
    createTime?: string
    description?: string
    id?: number
    isDelete?: number
    isForce?: number
    status?: number
    updateTime?: string
    version?: string
    versionCode?: number
  }

  type AreaChartData = {
    series?: MapStringObject_[]
    xaxisData?: string[]
  }

  type AudioFile = {
    album?: string
    artist?: string
    coverUrl?: string
    createTime?: string
    description?: string
    duration?: number
    fileName?: string
    filePath?: string
    fileSize?: number
    fileUrl?: string
    genre?: string
    id?: number
    isDelete?: number
    likeCount?: number
    md5?: string
    mimeType?: string
    spaceId?: number
    title?: string
    updateTime?: string
    userId?: number
    viewCount?: number
  }

  type AudioFileVO = {
    album?: string
    artist?: string
    coverUrl?: string
    createTime?: string
    description?: string
    duration?: number
    fileName?: string
    fileSize?: number
    fileUrl?: string
    genre?: string
    id?: number
    isLiked?: number
    likeCount?: number
    mimeType?: string
    title?: string
    updateTime?: string
    user?: UserVO
    userId?: number
    viewCount?: number
  }

  type BaseResponseActivity_ = {
    code?: number
    data?: Activity
    message?: string
  }

  type BaseResponseAudioFileVO_ = {
    code?: number
    data?: AudioFileVO
    message?: string
  }

  type BaseResponseBoolean_ = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseBugReportVO_ = {
    code?: number
    data?: BugReportVO
    message?: string
  }

  type BaseResponseChartVO_ = {
    code?: number
    data?: ChartVO
    message?: string
  }

  type BaseResponseChatMessage_ = {
    code?: number
    data?: ChatMessage
    message?: string
  }

  type BaseResponseComments_ = {
    code?: number
    data?: Comments
    message?: string
  }

  type BaseResponseCreateOutPaintingTaskResponse_ = {
    code?: number
    data?: CreateOutPaintingTaskResponse
    message?: string
  }

  type BaseResponseDashboardVO_ = {
    code?: number
    data?: DashboardVO
    message?: string
  }

  type BaseResponseFollowersAndFansVO_ = {
    code?: number
    data?: FollowersAndFansVO
    message?: string
  }

  type BaseResponseFriendLink_ = {
    code?: number
    data?: FriendLink
    message?: string
  }

  type BaseResponseGame2048Record_ = {
    code?: number
    data?: Game2048Record
    message?: string
  }

  type BaseResponseGetOutPaintingTaskResponse_ = {
    code?: number
    data?: GetOutPaintingTaskResponse
    message?: string
  }

  type BaseResponseInt_ = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseIPageRagMessageVO_ = {
    code?: number
    data?: IPageRagMessageVO_
    message?: string
  }

  type BaseResponseIPageRagSessionVO_ = {
    code?: number
    data?: IPageRagSessionVO_
    message?: string
  }

  type BaseResponseKnowledgeFile_ = {
    code?: number
    data?: KnowledgeFile
    message?: string
  }

  type BaseResponseKnowledgeFileVO_ = {
    code?: number
    data?: KnowledgeFileVO
    message?: string
  }

  type BaseResponseLikeRecord_ = {
    code?: number
    data?: LikeRecord
    message?: string
  }

  type BaseResponseListAudioFileVO_ = {
    code?: number
    data?: AudioFileVO[]
    message?: string
  }

  type BaseResponseListCategoryVO_ = {
    code?: number
    data?: CategoryVO[]
    message?: string
  }

  type BaseResponseListCommentsVO_ = {
    code?: number
    data?: CommentsVO[]
    message?: string
  }

  type BaseResponseListFavoriteRecordVO_ = {
    code?: number
    data?: FavoriteRecordVO[]
    message?: string
  }

  type BaseResponseListFriendLink_ = {
    code?: number
    data?: FriendLink[]
    message?: string
  }

  type BaseResponseListGame2048RecordVO_ = {
    code?: number
    data?: Game2048RecordVO[]
    message?: string
  }

  type BaseResponseListHotSearchVO_ = {
    code?: number
    data?: HotSearchVO[]
    message?: string
  }

  type BaseResponseListImageSearchResult_ = {
    code?: number
    data?: ImageSearchResult[]
    message?: string
  }

  type BaseResponseListInt_ = {
    code?: number
    data?: number[]
    message?: string
  }

  type BaseResponseListLikeRecordVO_ = {
    code?: number
    data?: LikeRecordVO[]
    message?: string
  }

  type BaseResponseListMapStringString_ = {
    code?: number
    data?: MapStringString_[]
    message?: string
  }

  type BaseResponseListMessageVO_ = {
    code?: number
    data?: MessageVO[]
    message?: string
  }

  type BaseResponseListPicture_ = {
    code?: number
    data?: Picture[]
    message?: string
  }

  type BaseResponseListPictureVO_ = {
    code?: number
    data?: PictureVO[]
    message?: string
  }

  type BaseResponseListPostVO_ = {
    code?: number
    data?: PostVO[]
    message?: string
  }

  type BaseResponseListRagMessageVO_ = {
    code?: number
    data?: RagMessageVO[]
    message?: string
  }

  type BaseResponseListShareRecordVO_ = {
    code?: number
    data?: ShareRecordVO[]
    message?: string
  }

  type BaseResponseListSnakeGameRecord_ = {
    code?: number
    data?: SnakeGameRecord[]
    message?: string
  }

  type BaseResponseListSpace_ = {
    code?: number
    data?: Space[]
    message?: string
  }

  type BaseResponseListSpaceCategoryAnalyzeResponse_ = {
    code?: number
    data?: SpaceCategoryAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceLevel_ = {
    code?: number
    data?: SpaceLevel[]
    message?: string
  }

  type BaseResponseListSpaceSizeAnalyzeResponse_ = {
    code?: number
    data?: SpaceSizeAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceTagAnalyzeResponse_ = {
    code?: number
    data?: SpaceTagAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceUserAnalyzeResponse_ = {
    code?: number
    data?: SpaceUserAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceUserVO_ = {
    code?: number
    data?: SpaceUserVO[]
    message?: string
  }

  type BaseResponseListSpaceVO_ = {
    code?: number
    data?: SpaceVO[]
    message?: string
  }

  type BaseResponseListString_ = {
    code?: number
    data?: string[]
    message?: string
  }

  type BaseResponseListTagVO_ = {
    code?: number
    data?: TagVO[]
    message?: string
  }

  type BaseResponseListUserSearchRecord_ = {
    code?: number
    data?: UserSearchRecord[]
    message?: string
  }

  type BaseResponseLoginUserVO_ = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong_ = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseLoveBoard_ = {
    code?: number
    data?: LoveBoard
    message?: string
  }

  type BaseResponseLoveBoardMusicAlbum_ = {
    code?: number
    data?: LoveBoardMusicAlbum
    message?: string
  }

  type BaseResponseMapStringObject_ = {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  type BaseResponseMapStringString_ = {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  type BaseResponseMessage_ = {
    code?: number
    data?: Message
    message?: string
  }

  type BaseResponseMessageCenterVO_ = {
    code?: number
    data?: MessageCenterVO
    message?: string
  }

  type BaseResponsePageActivity_ = {
    code?: number
    data?: PageActivity_
    message?: string
  }

  type BaseResponsePageAiChat_ = {
    code?: number
    data?: PageAiChat_
    message?: string
  }

  type BaseResponsePageAppVersion_ = {
    code?: number
    data?: PageAppVersion_
    message?: string
  }

  type BaseResponsePageAudioFile_ = {
    code?: number
    data?: PageAudioFile_
    message?: string
  }

  type BaseResponsePageAudioFileVO_ = {
    code?: number
    data?: PageAudioFileVO_
    message?: string
  }

  type BaseResponsePageBugReportVO_ = {
    code?: number
    data?: PageBugReportVO_
    message?: string
  }

  type BaseResponsePageCategoryVO_ = {
    code?: number
    data?: PageCategoryVO_
    message?: string
  }

  type BaseResponsePageChatMessage_ = {
    code?: number
    data?: PageChatMessage_
    message?: string
  }

  type BaseResponsePageComments_ = {
    code?: number
    data?: PageComments_
    message?: string
  }

  type BaseResponsePageCommentsVO_ = {
    code?: number
    data?: PageCommentsVO_
    message?: string
  }

  type BaseResponsePageFavoriteRecordVO_ = {
    code?: number
    data?: PageFavoriteRecordVO_
    message?: string
  }

  type BaseResponsePageFriendLink_ = {
    code?: number
    data?: PageFriendLink_
    message?: string
  }

  type BaseResponsePageKnowledgeFileVO_ = {
    code?: number
    data?: PageKnowledgeFileVO_
    message?: string
  }

  type BaseResponsePageLikeRecord_ = {
    code?: number
    data?: PageLikeRecord_
    message?: string
  }

  type BaseResponsePageLikeRecordVO_ = {
    code?: number
    data?: PageLikeRecordVO_
    message?: string
  }

  type BaseResponsePageLoveBoard_ = {
    code?: number
    data?: PageLoveBoard_
    message?: string
  }

  type BaseResponsePageLoveBoardMusicAlbum_ = {
    code?: number
    data?: PageLoveBoardMusicAlbum_
    message?: string
  }

  type BaseResponsePageMessage_ = {
    code?: number
    data?: PageMessage_
    message?: string
  }

  type BaseResponsePageMessageBoard_ = {
    code?: number
    data?: PageMessageBoard_
    message?: string
  }

  type BaseResponsePageObject_ = {
    code?: number
    data?: PageObject_
    message?: string
  }

  type BaseResponsePageObject_1 = {
    code?: number
    data?: PageObject_1
    message?: string
  }

  type BaseResponsePagePicture_ = {
    code?: number
    data?: PagePicture_
    message?: string
  }

  type BaseResponsePagePictureVO_ = {
    code?: number
    data?: PagePictureVO_
    message?: string
  }

  type BaseResponsePagePostVO_ = {
    code?: number
    data?: PagePostVO_
    message?: string
  }

  type BaseResponsePagePrivateChat_ = {
    code?: number
    data?: PagePrivateChat_
    message?: string
  }

  type BaseResponsePageReminderVO_ = {
    code?: number
    data?: PageReminderVO_
    message?: string
  }

  type BaseResponsePageReportVO_ = {
    code?: number
    data?: PageReportVO_
    message?: string
  }

  type BaseResponsePageShareRecord_ = {
    code?: number
    data?: PageShareRecord_
    message?: string
  }

  type BaseResponsePageShareRecordVO_ = {
    code?: number
    data?: PageShareRecordVO_
    message?: string
  }

  type BaseResponsePageSpace_ = {
    code?: number
    data?: PageSpace_
    message?: string
  }

  type BaseResponsePageSpaceVO_ = {
    code?: number
    data?: PageSpaceVO_
    message?: string
  }

  type BaseResponsePageSystemNotify_ = {
    code?: number
    data?: PageSystemNotify_
    message?: string
  }

  type BaseResponsePageTagVO_ = {
    code?: number
    data?: PageTagVO_
    message?: string
  }

  type BaseResponsePageTimeAlbum_ = {
    code?: number
    data?: PageTimeAlbum_
    message?: string
  }

  type BaseResponsePageUserVO_ = {
    code?: number
    data?: PageUserVO_
    message?: string
  }

  type BaseResponsePageViewRecordVO_ = {
    code?: number
    data?: PageViewRecordVO_
    message?: string
  }

  type BaseResponsePageWeiYan_ = {
    code?: number
    data?: PageWeiYan_
    message?: string
  }

  type BaseResponsePicture_ = {
    code?: number
    data?: Picture
    message?: string
  }

  type BaseResponsePictureTagCategory_ = {
    code?: number
    data?: PictureTagCategory
    message?: string
  }

  type BaseResponsePictureVO_ = {
    code?: number
    data?: PictureVO
    message?: string
  }

  type BaseResponsePostTagCategory_ = {
    code?: number
    data?: PostTagCategory
    message?: string
  }

  type BaseResponsePostVO_ = {
    code?: number
    data?: PostVO
    message?: string
  }

  type BaseResponsePrivateChat_ = {
    code?: number
    data?: PrivateChat
    message?: string
  }

  type BaseResponseRagChatResponse_ = {
    code?: number
    data?: RagChatResponse
    message?: string
  }

  type BaseResponseRagMessageVO_ = {
    code?: number
    data?: RagMessageVO
    message?: string
  }

  type BaseResponseRagSessionVO_ = {
    code?: number
    data?: RagSessionVO
    message?: string
  }

  type BaseResponseReportVO_ = {
    code?: number
    data?: ReportVO
    message?: string
  }

  type BaseResponseShareRecord_ = {
    code?: number
    data?: ShareRecord
    message?: string
  }

  type BaseResponseSnakeGameRecord_ = {
    code?: number
    data?: SnakeGameRecord
    message?: string
  }

  type BaseResponseSpace_ = {
    code?: number
    data?: Space
    message?: string
  }

  type BaseResponseSpaceUsageAnalyzeResponse_ = {
    code?: number
    data?: SpaceUsageAnalyzeResponse
    message?: string
  }

  type BaseResponseSpaceUser_ = {
    code?: number
    data?: SpaceUser
    message?: string
  }

  type BaseResponseSpaceVO_ = {
    code?: number
    data?: SpaceVO
    message?: string
  }

  type BaseResponseString_ = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseSystemNotify_ = {
    code?: number
    data?: SystemNotify
    message?: string
  }

  type BaseResponseTimeAlbum_ = {
    code?: number
    data?: TimeAlbum
    message?: string
  }

  type BaseResponseUser_ = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserHighestScoreVO_ = {
    code?: number
    data?: UserHighestScoreVO
    message?: string
  }

  type BaseResponseUserPublicVO_ = {
    code?: number
    data?: UserPublicVO
    message?: string
  }

  type BaseResponseUserUpdatePermissionsRequest_ = {
    code?: number
    data?: UserUpdatePermissionsRequest
    message?: string
  }

  type BaseResponseUserVO_ = {
    code?: number
    data?: UserVO
    message?: string
  }

  type BaseResponseYoloResponseDTO_ = {
    code?: number
    data?: YoloResponseDTO
    message?: string
  }

  type BugReportAddRequest = {
    bugType?: number
    description?: string
    screenshotUrls?: string
    title?: string
    websiteUrl?: string
  }

  type BugReportQueryRequest = {
    bugType?: number
    current?: number
    id?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    status?: number
    title?: string
    userId?: number
    websiteUrl?: string
  }

  type BugReportUpdateRequest = {
    bugType?: number
    description?: string
    id?: number
    screenshotUrls?: string
    status?: number
    title?: string
    websiteUrl?: string
  }

  type BugReportVO = {
    bugType?: number
    createTime?: string
    description?: string
    id?: number
    resolution?: string
    resolvedTime?: string
    screenshotUrls?: string[]
    status?: number
    title?: string
    updateTime?: string
    userId?: number
    userName?: string
    websiteUrl?: string
  }

  type cancelFavoriteUsingPOSTParams = {
    /** targetId */
    targetId: number
    /** targetType */
    targetType: number
    /** userId */
    userId: number
  }

  type CategoryVO = {
    categoryName?: string
    createTime?: string
    editTime?: string
    id?: number
    type?: number
    updateTime?: string
  }

  type ChartVO = {
    areaChartData?: AreaChartData
    pieChartData?: PieChartData
    radarChartData?: RadarChartData
    stackedBarChartData?: StackedBarChartData
  }

  type ChatMessage = {
    content?: string
    createTime?: string
    id?: number
    isDelete?: number
    messageLocation?: string
    messageSize?: number
    messageType?: string
    messageUrl?: string
    pictureId?: number
    privateChatId?: number
    receiverId?: number
    replyId?: number
    replyMessage?: ChatMessage
    rootId?: number
    sender?: User
    senderId?: number
    spaceId?: number
    status?: number
    type?: number
    updateTime?: string
  }

  type ChatMessageAdminRequest = {
    content?: string
    current?: number
    id?: number
    pageSize?: number
    pictureId?: number
    privateChatId?: number
    receiverId?: number
    senderId?: number
    sortField?: string
    sortOrder?: string
    spaceId?: number
    status?: number
    type?: number
  }

  type ChatMessageBatchRequest = {
    ids?: number[]
    operation?: string
  }

  type chatStreamUsingGETParams = {
    /** question */
    question: string
  }

  type chatUsingPOSTParams = {
    /** question */
    question: string
  }

  type checkHasFavoritedUsingGETParams = {
    /** targetId */
    targetId: number
    /** targetType */
    targetType: number
    /** userId */
    userId: number
  }

  type checkHasViewedUsingGETParams = {
    /** targetId */
    targetId: number
    /** targetType */
    targetType: number
    /** userId */
    userId: number
  }

  type checkPostPermissionUsingGETParams = {
    /** operation */
    operation: string
    /** postId */
    postId: number
  }

  type checkUsingGETParams = {
    /** echostr */
    echostr: string
    /** nonce */
    nonce: string
    /** signature */
    signature: string
    /** timestamp */
    timestamp: string
  }

  type checkWxBindStatusUsingGETParams = {
    /** sceneId */
    sceneId: string
  }

  type checkWxLoginStatusUsingGETParams = {
    /** sceneId */
    sceneId: string
  }

  type checkWxUnbindStatusUsingGETParams = {
    /** sceneId */
    sceneId: string
  }

  type clearSessionContextUsingPOSTParams = {
    /** sessionId */
    sessionId: number
  }

  type clearUnreadCountUsingPOSTParams = {
    /** isSender */
    isSender: boolean
    /** targetUserId */
    targetUserId: number
  }

  type Comments = {
    commentId?: number
    content?: string
    createTime?: string
    dislikeCount?: number
    isDelete?: number
    isRead?: number
    likeCount?: number
    location?: string
    parentCommentId?: number
    rootCommentId?: number
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type CommentsAddRequest = {
    content?: string
    parentCommentId?: number
    targetId?: number
    targetType?: number
    userId?: number
  }

  type CommentsAdminRequest = {
    commentId?: number
    content?: string
    current?: number
    isDelete?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetId?: number
    targetType?: number
    userId?: number
  }

  type CommentsBatchRequest = {
    ids?: number[]
    operation?: string
  }

  type CommentsDeleteRequest = {
    commentId?: number
  }

  type CommentsLikeRequest = {
    commentId?: number
    dislikeCount?: number
    likeCount?: number
    userId?: number
  }

  type CommentsQueryRequest = {
    commentId?: number
    content?: string
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetId?: number
    targetType?: number
    userId?: number
  }

  type CommentsVO = {
    children?: CommentsVO[]
    commentId?: number
    commentUser?: CommentUserVO
    content?: string
    createTime?: string
    current?: number
    dislikeCount?: number
    isRead?: number
    likeCount?: number
    location?: string
    pageSize?: number
    parentId?: number
    picture?: PictureVO
    post?: Post
    sortField?: string
    sortOrder?: string
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type CommentUserVO = {
    id?: number
    userAccount?: string
    userAvatar?: string
    userName?: string
  }

  type commonUploadAudioUsingPOSTParams = {
    /** 艺术家 */
    artist?: string
    /** 音频描述 */
    description?: string
    /** 标签 */
    tags?: string
    /** 音频标题 */
    title?: string
  }

  type commonUploadPictureUsingPOSTParams = {
    /** 图片描述 */
    description?: string
    /** 图片名称 */
    name?: string
    /** 标签 */
    tags?: string
  }

  type CopyPictureRequest = {
    pictureId?: number
    spaceId?: number
  }

  type countByTypeUsingGETParams = {
    /** siteType */
    siteType: string
  }

  type createMusicAlbumUsingPOSTParams = {
    /** loveBoardId */
    loveBoardId: number
  }

  type createOrUpdatePrivateChatUsingPOSTParams = {
    /** lastMessage */
    lastMessage?: string
    /** targetUserId */
    targetUserId: number
  }

  type CreateOutPaintingTaskResponse = {
    code?: string
    message?: string
    output?: Output
    requestId?: string
  }

  type CreatePictureOutPaintingTaskRequest = {
    parameters?: Parameters
    pictureId?: number
  }

  type DashboardVO = {
    newActivities?: number
    newAudioFiles?: number
    newBugReports?: number
    newChatMessages?: number
    newFriendLinks?: number
    newLoveBoards?: number
    newMessages?: number
    newPictures?: number
    newPosts?: number
    newReports?: number
    newSpaces?: number
    newUsers?: number
    totalBugReports?: number
    totalViews?: number
  }

  type deleteActivityUsingPOSTParams = {
    /** id */
    id: number
  }

  type deleteAudioFromAlbumUsingPOSTParams = {
    /** albumId */
    albumId: number
    /** audioId */
    audioId: number
  }

  type deleteAudioUsingDELETEParams = {
    /** 音频文件ID */
    id: number
  }

  type deleteCategoryUsingPOSTParams = {
    /** categoryId */
    categoryId: number
  }

  type deleteFriendLinkUsingPOSTParams = {
    /** id */
    id: number
  }

  type deleteHeartWallPictureUsingPOSTParams = {
    /** albumId */
    albumId: number
    /** pictureId */
    pictureId: number
  }

  type deleteLoveBoardUsingPOSTParams = {
    /** id */
    id: number
  }

  type deleteMessageUsingDELETEParams = {
    /** id */
    id: number
    /** ownerId */
    ownerId: number
  }

  type deleteMessageUsingPOSTParams = {
    /** id */
    id: number
  }

  type deletePostDraftUsingDELETEParams = {
    /** draftId */
    draftId: number
  }

  type deletePostUsingPOSTParams = {
    /** id */
    id: number
  }

  type deletePrivateChatUsingPOSTParams = {
    /** privateChatId */
    privateChatId: number
  }

  type deleteReminderUsingPOSTParams = {
    /** id */
    id: number
  }

  type DeleteRequest = {
    id?: number
  }

  type deleteSystemNotifyUsingDELETEParams = {
    /** id */
    id: number
  }

  type deleteTagUsingPOSTParams = {
    /** id */
    id?: number
  }

  type deleteTimeAlbumUsingPOSTParams = {
    /** id */
    id: number
    /** loveBoardId */
    loveBoardId: number
  }

  type deleteUserSearchHistoryUsingDELETEParams = {
    /** type */
    type: string
  }

  type deleteWeiYanUsingPOSTParams = {
    /** id */
    id: number
    /** loveBoardId */
    loveBoardId: number
  }

  type detectObjectsByUrlUsingPOSTParams = {
    /** imageUrl */
    imageUrl: string
  }

  type EmailCodeRequest = {
    email?: string
    type?: string
  }

  type FavoriteRecordAddRequest = {
    isFavorite?: boolean
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type FavoriteRecordQueryRequest = {
    current?: number
    isFavorite?: boolean
    isRead?: boolean
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type FavoriteRecordVO = {
    id?: number
    isRead?: number
    lastFavoriteTime?: string
    target?: Record<string, any>
    targetType?: number
    targetUserId?: number
    user?: UserVO
  }

  type fetchAlbumAudiosUsingGETParams = {
    /** albumId */
    albumId: number
    /** password */
    password?: string
  }

  type fetchMusicAlbumByIdUsingGETParams = {
    /** id */
    id: number
    /** password */
    password?: string
  }

  type fetchMusicAlbumsUsingGETParams = {
    /** current */
    current: number
    /** loveBoardId */
    loveBoardId: number
    /** pageSize */
    pageSize: number
  }

  type findCategoryUsingPOSTParams = {
    /** categoryName */
    categoryName: string
    /** type */
    type?: number
  }

  type FollowersAndFansVO = {
    fansCount?: number
    followCount?: number
  }

  type FriendLink = {
    clickCount?: number
    createTime?: string
    id?: number
    isDelete?: number
    ownerContact?: string
    ownerName?: string
    reviewMessage?: string
    siteDesc?: string
    siteLogo?: string
    siteName?: string
    siteType?: string
    siteUrl?: string
    status?: number
    updateTime?: string
    userId?: number
    viewCount?: number
    weight?: number
  }

  type Game2048Record = {
    createTime?: string
    gameTime?: number
    id?: number
    isDelete?: number
    maxTile?: number
    moveCount?: number
    score?: number
    updateTime?: string
    userId?: number
  }

  type Game2048RecordVO = {
    createTime?: string
    gameTime?: number
    maxTile?: number
    moveCount?: number
    score?: number
    userAvatar?: string
    userId?: number
    userName?: string
  }

  type GameRankingRequest = {
    gameMode?: number
    limit?: number
  }

  type getActivityByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getAdminMessageByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getAudioByIdUsingGETParams = {
    /** id */
    id: number
  }

  type getBugReportByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getChatHistoryUsingPOSTParams = {
    /** current */
    current?: number
    /** pageSize */
    pageSize?: number
  }

  type getCommentByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getFollowAndFansCountUsingPOSTParams = {
    /** id */
    id: number
  }

  type getFriendLinkByIdUsingGETParams = {
    /** id */
    id: number
  }

  type getGuessYouWantToSearchUsingGETParams = {
    /** size */
    size?: number
    /** type */
    type: string
  }

  type getHeartWallPicturesUsingGETParams = {
    /** albumId */
    albumId: number
    /** password */
    password?: string
    /** userId */
    userId?: number
  }

  type getHotSearchKeywordsUsingGETParams = {
    /** size */
    size?: number
    /** type */
    type: string
  }

  type getKnowledgeFileVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getLikeByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getLikeStatusUsingGETParams = {
    /** targetId */
    targetId: number
    /** targetType */
    targetType: number
  }

  type getLoveBoardByIdAdminUsingGETParams = {
    /** id */
    id?: number
  }

  type getLoveBoardByIdUsingGETParams = {
    /** id */
    id: number
  }

  type getMessageByIdUsingGET1Params = {
    /** id */
    id?: number
  }

  type getMessageByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getMessageListUsingGETParams = {
    /** current */
    current?: number
    /** pageSize */
    pageSize?: number
    /** type */
    type: string
  }

  type GetOutPaintingTaskResponse = {
    output?: Output1
    requestId?: string
  }

  type getPictureByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getPictureOutPaintingTaskUsingGETParams = {
    /** taskId */
    taskId?: string
  }

  type getPicturePermissionUsingGETParams = {
    /** pictureId */
    pictureId?: number
  }

  type getPictureVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getPostByIdUsingGETParams = {
    /** id */
    id: number
  }

  type getRankingListUsingGETParams = {
    /** limit */
    limit?: number
  }

  type getReportByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getSessionByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getShareByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getShareStatusUsingGETParams = {
    /** targetId */
    targetId: number
    /** targetType */
    targetType: number
  }

  type getSpaceByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getSpaceVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getSystemNotifyByIdUsingGETParams = {
    /** id */
    id: number
  }

  type getTimeAlbumByIdUsingGETParams = {
    /** id */
    id: number
    /** password */
    password?: string
    /** userId */
    userId?: number
  }

  type getTop100PictureUsingGETParams = {
    /** id */
    id: number
  }

  type getTop100PostUsingGETParams = {
    /** id */
    id: number
  }

  type getUserAllPicturesUsingGETParams = {
    /** current */
    current?: number
    /** size */
    size?: number
  }

  type getUserByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getUserGameHistoryUsingGETParams = {
    /** current */
    current?: number
    /** pageSize */
    pageSize?: number
  }

  type getUserMultiDeviceLoginUsingGETParams = {
    /** userId */
    userId: number
  }

  type getUserNotifyByIdUsingGETParams = {
    /** id */
    id: number
  }

  type getUserPermissionsUsingGETParams = {
    /** userId */
    userId: number
  }

  type getUserPicturesFromOtherSpacesUsingGETParams = {
    /** current */
    current?: number
    /** size */
    size?: number
    /** spaceId */
    spaceId?: number
  }

  type getUserPublicInfoUsingGETParams = {
    /** userId */
    userId: number
  }

  type getUserSearchHistoryUsingGETParams = {
    /** size */
    size?: number
    /** type */
    type: string
    /** userId */
    userId: number
  }

  type getUserSignInRecordUsingGETParams = {
    /** year */
    year?: number
  }

  type getUserVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getVersionHistoryUsingGETParams = {
    /** current */
    current?: number
    /** pageSize */
    pageSize?: number
  }

  type HotSearchVO = {
    count?: number
    keyword?: string
    realTimeCount?: number
    reason?: string
    score?: number
    trend?: number
  }

  type ImageSearchResult = {
    fromUrl?: string
    thumbUrl?: string
  }

  type increaseClickCountUsingPOSTParams = {
    /** id */
    id: number
  }

  type IPageRagMessageVO_ = {
    current?: number
    pages?: number
    records?: RagMessageVO[]
    size?: number
    total?: number
  }

  type IPageRagSessionVO_ = {
    current?: number
    pages?: number
    records?: RagSessionVO[]
    size?: number
    total?: number
  }

  type KnowledgeFile = {
    createTime?: string
    fileSize?: number
    fileType?: string
    fileUrl?: string
    id?: number
    isDelete?: number
    md5Hash?: string
    originalName?: string
    storedName?: string
    updateTime?: string
    uploadTime?: string
    userId?: number
    vectorCount?: number
  }

  type KnowledgeFileQueryRequest = {
    current?: number
    endUploadTime?: string
    fileType?: string
    id?: number
    maxVectorCount?: number
    md5Hash?: string
    minVectorCount?: number
    originalName?: string
    pageSize?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    startUploadTime?: string
    storedName?: string
    userId?: number
  }

  type KnowledgeFileVO = {
    createTime?: string
    fileSize?: number
    fileSizeDisplay?: string
    fileType?: string
    fileUrl?: string
    id?: number
    md5Hash?: string
    originalName?: string
    storedName?: string
    updateTime?: string
    uploadTime?: string
    userId?: number
    userName?: string
    vectorCount?: number
  }

  type LikeAdminRequest = {
    current?: number
    id?: number
    isLiked?: boolean
    isRead?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type LikeBatchRequest = {
    ids?: number[]
    operation?: string
  }

  type likeMessageUsingPOSTParams = {
    /** id */
    id: number
  }

  type likePostUsingPOSTParams = {
    /** id */
    id: number
  }

  type LikeQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetType?: number
  }

  type LikeRecord = {
    firstLikeTime?: string
    id?: number
    isLiked?: boolean
    isRead?: number
    lastLikeTime?: string
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type LikeRecordVO = {
    id?: number
    isRead?: number
    lastLikeTime?: string
    target?: Record<string, any>
    targetType?: number
    targetUserId?: number
    user?: UserVO
  }

  type LikeRequest = {
    isLiked?: boolean
    targetId?: number
    targetType?: number
  }

  type likeWeiYanUsingPOSTParams = {
    /** id */
    id: number
  }

  type listAudioByPageAdminUsingGETParams = {
    album?: string
    artist?: string
    current?: number
    genre?: string
    pageSize?: number
    sortField?: string
    sortOrder?: string
    spaceId?: number
    title?: string
    userId?: number
  }

  type listAudioByPageUsingGETParams = {
    album?: string
    artist?: string
    current?: number
    genre?: string
    pageSize?: number
    sortField?: string
    sortOrder?: string
    spaceId?: number
    title?: string
    userId?: number
  }

  type listCategoryByTypeUsingGETParams = {
    /** type */
    type: number
  }

  type listCategoryVOUsingPOSTParams = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    /** type */
    type?: number
  }

  type listChatByPageAdminUsingGETParams = {
    /** current */
    current?: number
    /** pageSize */
    pageSize?: number
    /** role */
    role?: string
    /** userId */
    userId?: number
  }

  type listFriendLinksByPageUsingGETParams = {
    current?: number
    pageSize?: number
    siteName?: string
    siteType?: string
    sortField?: string
    sortOrder?: string
    status?: number
  }

  type listMessagesByPageUsingGETParams = {
    /** current */
    current?: number
    /** ownerId */
    ownerId: number
    /** size */
    size?: number
  }

  type listMessagesBySessionIdUsingGETParams = {
    /** sessionId */
    sessionId: number
  }

  type listPublicLoveBoardsUsingGETParams = {
    /** current */
    current?: number
    /** manName */
    manName?: string
    /** size */
    size?: number
    /** sortField */
    sortField?: string
    /** sortOrder */
    sortOrder?: string
    /** womanName */
    womanName?: string
  }

  type listQaMessagesUsingGETParams = {
    content?: string
    current?: number
    isDelete?: number
    messageType?: number
    pageSize?: number
    sessionId?: number
    sortField?: string
    sortOrder?: string
    userId?: number
  }

  type listRecommendFriendLinksUsingGETParams = {
    /** limit */
    limit?: number
  }

  type listSessionsUsingGETParams = {
    current?: number
    id?: number
    isActive?: number
    isDelete?: number
    pageSize?: number
    sessionName?: string
    sortField?: string
    sortOrder?: string
    userId?: number
  }

  type listSystemNotifiesUsingGETParams = {
    current?: number
    isEnabled?: number
    notifyType?: string
    pageSize?: number
    readStatus?: number
    receiverType?: string
    sortField?: string
    sortOrder?: string
  }

  type listTimeAlbumUsingGETParams = {
    /** current */
    current: number
    /** loveBoardId */
    loveBoardId: number
    /** pageSize */
    pageSize: number
  }

  type listUserNotifiesUsingGETParams = {
    current?: number
    isEnabled?: number
    notifyType?: string
    pageSize?: number
    readStatus?: number
    receiverType?: string
    sortField?: string
    sortOrder?: string
  }

  type listUserReadNotifiesUsingGETParams = {
    current?: number
    isEnabled?: number
    notifyType?: string
    pageSize?: number
    readStatus?: number
    receiverType?: string
    sortField?: string
    sortOrder?: string
  }

  type listWeiYanUsingGETParams = {
    /** current */
    current?: number
    /** loveBoardId */
    loveBoardId: number
    /** pageSize */
    pageSize?: number
    /** userId */
    userId?: number
  }

  type LoginUserVO = {
    birthday?: string
    createTime?: string
    editTime?: string
    email?: string
    gender?: string
    hasBindWx?: boolean
    homepageBg?: string
    id?: number
    personalSign?: string
    region?: string
    updateTime?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
    userTags?: string
  }

  type LoveBoard = {
    bgCover?: string
    countdownTime?: string
    countdownTitle?: string
    createTime?: string
    familyInfo?: string
    id?: number
    isDelete?: number
    likeCount?: number
    manCover?: string
    manName?: string
    status?: number
    timing?: string
    updateTime?: string
    userId?: number
    viewCount?: number
    womanCover?: string
    womanName?: string
  }

  type LoveBoardAdminRequest = {
    current?: number
    id?: number
    manName?: string
    pageSize?: number
    sortField?: string
    sortOrder?: string
    status?: number
    userId?: number
    womanName?: string
  }

  type LoveBoardBatchRequest = {
    ids?: number[]
    operation?: string
  }

  type LoveBoardMusicAlbum = {
    albumName?: string
    coverUrl?: string
    createTime?: string
    description?: string
    id?: number
    isDelete?: number
    isPublic?: number
    loveBoardId?: number
    password?: string
    updateTime?: string
    userId?: number
  }

  type MapStringObject_ = true

  type MapStringString_ = true

  type markAsReadUsingPUTParams = {
    /** id */
    id: number
  }

  type markSingleAsReadUsingPOSTParams = {
    /** id */
    id: number
    /** type */
    type: string
  }

  type Message = {
    content?: string
    createTime?: string
    id?: number
    ip?: string
    isDelete?: number
    updateTime?: string
  }

  type MessageAdminRequest = {
    content?: string
    current?: number
    id?: number
    ip?: string
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type MessageBatchRequest = {
    ids?: number[]
    operation?: string
  }

  type MessageBoard = {
    browser?: string
    content?: string
    createTime?: string
    id?: number
    ipAddress?: string
    isDelete?: number
    likeCount?: number
    location?: string
    nickname?: string
    os?: string
    ownerId?: number
    qq?: string
    status?: number
    updateTime?: string
    userId?: number
  }

  type MessageCenterVO = {
    totalUnread?: number
    unreadComments?: number
    unreadLikes?: number
    unreadShares?: number
    unreadSystemNotifies?: number
  }

  type MessageQueryRequest = {
    content?: string
    current?: number
    ip?: string
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type MessageVO = {
    content?: string
    createTime?: string
    id?: number
  }

  type modifyMusicAlbumUsingPOSTParams = {
    /** loveBoardId */
    loveBoardId: number
  }

  type MusicAlbumPasswordRequest = {
    albumId?: number
    newPassword?: string
    oldPassword?: string
  }

  type Output = {
    taskId?: string
    taskStatus?: string
  }

  type Output1 = {
    code?: string
    endTime?: string
    message?: string
    outputImageUrl?: string
    scheduledTime?: string
    submitTime?: string
    taskId?: string
    taskMetrics?: TaskMetrics
    taskStatus?: string
  }

  type Pageable = {
    offset?: number
    pageNumber?: number
    pageSize?: number
    paged?: boolean
    sort?: Sort
    unpaged?: boolean
  }

  type PageActivity_ = {
    current?: number
    pages?: number
    records?: Activity[]
    size?: number
    total?: number
  }

  type PageAiChat_ = {
    current?: number
    pages?: number
    records?: AiChat[]
    size?: number
    total?: number
  }

  type PageAiChatVO_ = {
    current?: number
    pages?: number
    records?: AiChatVO[]
    size?: number
    total?: number
  }

  type PageAppVersion_ = {
    current?: number
    pages?: number
    records?: AppVersion[]
    size?: number
    total?: number
  }

  type PageAudioFile_ = {
    current?: number
    pages?: number
    records?: AudioFile[]
    size?: number
    total?: number
  }

  type PageAudioFileVO_ = {
    current?: number
    pages?: number
    records?: AudioFileVO[]
    size?: number
    total?: number
  }

  type PageBugReportVO_ = {
    current?: number
    pages?: number
    records?: BugReportVO[]
    size?: number
    total?: number
  }

  type PageCategoryVO_ = {
    current?: number
    pages?: number
    records?: CategoryVO[]
    size?: number
    total?: number
  }

  type PageChatMessage_ = {
    current?: number
    pages?: number
    records?: ChatMessage[]
    size?: number
    total?: number
  }

  type PageComments_ = {
    current?: number
    pages?: number
    records?: Comments[]
    size?: number
    total?: number
  }

  type PageCommentsVO_ = {
    current?: number
    pages?: number
    records?: CommentsVO[]
    size?: number
    total?: number
  }

  type PageFavoriteRecordVO_ = {
    current?: number
    pages?: number
    records?: FavoriteRecordVO[]
    size?: number
    total?: number
  }

  type PageFriendLink_ = {
    current?: number
    pages?: number
    records?: FriendLink[]
    size?: number
    total?: number
  }

  type PageKnowledgeFileVO_ = {
    current?: number
    pages?: number
    records?: KnowledgeFileVO[]
    size?: number
    total?: number
  }

  type PageLikeRecord_ = {
    current?: number
    pages?: number
    records?: LikeRecord[]
    size?: number
    total?: number
  }

  type PageLikeRecordVO_ = {
    current?: number
    pages?: number
    records?: LikeRecordVO[]
    size?: number
    total?: number
  }

  type PageLoveBoard_ = {
    current?: number
    pages?: number
    records?: LoveBoard[]
    size?: number
    total?: number
  }

  type PageLoveBoardMusicAlbum_ = {
    current?: number
    pages?: number
    records?: LoveBoardMusicAlbum[]
    size?: number
    total?: number
  }

  type PageMessage_ = {
    current?: number
    pages?: number
    records?: Message[]
    size?: number
    total?: number
  }

  type PageMessageBoard_ = {
    current?: number
    pages?: number
    records?: MessageBoard[]
    size?: number
    total?: number
  }

  type PageObject_ = {
    current?: number
    pages?: number
    records?: Record<string, any>[]
    size?: number
    total?: number
  }

  type PageObject_1 = {
    content?: Record<string, any>[]
    empty?: boolean
    first?: boolean
    last?: boolean
    number?: number
    numberOfElements?: number
    pageable?: Pageable
    size?: number
    sort?: Sort
    totalElements?: number
    totalPages?: number
  }

  type PagePicture_ = {
    current?: number
    pages?: number
    records?: Picture[]
    size?: number
    total?: number
  }

  type PagePictureVO_ = {
    current?: number
    pages?: number
    records?: PictureVO[]
    size?: number
    total?: number
  }

  type PagePostVO_ = {
    current?: number
    pages?: number
    records?: PostVO[]
    size?: number
    total?: number
  }

  type PagePrivateChat_ = {
    current?: number
    pages?: number
    records?: PrivateChat[]
    size?: number
    total?: number
  }

  type PageReminderVO_ = {
    current?: number
    pages?: number
    records?: ReminderVO[]
    size?: number
    total?: number
  }

  type PageReportVO_ = {
    current?: number
    pages?: number
    records?: ReportVO[]
    size?: number
    total?: number
  }

  type PageRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type PageShareRecord_ = {
    current?: number
    pages?: number
    records?: ShareRecord[]
    size?: number
    total?: number
  }

  type PageShareRecordVO_ = {
    current?: number
    pages?: number
    records?: ShareRecordVO[]
    size?: number
    total?: number
  }

  type PageSpace_ = {
    current?: number
    pages?: number
    records?: Space[]
    size?: number
    total?: number
  }

  type PageSpaceVO_ = {
    current?: number
    pages?: number
    records?: SpaceVO[]
    size?: number
    total?: number
  }

  type PageSystemNotify_ = {
    current?: number
    pages?: number
    records?: SystemNotify[]
    size?: number
    total?: number
  }

  type PageTagVO_ = {
    current?: number
    pages?: number
    records?: TagVO[]
    size?: number
    total?: number
  }

  type PageTimeAlbum_ = {
    current?: number
    pages?: number
    records?: TimeAlbum[]
    size?: number
    total?: number
  }

  type PageUserVO_ = {
    current?: number
    pages?: number
    records?: UserVO[]
    size?: number
    total?: number
  }

  type PageViewRecordVO_ = {
    current?: number
    pages?: number
    records?: ViewRecordVO[]
    size?: number
    total?: number
  }

  type PageWeiYan_ = {
    current?: number
    pages?: number
    records?: WeiYan[]
    size?: number
    total?: number
  }

  type Parameters = {
    addWatermark?: boolean
    angle?: number
    bestQuality?: boolean
    bottomOffset?: number
    leftOffset?: number
    limitImageSize?: boolean
    outputRatio?: string
    rightOffset?: number
    topOffset?: number
    xScale?: number
    yScale?: number
  }

  type Picture = {
    aiLabels?: string
    allowCollect?: number
    allowComment?: number
    allowLike?: number
    allowShare?: number
    category?: string
    commentCount?: number
    createTime?: string
    editTime?: string
    favoriteCount?: number
    hotScore?: number
    id?: number
    introduction?: string
    isDelete?: number
    isDownload?: number
    isDraft?: number
    isFeature?: number
    likeCount?: number
    name?: string
    picColor?: string
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    recommendScore?: number
    reviewMessage?: string
    reviewStatus?: number
    reviewTime?: string
    reviewerId?: number
    shareCount?: number
    spaceId?: number
    tags?: string
    thumbnailUrl?: string
    updateTime?: string
    url?: string
    userId?: number
    viewCount?: number
  }

  type PictureEditByBatchRequest = {
    category?: string
    nameRule?: string
    pictureIdList?: number[]
    spaceId?: number
    tags?: string[]
  }

  type PictureEditRequest = {
    aiLabels?: string
    category?: string
    id?: number
    introduction?: string
    isDownload?: number
    name?: string
    tags?: string[]
  }

  type PictureFeatureRequest = {
    id?: number
    isFeature?: number
  }

  type PictureOperation = {
    ids?: number[]
    operationType?: number
  }

  type PicturePermissionRequest = {
    allowCollect?: number
    allowComment?: number
    allowLike?: number
    allowShare?: number
    pictureId?: number
  }

  type PictureQueryRequest = {
    category?: string
    current?: number
    endEditTime?: string
    id?: number
    introduction?: string
    name?: string
    nullSpaceId?: boolean
    pageSize?: number
    picColor?: string
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    reviewMessage?: string
    reviewStatus?: number
    reviewTime?: string
    reviewerId?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    spaceId?: number
    startEditTime?: string
    tags?: string[]
    userId?: number
  }

  type PictureReviewRequest = {
    id?: number
    reviewMessage?: string
    reviewStatus?: number
  }

  type PictureTagCategory = {
    categoryList?: string[]
    tagList?: string[]
  }

  type PictureUploadByBatchRequest = {
    categoryName?: string
    count?: number
    namePrefix?: string
    searchText?: string
    tagName?: string[]
  }

  type PictureUploadRequest = {
    categoryName?: string
    fileUrl?: string
    id?: number
    introduction?: string
    picName?: string
    spaceId?: number
    tagName?: string
  }

  type PictureVO = {
    aiLabels?: string[]
    allowCollect?: boolean
    allowComment?: boolean
    allowLike?: boolean
    allowShare?: boolean
    category?: string
    chatCount?: number
    commentCount?: number
    createTime?: string
    editTime?: string
    favoriteCount?: number
    id?: number
    introduction?: string
    isDownload?: number
    isFavorited?: number
    isFeature?: number
    isLiked?: number
    isShared?: number
    likeCount?: number
    name?: string
    permissionList?: string[]
    picColor?: string
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    recommendScore?: number
    reviewMessage?: string
    reviewStatus?: number
    shareCount?: number
    spaceId?: number
    tags?: string[]
    thumbnailUrl?: string
    updateTime?: string
    url?: string
    user?: UserVO
    userId?: number
    viewCount?: number
  }

  type PieChartData = {
    labels?: string[]
    values?: number[]
  }

  type Post = {
    allowCollect?: number
    allowComment?: number
    allowLike?: number
    allowShare?: number
    category?: string
    commentCount?: number
    content?: string
    coverUrl?: string
    createTime?: string
    favoriteCount?: number
    hotScore?: number
    id?: number
    isDelete?: number
    isDraft?: number
    likeCount?: number
    reviewMessage?: string
    shareCount?: number
    status?: number
    tags?: string
    title?: string
    updateTime?: string
    user?: UserVO
    userId?: number
    viewCount?: number
  }

  type PostAddRequest = {
    category?: string
    content?: string
    coverUrl?: string
    id?: number
    tags?: string[]
    title?: string
  }

  type PostPermissionRequest = {
    allowCollect?: number
    allowComment?: number
    allowLike?: number
    allowShare?: number
    postId?: number
  }

  type PostQueryRequest = {
    category?: string
    current?: number
    isPublic?: boolean
    pageSize?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    status?: number
    userId?: number
  }

  type PostTagCategory = {
    categoryList?: string[]
    tagList?: string[]
  }

  type PostVO = {
    allowCollect?: number
    allowComment?: number
    allowLike?: number
    allowShare?: number
    category?: string
    commentCount?: number
    content?: string
    coverUrl?: string
    createTime?: string
    favoriteCount?: number
    hotScore?: number
    id?: number
    isDraft?: number
    isFavorited?: number
    isLiked?: number
    isShared?: number
    likeCount?: number
    reviewMessage?: string
    shareCount?: number
    status?: number
    tagList?: string[]
    tags?: string
    title?: string
    updateTime?: string
    user?: UserVO
    userId?: number
    viewCount?: number
  }

  type PrivateChat = {
    chatType?: number
    createTime?: string
    id?: number
    isDelete?: number
    isSender?: boolean
    lastMessage?: string
    lastMessageTime?: string
    targetUser?: UserVO
    targetUserChatName?: string
    targetUserId?: number
    targetUserUnreadCount?: number
    updateTime?: string
    userChatName?: string
    userId?: number
    userUnreadCount?: number
  }

  type PrivateChatQueryRequest = {
    chatType?: number
    current?: number
    pageSize?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    targetUserId?: number
  }

  type QaMessageAddRequest = {
    content?: string
    sessionId?: number
  }

  type QaMessageQueryRequest = {
    content?: string
    current?: number
    isDelete?: number
    messageType?: number
    pageSize?: number
    sessionId?: number
    sortField?: string
    sortOrder?: string
    userId?: number
  }

  type RadarChartData = {
    data?: MapStringObject_[]
    indicator?: string[]
  }

  type RagChatResponse = {
    answer?: string
    cacheHit?: boolean
  }

  type RagMessageVO = {
    content?: string
    createTime?: string
    id?: number
    isDelete?: number
    messageType?: number
    sessionId?: number
    userId?: number
  }

  type RagSessionVO = {
    createTime?: string
    id?: number
    isActive?: number
    isDelete?: number
    sessionName?: string
    updateTime?: string
    userId?: number
  }

  type ReminderAddRequest = {
    content?: string
  }

  type ReminderQueryRequest = {
    completed?: boolean
    current?: number
    date?: string
    important?: boolean
    pageSize?: number
    sortField?: string
    sortOrder?: string
    starred?: boolean
  }

  type ReminderVO = {
    completed?: boolean
    content?: string
    id?: number
    important?: boolean
    starred?: boolean
    time?: string
  }

  type removeMusicAlbumUsingPOSTParams = {
    /** id */
    id: number
    /** loveBoardId */
    loveBoardId: number
  }

  type ReportAddRequest = {
    reason?: string
    reportType?: number
    screenshotUrls?: string[]
    targetId?: number
    targetType?: number
  }

  type ReportQueryRequest = {
    current?: number
    id?: number
    pageSize?: number
    reportType?: number
    sortField?: string
    sortOrder?: string
    status?: number
    targetId?: number
    targetType?: number
    userId?: number
  }

  type ReportUpdateRequest = {
    handleResult?: string
    id?: number
    status?: number
  }

  type ReportViewDurationRequest = {
    clientTimestamp?: number
    duration?: number
    targetId?: number
    targetType?: number
    userId?: number
  }

  type ReportVO = {
    createTime?: string
    handleResult?: string
    handleTime?: string
    handlerId?: number
    handlerName?: string
    id?: number
    reason?: string
    reportType?: number
    reportTypeText?: string
    screenshotUrls?: string[]
    status?: number
    statusText?: string
    targetId?: number
    targetType?: number
    targetTypeText?: string
    updateTime?: string
    userAvatar?: string
    userId?: number
    userName?: string
  }

  type reviewActivityUsingPOSTParams = {
    /** activityId */
    activityId: number
    /** message */
    message?: string
    /** status */
    status: number
  }

  type reviewFriendLinkUsingPOSTParams = {
    /** id */
    id: number
    /** reviewMessage */
    reviewMessage?: string
    /** status */
    status: number
  }

  type reviewPostUsingPOSTParams = {
    /** id */
    id: number
    /** message */
    message?: string
    /** status */
    status: number
  }

  type SaveGameRecordRequest = {
    gameTime?: number
    maxTile?: number
    moveCount?: number
    score?: number
  }

  type SaveGameRecordRequest1 = {
    foodCount?: number
    gameMode?: number
    gameTime?: number
    score?: number
    snakeLength?: number
  }

  type SearchPictureByColorRequest = {
    picColor?: string
    spaceId?: number
  }

  type SearchPictureByPictureRequest = {
    pictureId?: number
  }

  type SearchRequest = {
    current?: number
    pageSize?: number
    searchText?: string
    type?: string
    userId?: number
  }

  type searchTagUsingPOSTParams = {
    /** tagName */
    tagName?: string
  }

  type sendQaStreamMessageUsingGETParams = {
    /** message */
    message: string
    /** sessionId */
    sessionId?: number
  }

  type sendUsingPOSTParams = {
    /** query */
    query?: string
  }

  type SessionDeleteRequest = {
    sessionId?: number
  }

  type SessionQueryRequest = {
    current?: number
    id?: number
    isActive?: number
    isDelete?: number
    pageSize?: number
    sessionName?: string
    sortField?: string
    sortOrder?: string
    userId?: number
  }

  type SessionSwitchRequest = {
    sessionId?: number
  }

  type setRecommendStatusUsingPOSTParams = {
    /** recommendStatus */
    recommendStatus: number
    /** spaceId */
    spaceId: number
  }

  type ShareAdminRequest = {
    current?: number
    id?: number
    isRead?: number
    isShared?: boolean
    pageSize?: number
    shareTime?: string
    sortField?: string
    sortOrder?: string
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type ShareBatchRequest = {
    ids?: number[]
    operation?: string
  }

  type ShareQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetType?: number
  }

  type ShareRecord = {
    id?: number
    isRead?: number
    isShared?: boolean
    shareTime?: string
    targetId?: number
    targetType?: number
    targetUserId?: number
    userId?: number
  }

  type ShareRecordVO = {
    id?: number
    isRead?: number
    shareTime?: string
    target?: Record<string, any>
    targetType?: number
    user?: UserVO
  }

  type ShareRequest = {
    isShared?: boolean
    targetId?: number
    targetType?: number
  }

  type SnakeGameRecord = {
    createTime?: string
    foodCount?: number
    gameMode?: number
    gameTime?: number
    id?: number
    isDelete?: number
    score?: number
    snakeLength?: number
    updateTime?: string
    userId?: number
  }

  type solveBugReportUsingPOSTParams = {
    /** id */
    id?: number
    /** resolution */
    resolution?: string
    /** status */
    status?: number
  }

  type Sort = {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }

  type Space = {
    createTime?: string
    editTime?: string
    id?: number
    isDelete?: number
    isRecommended?: number
    maxCount?: number
    maxSize?: number
    spaceCover?: string
    spaceDesc?: string
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    totalCount?: number
    totalSize?: number
    updateTime?: string
    userId?: number
  }

  type SpaceAddRequest = {
    spaceCover?: string
    spaceDesc?: string
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
  }

  type SpaceCategoryAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceCategoryAnalyzeResponse = {
    category?: string
    count?: number
    totalSize?: number
  }

  type SpaceEditRequest = {
    id?: number
    spaceCover?: string
    spaceDesc?: string
    spaceName?: string
  }

  type SpaceLevel = {
    maxCount?: number
    maxSize?: number
    text?: string
    value?: number
  }

  type SpaceQueryRequest = {
    current?: number
    id?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    userId?: number
  }

  type SpaceRankAnalyzeRequest = {
    topN?: number
  }

  type SpaceSizeAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceSizeAnalyzeResponse = {
    count?: number
    sizeRange?: string
  }

  type SpaceTagAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceTagAnalyzeResponse = {
    count?: number
    tag?: string
  }

  type SpaceUpdateRequest = {
    id?: number
    maxCount?: number
    maxSize?: number
    spaceCover?: string
    spaceDesc?: string
    spaceLevel?: number
    spaceName?: string
  }

  type SpaceUsageAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceUsageAnalyzeResponse = {
    countUsageRatio?: number
    maxCount?: number
    maxSize?: number
    sizeUsageRatio?: number
    usedCount?: number
    usedSize?: number
  }

  type SpaceUser = {
    createTime?: string
    id?: number
    isRecommended?: number
    spaceId?: number
    spaceRole?: string
    status?: number
    updateTime?: string
    userId?: number
  }

  type SpaceUserAddRequest = {
    spaceId?: number
    spaceRole?: string
    userId?: number
  }

  type SpaceUserAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
    timeDimension?: string
    userId?: number
  }

  type SpaceUserAnalyzeResponse = {
    count?: number
    period?: string
  }

  type SpaceUserAuditRequest = {
    spaceId?: number
    status?: number
    userId?: number
  }

  type SpaceUserEditRequest = {
    id?: number
    spaceRole?: string
  }

  type SpaceUserJoinRequest = {
    spaceId?: number
  }

  type SpaceUserQueryRequest = {
    current?: number
    id?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    spaceId?: number
    spaceRole?: string
    status?: number
    userId?: number
  }

  type SpaceUserRecommendRequest = {
    isRecommended?: number
    spaceId?: number
    userId?: number
  }

  type SpaceUserVO = {
    createTime?: string
    id?: number
    space?: SpaceVO
    spaceId?: number
    spaceRole?: string
    status?: number
    updateTime?: string
    user?: UserVO
    userId?: number
  }

  type SpaceVO = {
    activities?: Activity[]
    createTime?: string
    editTime?: string
    id?: number
    isRecommended?: number
    maxCount?: number
    maxSize?: number
    memberCount?: number
    permissionList?: string[]
    recommendedUsers?: SpaceUserVO[]
    spaceCover?: string
    spaceDesc?: string
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    totalCount?: number
    totalSize?: number
    updateTime?: string
    user?: UserVO
    userId?: number
  }

  type SseEmitter = {
    timeout?: number
  }

  type StackedBarChartData = {
    series?: MapStringObject_[]
    xaxisData?: string[]
  }

  type SystemNotify = {
    content?: string
    createTime?: string
    expireTime?: string
    id?: number
    isDelete?: number
    isEnabled?: number
    isGlobal?: number
    notifyIcon?: string
    notifyType?: string
    operatorId?: string
    operatorType?: string
    readStatus?: number
    readTime?: string
    receiverId?: string
    receiverType?: string
    relatedBizId?: string
    relatedBizType?: string
    senderId?: string
    senderType?: string
    title?: string
    updateTime?: string
  }

  type SystemNotifyAddRequest = {
    content?: string
    expireTime?: string
    isEnabled?: number
    isGlobal?: number
    notifyIcon?: string
    notifyType?: string
    receiverId?: string
    receiverType?: string
    relatedBizId?: string
    relatedBizType?: string
    senderId?: string
    senderType?: string
    title?: string
  }

  type SystemNotifyUpdateRequest = {
    content?: string
    id?: number
    isEnabled?: number
    title?: string
  }

  type TagVO = {
    createTime?: string
    editTime?: string
    id?: number
    tagName?: string
    updateTime?: string
  }

  type TaskMetrics = {
    failed?: number
    succeeded?: number
    total?: number
  }

  type testDownloadFileUsingGETParams = {
    /** filepath */
    filepath?: string
  }

  type TimeAlbum = {
    albumName?: string
    coverUrl?: string
    createTime?: string
    description?: string
    id?: number
    isDelete?: number
    isPublic?: number
    loveBoardId?: number
    password?: string
    updateTime?: string
    userId?: number
  }

  type TimeAlbumPasswordRequest = {
    albumId?: number
    newPassword?: string
    oldPassword?: string
  }

  type toggleImportantUsingPOSTParams = {
    /** id */
    id: number
  }

  type toggleReminderUsingPOSTParams = {
    /** id */
    id: number
  }

  type toggleStarredUsingPOSTParams = {
    /** id */
    id: number
  }

  type updateChatNameUsingPOSTParams = {
    /** chatName */
    chatName: string
    /** privateChatId */
    privateChatId: number
  }

  type updateChatTypeUsingPOSTParams = {
    /** isFriend */
    isFriend: boolean
    /** targetUserId */
    targetUserId: number
  }

  type updateMessageStatusUsingPOSTParams = {
    /** id */
    id: number
    /** status */
    status: number
  }

  type UpdatePictureDraftRequest = {
    isDraft?: number
    pictureId?: number
  }

  type updateSystemNotifyUsingPUTParams = {
    /** id */
    id: number
  }

  type updateTimeAlbumUsingPOSTParams = {
    /** loveBoardId */
    loveBoardId: number
  }

  type updateUserAvatarUsingPOSTParams = {
    /** id */
    id?: number
  }

  type updateWeightUsingPOSTParams = {
    /** id */
    id: number
    /** weight */
    weight: number
  }

  type uploadAudioUsingPOSTParams = {
    album?: string
    artist?: string
    description?: string
    genre?: string
    spaceId?: number
    title?: string
  }

  type uploadHeartWallPicturesUsingPOSTParams = {
    albumId?: number
    files?: string[]
    override?: boolean
  }

  type uploadMusicToAlbumUsingPOSTParams = {
    /** album */
    album?: string
    /** albumId */
    albumId: number
    /** artist */
    artist?: string
    /** coverUrl */
    coverUrl?: string
    /** description */
    description?: string
    /** genre */
    genre?: string
    /** title */
    title?: string
  }

  type uploadNewVersionUsingPOSTParams = {
    /** appVersion */
    appVersion: string
  }

  type uploadPictureUsingPOSTParams = {
    categoryName?: string
    fileUrl?: string
    id?: number
    introduction?: string
    picName?: string
    spaceId?: number
    tagName?: string
  }

  type uploadPostImageUsingPOSTParams = {
    categoryName?: string
    fileUrl?: string
    id?: number
    introduction?: string
    picName?: string
    spaceId?: number
    tagName?: string
  }

  type User = {
    allowFollow?: number
    allowMultiDeviceLogin?: number
    allowPrivateChat?: number
    birthday?: string
    createTime?: string
    editTime?: string
    email?: string
    gender?: string
    homepageBg?: string
    id?: number
    interestField?: string
    isDelete?: number
    lastActiveTime?: string
    mpOpenId?: string
    personalSign?: string
    region?: string
    showFansList?: number
    showFollowList?: number
    themePreference?: string
    updateTime?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userPassword?: string
    userProfile?: string
    userRole?: string
    userTags?: string
    visibilitySetting?: string
  }

  type UserAddRequest = {
    birthday?: string
    gender?: string
    homepageBg?: string
    interestField?: string
    lastActiveTime?: string
    personalSign?: string
    region?: string
    themePreference?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
    userTags?: string
    visibilitySetting?: string
  }

  type UserChangeEmailRequest = {
    code?: string
    newEmail?: string
  }

  type UserDestroySecureRequest = {
    code?: string
    userPassword?: string
  }

  type UserExportRequest = {
    endTime?: string
    startTime?: string
    type?: number
  }

  type UserFollowsAddRequest = {
    followStatus?: number
    followerId?: number
    followingId?: number
  }

  type UserFollowsIsFollowsRequest = {
    followerId?: number
    followingId?: number
  }

  type UserfollowsQueryRequest = {
    current?: number
    followerId?: number
    followingId?: number
    pageSize?: number
    searchType?: number
    userAccountKeyword?: string
    userNameKeyword?: string
    userProfileKeyword?: string
  }

  type UserHighestScoreVO = {
    classicModeScore?: number
    noWallModeScore?: number
    speedModeScore?: number
  }

  type UserLoginRequest = {
    accountOrEmail?: string
    serververifycode?: string
    userPassword?: string
    verifyCode?: string
  }

  type UserModifyPassWord = {
    checkPassword?: string
    id?: number
    newPassword?: string
    oldPassword?: string
  }

  type UserMultiDeviceLoginUpdateRequest = {
    allowMultiDeviceLogin?: number
    userId?: number
  }

  type UserPublicVO = {
    allowFollow?: number
    allowPrivateChat?: number
    birthday?: string
    createTime?: string
    gender?: string
    homepageBg?: string
    id?: number
    personalSign?: string
    region?: string
    showFansList?: number
    showFollowList?: number
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
    userTags?: string
  }

  type UserQueryRequest = {
    current?: number
    gender?: string
    homepageBg?: string
    id?: number
    interestField?: string
    pageSize?: number
    personalSign?: string
    region?: string
    sortField?: string
    sortOrder?: string
    themePreference?: string
    userAccount?: string
    userName?: string
    userProfile?: string
    userRole?: string
    userTags?: string
    visibilitySetting?: string
  }

  type UserRegisterRequest = {
    checkPassword?: string
    code?: string
    email?: string
    userPassword?: string
  }

  type UserResetPasswordRequest = {
    checkPassword?: string
    code?: string
    email?: string
    newPassword?: string
  }

  type UserSearchRecord = {
    createTime?: string
    id?: number
    isDelete?: number
    keyword?: string
    searchTime?: string
    type?: string
    updateTime?: string
    userId?: number
  }

  type UserUnbanRequest = {
    isUnban?: boolean
    userId?: number
  }

  type UserUpdatePermissionsRequest = {
    allowFollow?: number
    allowPrivateChat?: number
    showFansList?: number
    showFollowList?: number
    userId?: number
  }

  type UserUpdateRequest = {
    birthday?: string
    gender?: string
    homepageBg?: string
    id?: number
    interestField?: string
    lastActiveTime?: string
    personalSign?: string
    region?: string
    themePreference?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
    userTags?: string
    visibilitySetting?: string
  }

  type UserVO = {
    allowFollow?: number
    allowPrivateChat?: number
    createTime?: string
    email?: string
    fansCount?: number
    followCount?: number
    homepageBg?: string
    id?: number
    showFansList?: number
    showFollowList?: number
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserWxBindRequest = {
    code?: string
  }

  type UserWxLoginRequest = {
    code?: string
  }

  type ViewRecordAddRequest = {
    targetId?: number
    targetType?: number
    userId?: number
    viewDuration?: number
  }

  type ViewRecordQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetType?: number
  }

  type ViewRecordVO = {
    createTime?: string
    id?: number
    targetAuthorAvatar?: string
    targetAuthorUsername?: string
    targetCover?: string
    targetId?: number
    targetTitle?: string
    targetType?: number
    userId?: number
    viewDuration?: number
  }

  type WeiYan = {
    content?: string
    createTime?: string
    id?: number
    isDelete?: number
    isPublic?: number
    likeCount?: number
    loveBoardId?: number
    source?: number
    type?: string
    updateTime?: string
    userId?: number
  }

  type YoloDetection = {
    confidence?: number
    height?: number
    label?: string
    width?: number
    x?: number
    y?: number
  }

  type YoloResponseDTO = {
    annotatedImageBase64?: string
    detections?: YoloDetection[]
  }
}
