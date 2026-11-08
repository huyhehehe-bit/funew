import { useState } from 'react'
import Logo from '../../Long/components/Logo'
import { api } from '../api'
import useAction from '../../Huy/useAction'
import { required, isEmail } from '../../Huy/validate'

const emptySignUp = { fullName: '', email: '', username: '', password: '' }

// Kiem tra form Dang ky phia trinh duyet. Khop @NotBlank/@Email/@Size o
// RegisterRequest.java ben backend de bao loi giong nhau.
function registerErrors(f) {
  const e = {}
  if (!required(f.fullName)) e.fullName = 'Họ tên không được để trống'
  if (!isEmail(f.email)) e.email = 'Email không đúng định dạng'
  if ((f.password || '').length < 6) e.password = 'Mật khẩu phải ít nhất 6 ký tự'
  return e
}

export default function Login({ onLogin, serverError, onCancel }) {
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [form, setForm] = useState(emptySignUp)
  const { pending, error, run, clearError } = useAction()
  const regErrs = registerErrors(form)

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
    if (Object.keys(registerErrors(form)).length > 0) return
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
            <input value={form.fullName} disabled={pending} aria-invalid={!!regErrs.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            {regErrs.fullName && <p className="field-error" role="alert">{regErrs.fullName}</p>}
          </label>
          <label>
            Email
            <input type="email" autoComplete="email" value={form.email} disabled={pending} aria-invalid={!!regErrs.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })} />
            {regErrs.email && <p className="field-error" role="alert">{regErrs.email}</p>}
          </label>
          <label>
            Tên đăng nhập (để trống thì dùng email)
            <input autoComplete="username" value={form.username} disabled={pending}
              onChange={(e) => setForm({ ...form, username: e.target.value })} />
          </label>
          <label>
            Mật khẩu (ít nhất 6 ký tự)
            <input type="password" minLength={6} autoComplete="new-password"
              value={form.password} disabled={pending} aria-invalid={!!regErrs.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })} />
            {regErrs.password && <p className="field-error" role="alert">{regErrs.password}</p>}
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <button type="submit" disabled={pending || Object.keys(regErrs).length > 0}>{pending ? 'Đang tạo tài khoản...' : 'Đăng ký'}</button>
          <p className="hint">
            Đã có tài khoản?{' '}
            <button type="button" onClick={() => switchTo('login')} disabled={pending}>Đăng nhập</button>
          </p>
          {onCancel && (
            <p className="hint">
              <button type="button" onClick={onCancel} disabled={pending}>← Về trang đọc báo</button>
            </p>
          )}
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
        <p className="hint">
          Chưa có tài khoản?{' '}
          <button type="button" onClick={() => switchTo('register')} disabled={pending}>Đăng ký làm độc giả</button>
        </p>
        {onCancel && (
          <p className="hint">
            <button type="button" onClick={onCancel} disabled={pending}>← Về trang đọc báo</button>
          </p>
        )}
      </form>
    </div>
  )
}
