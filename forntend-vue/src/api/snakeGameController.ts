// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getUserAllHighestScores GET /api/snake/highest/all */
export async function getUserAllHighestScoresUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseUserHighestScoreVO_>('/api/snake/highest/all', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getRankingList POST /api/snake/ranking */
export async function getRankingListUsingPost(
  body: API.GameRankingRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSnakeGameRecord_>('/api/snake/ranking', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** saveGameRecord POST /api/snake/save */
export async function saveGameRecordUsingPost(
  body: API.SaveGameRecordRequest1,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSnakeGameRecord_>('/api/snake/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
