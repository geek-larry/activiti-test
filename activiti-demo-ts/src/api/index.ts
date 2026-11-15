import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 每个请求带上当前操作人(X-Current-User 头)
api.interceptors.request.use((config) => {
  const user = localStorage.getItem('currentUser') || 'zhangsan'
  config.headers['X-Current-User'] = user
  return config
})

export default api

// -------- 类型 --------
export interface SysUser {
  id: number
  username: string
  displayName: string
  role: string
}

export interface StartApprovalRequest {
  applicant: string
  leaveType: string
  days: number
  reason: string
  firstReviewers: string[]
  secondReviewers: string[]
}

export interface TaskResponse {
  taskId: string
  taskName: string
  assignee: string
  businessCode: string
  applicant: string
  leaveType: string
  days: number
  reason: string
  processInstanceId: string
  createTime: string
}

export interface CompleteTaskRequest {
  approved: boolean
  comment: string
}

export interface TrackItem {
  activityName: string
  assignee: string
  endTime: string
  comment: string
  outcome: string
}

export interface ApprovalView {
  businessCode: string
  applicant: string
  leaveType: string
  days: number
  reason: string
  status: string
  actualFirstApprover: string
  actualSecondApprover: string
  processInstanceId: string
  createTime: string
  tracks: TrackItem[]
}

// -------- API 方法(后端统一返回 {code,message,data},这里取 data) --------
const pick = <T>(p: Promise<any>): Promise<T> => p.then((r) => r.data.data as T)

export const getUsers = () => pick<SysUser[]>(api.get('/users'))
export const startApproval = (req: StartApprovalRequest) =>
  pick<string>(api.post('/approval/start', req))
export const getTasks = (assignee: string) =>
  pick<TaskResponse[]>(api.get('/tasks', { params: { assignee } }))
export const getTaskDetail = (taskId: string) =>
  pick<TaskResponse>(api.get(`/tasks/${taskId}`))
export const completeTask = (taskId: string, req: CompleteTaskRequest) =>
  api.post(`/tasks/${taskId}/complete`, req)
export const getApprovalDetail = (businessCode: string) =>
  pick<ApprovalView>(api.get(`/approval/${businessCode}`))
