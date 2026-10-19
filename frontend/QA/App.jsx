import { useEffect, useState } from 'react'
import { api } from './api'
import useList from '../Huy/useList'
import useAction from '../Huy/useAction'
import Header from '../Long/components/Header'
import Menu from '../Long/components/Menu'
import Login from './pages/Login'
import Dashboard from '../Huy/pages/Dashboard'
import Categories from '../Long/pages/Categories'
import News from '../Huy/pages/News'
import Users from './pages/Users'
import Settings from '../Long/pages/Settings'

const ADMIN_MENU = ['Dashboard', 'Category', 'News', 'Users', 'Settings']
const STAFF_MENU = ['Dashboard', 'News', 'Settings']

export default function App() {
  const [session, setSession] = useState({ loading: true, user: null, error: '' })

  useEffect(() => {
    const controller = new AbortController()
    api('/api/auth/me', { signal: controller.signal })
      .then((user) => setSession({ loading: false, user, error: '' }))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setSession({ loading: false, user: null, error: error.status === 401 ? '' : error.message })
        }
      })
    const expire = () => setSession({ loading: false, user: null, error: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.' })
    window.addEventListener('session-expired', expire)
    return () => {
      controller.abort()
      window.removeEventListener('session-expired', expire)
    }
  }, [])

  if (session.loading) return <p role="status">Đang kiểm tra phiên đăng nhập...</p>
  if (!session.user) {
    return <Login serverError={session.error} onLogin={(user) => setSession({ loading: false, user, error: '' })} />
  }

  return (
    <AuthenticatedApp
      key={`${session.user.id}:${session.user.role}`}
      user={session.user}
      onUserChange={(user) => setSession({ loading: false, user, error: '' })}
      onLogout={() => setSession({ loading: false, user: null, error: '' })}
    />
  )
}

function AuthenticatedApp({ user, onUserChange, onLogout }) {
  const [page, setPage] = useState('Dashboard')
  const accounts = useList('/api/accounts', user.role === 1)
  const authors = useList('/api/accounts/lookup', user.role !== 1)
  const categories = useList('/api/categories')
  const news = useList('/api/news')
  const logout = useAction()
  const menu = user.role === 1 ? ADMIN_MENU : STAFF_MENU

  const saveProfile = async (form) => {
    const saved = await api('/api/profile', { method: 'PUT', body: { name: form.name, email: form.email } })
    onUserChange(saved)
    if (saved.role === 1) await accounts.reload()
    else await authors.reload()
    return saved
  }

  const signOut = () => logout.run(async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' })
    } catch (error) {
      if (error.status !== 401) throw error
    }
    onLogout()
  })

  return (
    <div className="layout">
      <Header user={user} onLogout={signOut} busy={logout.pending} />
      <Menu items={menu} current={page} onChange={setPage} />
      <main className="block content">
        {logout.error && <p className="error" role="alert">{logout.error}</p>}
        {page === 'Dashboard' && <Dashboard />}
        {page === 'Category' && user.role === 1 && <Categories store={categories} />}
        {page === 'News' && (
          <News
            store={news}
            categories={categories.items}
            accounts={user.role === 1 ? accounts.items : authors.items}
            categoryStore={categories}
            authorStore={user.role === 1 ? accounts : authors}
          />
        )}
        {page === 'Users' && user.role === 1 && (
          <Users store={accounts} onSaved={(saved) => {
            if (saved.id === user.id) onUserChange(saved)
          }} />
        )}
        {page === 'Settings' && <Settings key={user.id} user={user} onSave={saveProfile} />}
      </main>
    </div>
  )
}
