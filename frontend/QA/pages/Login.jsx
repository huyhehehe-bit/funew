import { useState } from 'react'
import Logo from '../../Long/components/Logo'
import { api } from '../api'
import useAction from '../../Huy/useAction'

const emptySignUp = { fullName: '', email: '', username: '', password: '' }

export default function Login({ onLogin, serverError }) {
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [form, setForm] = useState(emptySignUp)
  const { pending, error, run, clearError } = useAction()

  const submit = (e) => {
    e.preventDefault()
    run(async () => {
      const user = await api('/api/auth/login', { method: 'POST', body: { username: username.trim(), password } })
      onLogin(user)
    })
  }

  // Dang ky xong thi backend tra luon accessToken, nen vao thang ung dung,
  // khong bat go lai mat khau.
  const register = (e) => {
    e.preventDefault()
    run(async () => {
      const user = await api('/api/auth/register', {
        method: 'POST',
        body: {
          fullName: form.fullName.trim(),
          email: form.email.trim(),
          username: form.username.trim() || null,
          password: form.password,
        },
      })
      onLogin(user)
    })
  }

  const switchTo = (next) => {
    clearError()
    setMode(next)
  }

  if (mode === 'register') {
    return (
      <div className="login">
        <form className="block" onSubmit={register}>
          <Logo />
          <h2>Đăng ký</h2>
          <p className="hint">Tài khoản đăng ký là độc giả: đọc và chấm điểm bài viết.</p>
          <label>
            Họ tên
            <input required value={form.fullName} disabled={pending}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          </label>
          <label>
            Email
            <input type="email" required autoComplete="email" value={form.email} disabled={pending}
              onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </label>
          <label>
            Tên đăng nhập (để trống thì dùng email)
            <input autoComplete="username" value={form.username} disabled={pending}
              onChange={(e) => setForm({ ...form, username: e.target.value })} />
          </label>
          <label>
            Mật khẩu (ít nhất 6 ký tự)
            <input type="password" required minLength={6} autoComplete="new-password"
              value={form.password} disabled={pending}
              onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <button type="submit" disabled={pending}>{pending ? 'Đang tạo tài khoản...' : 'Đăng ký'}</button>
          <p className="hint">
            Đã có tài khoản?{' '}
            <button type="button" onClick={() => switchTo('login')} disabled={pending}>Đăng nhập</button>
          </p>
        </form>
      </div>
    )
  }

  return (
    <div className="login">
      <form className="block" onSubmit={submit}>
        <Logo />
        <h2>Login</h2>
        <label>
          Username / Email
          <input required autoComplete="username" value={username} disabled={pending} onChange={(e) => setUsername(e.target.value)} />
        </label>
        <label>
          Password
          <input
            type="password"
            required
            autoComplete="current-password"
            disabled={pending}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {(error || serverError) && <p className="error" role="alert">{error || serverError}</p>}
        <button type="submit" disabled={pending}>{pending ? 'Đang đăng nhập...' : 'Login'}</button>
        <p className="hint">Admin / Admin hoặc admin@fu.edu.vn / Admin</p>
        <p className="hint">
          Chưa có tài khoản?{' '}
          <button type="button" onClick={() => switchTo('register')} disabled={pending}>Đăng ký làm độc giả</button>
        </p>
      </form>
    </div>
  )
}
