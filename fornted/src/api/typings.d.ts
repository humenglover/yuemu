declare namespace API {
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

  type BaseResponseBoolean_ = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseCreateOutPaintingTaskResponse_ = {
    code?: number
    data?: CreateOutPaintingTaskResponse
    message?: string
  }

  type BaseResponseFollowersAndFansVO_ = {
    code?: number
    data?: FollowersAndFansVO
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

  type BaseResponseListMessageVO_ = {
    code?: number
    data?: MessageVO[]
    message?: string
  }

  type BaseResponseListPictureVO_ = {
    code?: number
    data?: PictureVO[]
    message?: string
  }

  type BaseResponseListPost_ = {
    code?: number
    data?: Post[]
    message?: string
  }

  type BaseResponseListShareRecordVO_ = {
    code?: number
    data?: ShareRecordVO[]
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

  type BaseResponseMapStringString_ = {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  type BaseResponseMessageCenterVO_ = {
    code?: number
    data?: MessageCenterVO
    message?: string
  }

  type BaseResponsePageCategoryVO_ = {
    code?: number
    data?: PageCategoryVO_
    message?: string
  }

  type BaseResponsePageCommentsVO_ = {
    code?: number
    data?: PageCommentsVO_
    message?: string
  }

  type BaseResponsePageLikeRecordVO_ = {
    code?: number
    data?: PageLikeRecordVO_
    message?: string
  }

  type BaseResponsePageObject_ = {
    code?: number
    data?: PageObject_
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

  type BaseResponsePagePost_ = {
    code?: number
    data?: PagePost_
    message?: string
  }

  type BaseResponsePagePrivateChat_ = {
    code?: number
    data?: PagePrivateChat_
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

  type BaseResponsePageTagVO_ = {
    code?: number
    data?: PageTagVO_
    message?: string
  }

  type BaseResponsePageUserVO_ = {
    code?: number
    data?: PageUserVO_
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

  type BaseResponsePost_ = {
    code?: number
    data?: Post
    message?: string
  }

  type BaseResponsePrivateChat_ = {
    code?: number
    data?: PrivateChat
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

  type BaseResponseUser_ = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO_ = {
    code?: number
    data?: UserVO
    message?: string
  }

  type CategoryVO = {
    categoryName?: string
    createTime?: string
    editTime?: string
    id?: number
    type?: number
    updateTime?: string
  }

  type clearUnreadCountUsingPOSTParams = {
    /** isSender */
    isSender?: boolean
    /** targetUserId */
    targetUserId: number
  }

  type CommentsAddRequest = {
    content?: string
    parentCommentId?: number
    targetId?: number
    targetType?: number
    userId?: number
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
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetId?: number
    targetType?: number
  }

  type CommentsVO = {
    children?: CommentsVO[]
    commentId?: number
    commentUser?: CommentUserVO
    content?: string
    createTime?: string
    current?: number
    dislikeCount?: number
    likeCount?: number
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

  type deleteCategoryUsingPOSTParams = {
    /** categoryId */
    categoryId: number
  }

  type deletePostUsingPOSTParams = {
    /** id */
    id: number
  }

  type deletePrivateChatUsingPOSTParams = {
    /** privateChatId */
    privateChatId: number
  }

  type DeleteRequest = {
    id?: number
  }

  type deleteTagUsingPOSTParams = {
    /** id */
    id?: number
  }

  type EmailCodeRequest = {
    email?: string
    type?: string
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

  type getFollowAndFansCountUsingPOSTParams = {
    /** id */
    id: number
  }

  type getHotSearchKeywordsUsingGETParams = {
    /** size */
    size?: number
    /** type */
    type: string
  }

  type getLikeStatusUsingGETParams = {
    /** targetId */
    targetId: number
    /** targetType */
    targetType: number
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

  type getPictureVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getPostByIdUsingGETParams = {
    /** id */
    id: number
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

  type getTop100PictureUsingGETParams = {
    /** id */
    id: number
  }

  type getTop100PostUsingGETParams = {
    /** id */
    id: number
  }

  type getUserByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getUserSignInRecordUsingGETParams = {
    /** year */
    year?: number
  }

  type getUserVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type ImageSearchResult = {
    fromUrl?: string
    thumbUrl?: string
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

  type LikeRecordVO = {
    id?: number
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

  type LoginUserVO = {
    createTime?: string
    editTime?: string
    email?: string
    id?: number
    updateTime?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type MapStringString_ = true

  type MessageCenterVO = {
    totalUnread?: number
    unreadComments?: number
    unreadLikes?: number
    unreadShares?: number
  }

  type MessageVO = {
    content?: string
    createTime?: string
    id?: number
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

  type PageCategoryVO_ = {
    current?: number
    pages?: number
    records?: CategoryVO[]
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

  type PageLikeRecordVO_ = {
    current?: number
    pages?: number
    records?: LikeRecordVO[]
    size?: number
    total?: number
  }

  type PageObject_ = {
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

  type PagePost_ = {
    current?: number
    pages?: number
    records?: Post[]
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

  type PageRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
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

  type PageTagVO_ = {
    current?: number
    pages?: number
    records?: TagVO[]
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
    category?: string
    commentCount?: number
    createTime?: string
    editTime?: string
    id?: number
    introduction?: string
    isDelete?: number
    likeCount?: number
    name?: string
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
    category?: string
    id?: number
    introduction?: string
    name?: string
    tags?: string[]
  }

  type PictureOperation = {
    ids?: number[]
    operationType?: number
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
    picName?: string
    spaceId?: number
    tagName?: string
  }

  type PictureVO = {
    category?: string
    commentCount?: number
    createTime?: string
    editTime?: string
    id?: number
    introduction?: string
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

  type Post = {
    attachments?: PostAttachment[]
    category?: string
    commentCount?: number
    content?: string
    createTime?: string
    id?: number
    isDelete?: number
    isLiked?: number
    isShared?: number
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
    attachments?: PostAttachmentRequest[]
    category?: string
    content?: string
    tags?: string[]
    title?: string
  }

  type PostAttachment = {
    createTime?: string
    id?: number
    isDelete?: number
    marker?: string
    name?: string
    position?: number
    postId?: number
    size?: number
    sort?: number
    type?: number
    updateTime?: string
    url?: string
  }

  type PostAttachmentRequest = {
    name?: string
    size?: number
    sort?: number
    type?: number
    url?: string
  }

  type PostQueryRequest = {
    category?: string
    current?: number
    pageSize?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    status?: number
    userId?: number
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

  type reviewPostUsingPOSTParams = {
    /** id */
    id: number
    /** message */
    message?: string
    /** status */
    status: number
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
  }

  type searchTagUsingPOSTParams = {
    /** tagName */
    tagName?: string
  }

  type ShareQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    targetType?: number
  }

  type ShareRecordVO = {
    id?: number
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
    maxCount?: number
    maxSize?: number
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    totalCount?: number
    totalSize?: number
    updateTime?: string
    userId?: number
  }

  type SpaceAddRequest = {
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
    createTime?: string
    editTime?: string
    id?: number
    maxCount?: number
    maxSize?: number
    memberCount?: number
    permissionList?: string[]
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    totalCount?: number
    totalSize?: number
    updateTime?: string
    user?: UserVO
    userId?: number
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

  type updateUserAvatarUsingPOSTParams = {
    /** id */
    id?: number
  }

  type uploadPictureUsingPOSTParams = {
    categoryName?: string
    fileUrl?: string
    id?: number
    picName?: string
    spaceId?: number
    tagName?: string
  }

  type uploadPostImageUsingPOSTParams = {
    categoryName?: string
    fileUrl?: string
    id?: number
    picName?: string
    spaceId?: number
    tagName?: string
  }

  type User = {
    createTime?: string
    editTime?: string
    email?: string
    id?: number
    isDelete?: number
    updateTime?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userPassword?: string
    userProfile?: string
    userRole?: string
  }

  type UserAddRequest = {
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserChangeEmailRequest = {
    code?: string
    newEmail?: string
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
    sortField?: string
    sortOrder?: string
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

  type UserQueryRequest = {
    current?: number
    id?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userAccount?: string
    userName?: string
    userProfile?: string
    userRole?: string
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

  type UserUnbanRequest = {
    isUnban?: boolean
    userId?: number
  }

  type UserUpdateRequest = {
    id?: number
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    createTime?: string
    email?: string
    id?: number
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }
}
