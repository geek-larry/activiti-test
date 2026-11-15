import { useEffect, useState } from 'react'
import { Card, Table, Button, Space, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getTasks, type TaskResponse } from '../api'
import { useUser } from '../context/UserContext'

export default function TaskList() {
  const [tasks, setTasks] = useState<TaskResponse[]>([])
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { currentUser } = useUser()

  const load = () => {
    setLoading(true)
    getTasks(currentUser)
      .then(setTasks)
      .catch(() => message.error('加载待办失败'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUser])

  const columns = [
    { title: '任务节点', dataIndex: 'taskName', key: 'taskName' },
    { title: '申请单号', dataIndex: 'businessCode', key: 'businessCode' },
    { title: '申请人', dataIndex: 'applicant', key: 'applicant' },
    { title: '请假类型', dataIndex: 'leaveType', key: 'leaveType' },
    { title: '天数', dataIndex: 'days', key: 'days' },
    { title: '事由', dataIndex: 'reason', key: 'reason', ellipsis: true },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: TaskResponse) => (
        <Space>
          <Button type="primary" size="small" onClick={() => navigate(`/task/${record.taskId}`)}>
            审批
          </Button>
          <Button size="small" onClick={() => navigate(`/approval/${record.businessCode}`)}>
            详情
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <Card
      title={`我的待办(${currentUser})`}
      extra={
        <Button icon={<ReloadOutlined />} onClick={load}>
          刷新
        </Button>
      }
    >
      <Table
        rowKey="taskId"
        columns={columns}
        dataSource={tasks}
        loading={loading}
        pagination={false}
        locale={{ emptyText: '暂无待办任务' }}
      />
    </Card>
  )
}
