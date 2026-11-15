import { createContext, useContext, useState, type ReactNode } from 'react'

interface UserContextType {
  currentUser: string
  setCurrentUser: (u: string) => void
}

const UserContext = createContext<UserContextType>({
  currentUser: 'zhangsan',
  setCurrentUser: () => {},
})

export function UserProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUserState] = useState(
    localStorage.getItem('currentUser') || 'zhangsan',
  )

  const setCurrentUser = (u: string) => {
    localStorage.setItem('currentUser', u)
    setCurrentUserState(u)
  }

  return (
    <UserContext.Provider value={{ currentUser, setCurrentUser }}>
      {children}
    </UserContext.Provider>
  )
}

export const useUser = () => useContext(UserContext)
