import Logo from './Logo'
import { ROLES } from '../../QA/data'
import NotificationBell from '../../Huy/components/NotificationBell'

export default function Header({ user, onLogout, busy }) {
  return (
    <header className="block header">
      <Logo />
      <span>
        {user.name} ({ROLES[user.role]}){' '}
        <NotificationBell />{' '}
        <button onClick={onLogout} disabled={busy}>{busy ? 'Đang đăng xuất...' : 'Logout'}</button>
      </span>
    </header>
  )
}
