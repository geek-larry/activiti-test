import { useEffect, useState } from 'react'
import { Card, Form, Input, InputNumber, Select, Button, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getUsers, startApproval, type SysUser } from '../api'
import { useUser } from '../context/UserContext'

const { TextArea } = Input
const LEAVE_TYPES = ['年假', '事假', '病假', '调休']

export default function StartApproval() {
  const [form] = Form.useForm()
  const [users, setUsers] = useState<SysUser[]>([])
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()
  const { currentUser } = useUser()

  useEffect(() => {
    getUsers().then(setUsers).catch(() => message.error('加载用户失败'))
  }, [])

  const onFinish = async (values: any) => {
    setSubmitting(true)
    try {
      const businessCode = await startApproval({
        applicant: values.applicant,
        leaveType: values.leaveType,
        days: values.days,
        reason: values.reason,
        firstReviewers: values.firstReviewers,
        secondReviewers: values.secondReviewers,
      })
      message.success(`发起成功,单号:${businessCode}`)
      form.resetFields()
      navigate('/')
    } catch {
      message.error('发起失败')
    } finally {
      setSubmitting(false)
    }
  }

  const userOptions = users.map((u) => ({
    value: u.username,
    label: `${u.displayName}(${u.role})`,
  }))

  return (
    <Card title="发起请假审批" style={{ maxWidth: 720, margin: '0 auto' }}>
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{ applicant: currentUser, leaveType: '年假', days: 1 }}
      >
        <Form.Item name="applicant" label="申请人" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="leaveType" label="请假类型" rules={[{ required: true }]}>
          <Select options={LEAVE_TYPES.map((t) => ({ value: t, label: t }))} />
        </Form.Item>
        <Form.Item name="days" label="请假天数" rules={[{ required: true }]}>
          <InputNumber min={0.5} step={0.5} style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="reason" label="事由" rules={[{ required: true }]}>
          <TextArea rows={3} maxLength={200} showCount />
        </Form.Item>
        <Form.Item
          name="firstReviewers"
          label="初审人(或签:任一人审批即流转)"
          rules={[{ required: true, message: '至少选择1个初审人' }]}
        >
          <Select mode="multiple" placeholder="选择初审人" options={userOptions} />
        </Form.Item>
        <Form.Item
          name="secondReviewers"
          label="复审人(或签:任一人审批即流转)"
          rules={[{ required: true, message: '至少选择1个复审人' }]}
        >
          <Select mode="multiple" placeholder="选择复审人" options={userOptions} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting}>
            提交申请
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
