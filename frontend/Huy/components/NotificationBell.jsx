import { useCallback, useEffect, useState } from 'react'
import { api } from '../../QA/api'
import { formatMoment } from '../newsStatus'

const TYPE_LABEL = {
  SUBMITTED: 'Có bài mới nộp',
  CHANGES_REQUESTED: 'Bài bị trả lại',
  SCHEDULED: 'Bài đã được duyệt',
  PUBLISHED: 'Bài đã lên trang',
  ARCHIVED: 'Bài đã bị gỡ',
}

const panelStyle = {
  position: 'absolute', right: 0, top: '2rem', zIndex: 20, width: 'min(28rem, 90vw)',
  maxHeight: '60vh', overflowY: 'auto', padding: '0.75rem',
  border: '1px solid currentColor', borderRadius: '0.4rem', background: 'inherit',
}

/**
 * Chuong thong bao - AC-ED-002.
 *
 * So chua doc tu lam moi moi 30 giay, va lam moi lai moi lan mo hop thu, de tac
 * gia thay bai bi tra lai ma khong phai tai lai trang.
 */
export default function NotificationBell() {
  const [count, setCount] = useState(0)
  const [items, setItems] = useState([])
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')

  const loadCount = useCallback(async () => {
    try {
      const result = await api('/api/notifications/unread-count')
      setCount(result?.count ?? 0)
    } catch (failure) {
      // Hong hop thu khong duoc lam hong ca trang: giu nguyen so dang hien.
      setError(failure.status === 401 ? '' : failure.message)
    }
  }, [])

  useEffect(() => {
    // Hoan lan dem dau tien sang tick sau thay vi goi thang trong than effect:
    // goi thang la setState dong bo trong effect, React va eslint deu khong thich.
    const first = setTimeout(loadCount, 0)
    const timer = setInterval(loadCount, 30000)
    return () => {
      clearTimeout(first)
      clearInterval(timer)
    }
  }, [loadCount])

  const toggle = async () => {
    if (open) {
      setOpen(false)
      return
    }
    setError('')
    try {
      setItems(await api('/api/notifications'))
    } catch (failure) {
      setError(failure.message)
    }
    setOpen(true)
    loadCount()
  }

  const markRead = async (notification) => {
    if (notification.isRead) return
    try {
      const saved = await api(`/api/notifications/${notification.id}/read`, { method: 'POST' })
      setItems((current) => current.map((item) => (item.id === saved.id ? saved : item)))
      loadCount()
    } catch (failure) {
      setError(failure.message)
    }
  }

  return (
    <span style={{ position: 'relative', display: 'inline-block' }}>
      <button type="button" onClick={toggle} aria-expanded={open} aria-label="Thông báo">
        Thông báo{count > 0 ? ` (${count})` : ''}
      </button>
      {open && (
        <div className="block" style={panelStyle}>
          {error && <p className="error" role="alert">{error}</p>}
          {items.length === 0 && !error && <p>Chưa có thông báo nào.</p>}
          {items.map((item) => (
            <p key={item.id} style={{ margin: '0 0 0.6rem', opacity: item.isRead ? 0.6 : 1 }}>
              <b>{TYPE_LABEL[item.type] || item.type}</b>
              {' — '}
              <small>{formatMoment(item.createdAt)}</small>
              <br />
              {item.message}
              {!item.isRead && (
                <>
                  {' '}
                  <button type="button" onClick={() => markRead(item)}>Đánh dấu đã đọc</button>
                </>
              )}
            </p>
          ))}
        </div>
      )}
    </span>
  )
}
