import { Select, Space, Typography } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { useUser } from '../context/UserContext'

const DEMO_USERS = [
  { username: 'zhangsan', displayName: '张三', role: '初审人' },
  { username: 'lisi', displayName: '李四', role: '初审人' },
  { username: 'wangwu', displayName: '王五', role: '复审人' },
]

export default function UserSwitcher() {
  const { currentUser, setCurrentUser } = useUser()

  return (
    <Space>
      <UserOutlined />
      <Typography.Text>当前用户:</Typography.Text>
      <Select
        value={currentUser}
        onChange={setCurrentUser}
        style={{ width: 160 }}
        options={DEMO_USERS.map((u) => ({
          value: u.username,
          label: `${u.displayName}（${u.role}）`,
        }))}
      />
    </Space>
  )
}
