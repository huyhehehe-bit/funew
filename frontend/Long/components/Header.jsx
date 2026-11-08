import Logo from './Logo'
import { ROLES } from '../../QA/data'
import NotificationBell from '../../Huy/components/NotificationBell'

export default function Header({ user, onLogout, busy, onLogo }) {
  return (
    <header className="block header">
      {onLogo
        ? <button type="button" className="brand" onClick={onLogo} title="Xem trang đọc báo"><Logo /></button>
        : <Logo />}
      <span>
        {user.name} ({ROLES[user.role]}){' '}
        <NotificationBell />{' '}
        <button onClick={onLogout} disabled={busy}>{busy ? 'Đang đăng xuất...' : 'Logout'}</button>
      </span>
    </header>
  )
}
