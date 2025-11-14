import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// WebSocket连接状态
export interface WebSocketStatus {
  xianyuAccountId: number;
  connected: boolean;
  status: string;
  cookieStatus?: number;      // Cookie状态 1:有效 2:过期 3:失效
  cookieText?: string;        // Cookie值
  websocketToken?: string;    // WebSocket Token
  tokenExpireTime?: number;   // Token过期时间戳（毫秒）
}

// 获取连接状态
export function getConnectionStatus(accountId: number) {
  return request<WebSocketStatus>({
    url: '/websocket/status',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}

// 启动连接
export function startConnection(accountId: number) {
  return request({
    url: '/websocket/start',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}

// 停止连接
export function stopConnection(accountId: number) {
  return request({
    url: '/websocket/stop',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}
