import { useState } from 'react'
import Logo from '../../Long/components/Logo'
import { api } from '../api'
import useAction from '../../Huy/useAction'

export default function Login({ onLogin, serverError }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const { pending, error, run } = useAction()

  const submit = (e) => {
    e.preventDefault()
    run(async () => {
      const user = await api('/api/auth/login', { method: 'POST', body: { username: username.trim(), password } })
      onLogin(user)
    })
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
        <p className="hint">Admin / Admin - hoac a@fu.edu.vn / 123</p>
      </form>
    </div>
  )
}
