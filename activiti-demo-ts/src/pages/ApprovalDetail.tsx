import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Descriptions, Timeline, Spin, Tag, message } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { getApprovalDetail, type ApprovalView } from '../api'

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING_FIRST: { label: '待初审', color: 'processing' },
  PENDING_SECOND: { label: '待复审', color: 'processing' },
  APPROVED: { label: '已通过', color: 'success' },
  REJECTED: { label: '已驳回', color: 'error' },
}

export default function ApprovalDetail() {
  const { businessCode } = useParams<{ businessCode: string }>()
  const [data, setData] = useState<ApprovalView | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!businessCode) return
    setLoading(true)
    getApprovalDetail(businessCode)
      .then(setData)
      .catch(() => message.error('加载详情失败'))
      .finally(() => setLoading(false))
  }, [businessCode])

  if (loading) return <Spin />
  if (!data) return <div>申请单不存在</div>

  const status = STATUS_MAP[data.status] || { label: data.status, color: 'default' }

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <Card title={`申请单详情 - ${data.businessCode}`} style={{ marginBottom: 16 }}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="状态">
            <Tag color={status.color}>{status.label}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="申请人">{data.applicant}</Descriptions.Item>
          <Descriptions.Item label="请假类型">{data.leaveType}</Descriptions.Item>
          <Descriptions.Item label="天数">{data.days}</Descriptions.Item>
          <Descriptions.Item label="事由">{data.reason}</Descriptions.Item>
          <Descriptions.Item label="实际初审人">
            {data.actualFirstApprover || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="实际复审人">
            {data.actualSecondApprover || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">{data.createTime}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="审批轨迹">
        <Timeline
          items={data.tracks.map((t, idx) => {
            const isReject = t.outcome === '驳回'
            const isPass = t.outcome === '通过'
            const icon = isReject ? (
              <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
            ) : isPass ? (
              <CheckCircleOutlined style={{ color: '#52c41a' }} />
            ) : (
              <ClockCircleOutlined style={{ color: '#1677ff' }} />
            )
            return {
              key: idx,
              dot: icon,
              children: (
                <div>
                  <strong>{t.activityName}</strong>
                  {t.assignee && ` - ${t.assignee}`}
                  {t.outcome && (
                    <Tag
                      color={isReject ? 'error' : isPass ? 'success' : 'default'}
                      style={{ marginLeft: 8 }}
                    >
                      {t.outcome}
                    </Tag>
                  )}
                  {t.endTime && (
                    <div style={{ color: '#999', fontSize: 12 }}>{t.endTime}</div>
                  )}
                  {t.comment && <div style={{ marginTop: 4 }}>{t.comment}</div>}
                </div>
              ),
            }
          })}
        />
      </Card>
    </div>
  )
}
