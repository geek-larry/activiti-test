import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Descriptions, Form, Radio, Input, Button, message, Spin, Space } from 'antd'
import { getTaskDetail, completeTask, type TaskResponse } from '../api'

const { TextArea } = Input

export default function TaskDetail() {
  const { taskId } = useParams<{ taskId: string }>()
  const navigate = useNavigate()
  const [task, setTask] = useState<TaskResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    if (!taskId) return
    setLoading(true)
    getTaskDetail(taskId)
      .then(setTask)
      .catch(() => message.error('加载任务失败'))
      .finally(() => setLoading(false))
  }, [taskId])

  const onFinish = async (values: any) => {
    setSubmitting(true)
    try {
      await completeTask(taskId!, {
        approved: values.approved,
        comment: values.comment || '',
      })
      message.success(values.approved ? '已通过' : '已驳回')
      navigate('/')
    } catch (err: any) {
      message.error(err?.message || '操作失败,可能任务已被他人处理')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <Spin />
  if (!task) return <div>任务不存在</div>

  return (
    <Card title="审批任务" style={{ maxWidth: 720, margin: '0 auto' }}>
      <Descriptions column={1} bordered size="small" style={{ marginBottom: 24 }}>
        <Descriptions.Item label="任务节点">{task.taskName}</Descriptions.Item>
        <Descriptions.Item label="申请单号">{task.businessCode}</Descriptions.Item>
        <Descriptions.Item label="申请人">{task.applicant}</Descriptions.Item>
        <Descriptions.Item label="请假类型">{task.leaveType}</Descriptions.Item>
        <Descriptions.Item label="天数">{task.days}</Descriptions.Item>
        <Descriptions.Item label="事由">{task.reason}</Descriptions.Item>
      </Descriptions>

      <Form form={form} layout="vertical" onFinish={onFinish} initialValues={{ approved: true }}>
        <Form.Item name="approved" label="审批结果" rules={[{ required: true }]}>
          <Radio.Group>
            <Radio value={true}>通过</Radio>
            <Radio value={false}>驳回</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="comment" label="审批备注">
          <TextArea rows={3} maxLength={200} showCount placeholder="请输入审批意见" />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={submitting}>
              提交审批
            </Button>
            <Button onClick={() => navigate('/')}>取消</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  )
}
