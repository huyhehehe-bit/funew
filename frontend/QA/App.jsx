import { useEffect, useState } from 'react'
import { api } from './api'
import useList from '../Huy/useList'
import useAction from '../Huy/useAction'
import Header from '../Long/components/Header'
import Menu from '../Long/components/Menu'
import Logo from '../Long/components/Logo'
import Login from './pages/Login'
import Dashboard from '../Huy/pages/Dashboard'
import Categories from '../Long/pages/Categories'
import News from '../Huy/pages/News'
import Users from './pages/Users'
import Settings from '../Long/pages/Settings'
import EditorialRules from '../Huy/pages/EditorialRules'
import Home from '../Huy/pages/Home'
import Article from '../Huy/pages/Article'

const ADMIN_MENU = ['Dashboard', 'Category', 'News', 'Users', 'Rules', 'Settings']
const STAFF_MENU = ['Dashboard', 'News', 'Settings']
const ROLE_BY_NUM = { 1: 'ADMIN', 2: 'EDITOR', 3: 'CONTRIBUTOR', 4: 'READER' }
// Ba vai tro nay dung back-office; READER va khach di qua trang doc bao.
const BACK_OFFICE = ['ADMIN', 'EDITOR', 'CONTRIBUTOR']

const roleOf = (u) => (u ? (u.roleCode || ROLE_BY_NUM[u.role] || '') : '')

export default function App() {
  const [session, setSession] = useState({ loading: true, user: null, error: '' })
  const [showLogin, setShowLogin] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    api('/api/auth/me', { signal: controller.signal })
      .then((user) => setSession({ loading: false, user, error: '' }))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setSession((current) => ({ loading: false, user: null, error: error.status === 401 ? current.error : error.message }))
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

  const user = session.user

  // Nhan vien va quan tri -> back-office, khong di qua trang doc bao.
  if (user && BACK_OFFICE.includes(roleOf(user))) {
    return (
      <AuthenticatedApp
        key={`${user.id}:${roleOf(user)}`}
        user={user}
        onUserChange={(u) => setSession({ loading: false, user: u, error: '' })}
        onLogout={(error = '') => { setSession({ loading: false, user: null, error }); setShowLogin(false) }}
      />
    )
  }

  // Login la mot view mo tu nut Dang nhap tren trang doc bao.
  if (showLogin) {
    return (
      <Login
        serverError={session.error}
        onLogin={(u) => { setSession({ loading: false, user: u, error: '' }); setShowLogin(false) }}
        onCancel={() => setShowLogin(false)}
      />
    )
  }

  // Khach chua dang nhap hoac doc gia: trang doc bao cong khai.
  return (
    <PublicSite
      user={user}
      onLoginClick={() => setShowLogin(true)}
      onLogout={() => setSession({ loading: false, user: null, error: '' })}
    />
  )
}

function PublicSite({ user, onLoginClick, onLogout }) {
  const news = useList('/api/news')
  const categories = useList('/api/categories')
  const [view, setView] = useState({ name: 'home' })
  // Diem cua chinh minh, nho lai trong phien de khoi phai tai lai ca danh sach.
  const [myScores, setMyScores] = useState({})
  const rating = useAction()
  const logout = useAction()

  const categoryName = (id) => categories.items.find((c) => c.id === id)?.name || 'Tin tức'
  const canRate = roleOf(user) === 'READER'

  const rate = (article, score) => rating.run(async () => {
    const saved = await api(`/api/news/${article.id}/rating`, { method: 'PUT', body: { score } })
    setMyScores((cur) => ({ ...cur, [article.id]: saved.score }))
    news.replace({ ...article, ratingCount: saved.ratingCount, ratingAvg: saved.ratingAvg })
  })

  const signOut = () => logout.run(async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' })
    } catch {
      // Dang xuat cuc bo du server tu choi: ve trang khach.
    }
    onLogout()
  })

  const current = view.name === 'article' ? news.items.find((a) => a.id === view.id) : null

  return (
    <div className="public">
      <header className="public-head">
        <button type="button" className="brand" onClick={() => setView({ name: 'home' })}>
          <Logo />
        </button>
        <div className="public-auth">
          {user ? (
            <>
              Xin chào, <b>{user.name}</b>{' '}
              <button type="button" onClick={signOut} disabled={logout.pending}>Đăng xuất</button>
            </>
          ) : (
            <button type="button" onClick={onLoginClick}>Đăng nhập</button>
          )}
        </div>
      </header>
      <main className="public-main">
        {view.name === 'article' && current ? (
          <Article
            article={current}
            categoryName={categoryName}
            canRate={canRate}
            myScore={myScores[current.id]}
            rating={rating.pending}
            onRate={rate}
            onBack={() => setView({ name: 'home' })}
          />
        ) : (
          <Home
            articles={news.items}
            categoryName={categoryName}
            loading={news.loading}
            error={news.error}
            onOpen={(a) => setView({ name: 'article', id: a.id })}
          />
        )}
        {rating.error && <p className="error" role="alert">{rating.error}</p>}
      </main>
    </div>
  )
}

function AuthenticatedApp({ user, onUserChange, onLogout }) {
  const [page, setPage] = useState('Dashboard')
  const isAdmin = user.roleCode ? user.roleCode === 'ADMIN' : user.role === 1
  const accounts = useList('/api/accounts', isAdmin)
  const authors = useList('/api/accounts/lookup', !isAdmin)
  const categories = useList('/api/categories')
  const news = useList('/api/news')
  const logout = useAction()
  const menu = isAdmin ? ADMIN_MENU : STAFF_MENU

  const saveProfile = async (form) => {
    const saved = await api('/api/profile', { method: 'PUT', body: { name: form.name, email: form.email } })
    onUserChange(saved)
    if (saved.roleCode ? saved.roleCode === 'ADMIN' : saved.role === 1) await accounts.reload()
    else await authors.reload()
    return saved
  }

  const signOut = () => logout.run(async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' })
    } catch (error) {
      onLogout(error.status === 401 ? '' : error.message)
      return
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
        {page === 'Category' && isAdmin && <Categories store={categories} />}
        {page === 'News' && (
          <News
            store={news}
            categories={categories.items}
            accounts={isAdmin ? accounts.items : authors.items}
            categoryStore={categories}
            authorStore={isAdmin ? accounts : authors}
            user={user}
          />
        )}
        {page === 'Users' && isAdmin && (
          <Users store={accounts} onSaved={(saved) => {
            if (saved.id === user.id) onUserChange(saved)
          }} />
        )}
        {page === 'Rules' && isAdmin && <EditorialRules />}
        {page === 'Settings' && <Settings key={user.id} user={user} onSave={saveProfile} />}
      </main>
    </div>
  )
}
