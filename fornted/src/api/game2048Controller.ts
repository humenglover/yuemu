// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getUserHighestScore GET /api/game2048/highest */
export async function getUserHighestScoreUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseInt_>('/api/game2048/highest', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getUserGameHistory GET /api/game2048/history */
export async function getUserGameHistoryUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserGameHistoryUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject_>('/api/game2048/history', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getRankingList GET /api/game2048/ranking */
export async function getRankingListUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRankingListUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListGame2048RecordVO_>('/api/game2048/ranking', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** saveGame2048Record POST /api/game2048/save */
export async function saveGame2048RecordUsingPost(
  body: API.SaveGameRecordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseGame2048Record_>('/api/game2048/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
