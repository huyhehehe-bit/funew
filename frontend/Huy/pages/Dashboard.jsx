import { useEffect, useState } from 'react'
import { api } from '../../QA/api'
import LoadStatus from '../../Long/components/LoadStatus'
import { STATUS_LABEL, formatMoment } from '../newsStatus'

export default function Dashboard() {
  const [state, setState] = useState({ loading: true, error: '', data: null })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    api('/api/dashboard', { signal: controller.signal })
      .then((data) => setState({ loading: false, error: '', data }))
      .catch((error) => {
        if (error.name !== 'AbortError') setState({ loading: false, error: error.message, data: null })
      })
    return () => controller.abort()
  }, [attempt])

  const reload = () => {
    setState({ loading: true, error: '', data: null })
    setAttempt((value) => value + 1)
  }
  const { data } = state

  return (
    <>
      <h2>Dashboard</h2>
      <LoadStatus store={{ ...state, reload }} />
      {data && (
        <>
          <div className="cards">
            <div className="block card">
              <b>{data.accountCount}</b>
              <span>Tài khoản</span>
            </div>
            <div className="block card">
              <b>{data.categoryCount}</b>
              <span>Danh mục</span>
            </div>
            <div className="block card">
              <b>{data.newsCount}</b>
              <span>Bài viết</span>
            </div>
            <div className="block card">
              <b>{data.publishedCount}</b>
              <span>Đang công khai</span>
            </div>
            <div className="block card">
              <b>{data.pendingReviewCount}</b>
              <span>Chờ duyệt</span>
            </div>
          </div>

          {/* Danh sach nay do backend loc theo vai tro: tac gia thay bai cua minh,
              bien tap thay het, doc gia chi thay bai da xuat ban. */}
          <h3>Bài mới nhất bạn được xem</h3>
          <table className="block">
            <thead>
              <tr>
                <th>Tiêu đề</th>
                <th>Tạo lúc</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {data.recentNews.map((n) => (
                <tr key={n.id}>
                  <td>{n.title}</td>
                  <td>{formatMoment(n.createdAt)}</td>
                  <td>{STATUS_LABEL[n.status] || n.status}</td>
                </tr>
              ))}
              {data.recentNews.length === 0 && <tr><td colSpan="3">Không có dữ liệu</td></tr>}
            </tbody>
          </table>
        </>
      )}
    </>
  )
}
