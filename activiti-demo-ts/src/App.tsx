import { Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu } from 'antd'
import { HomeOutlined, PlusOutlined } from '@ant-design/icons'
import UserSwitcher from './components/UserSwitcher'
import StartApproval from './pages/StartApproval'
import TaskList from './pages/TaskList'
import TaskDetail from './pages/TaskDetail'
import ApprovalDetail from './pages/ApprovalDetail'

const { Header, Sider, Content } = Layout

export default function App() {
  const navigate = useNavigate()
  const location = useLocation()

  const selectedKey = location.pathname === '/start' ? 'start' : 'tasks'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          padding: '0 24px',
        }}
      >
        <h2 style={{ margin: 0 }}>Activiti7 审批工作流 Demo</h2>
        <UserSwitcher />
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            style={{ height: '100%', borderRight: 0 }}
            onClick={({ key }) => navigate(key === 'start' ? '/start' : '/')}
            items={[
              { key: 'tasks', icon: <HomeOutlined />, label: '我的待办' },
              { key: 'start', icon: <PlusOutlined />, label: '发起审批' },
            ]}
          />
        </Sider>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Routes>
            <Route path="/" element={<TaskList />} />
            <Route path="/start" element={<StartApproval />} />
            <Route path="/task/:taskId" element={<TaskDetail />} />
            <Route path="/approval/:businessCode" element={<ApprovalDetail />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}
