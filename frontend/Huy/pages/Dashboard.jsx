import { useEffect, useState } from 'react'
import { api } from '../../QA/api'
import LoadStatus from '../../Long/components/LoadStatus'

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
              <span>Accounts</span>
            </div>
            <div className="block card">
              <b>{data.categoryCount}</b>
              <span>Categories</span>
            </div>
            <div className="block card">
              <b>{data.newsCount}</b>
              <span>News articles</span>
            </div>
            <div className="block card">
              <b>{data.activeNewsCount}</b>
              <span>Active news</span>
            </div>
          </div>

          <h3>Tin moi nhat</h3>
          <table className="block">
            <thead>
              <tr>
                <th>Title</th>
                <th>Created date</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {data.recentNews.map((n) => (
                <tr key={n.id}>
                  <td>{n.title}</td>
                  <td>{n.createdDate}</td>
                  <td>{n.status === 1 ? 'Active' : 'Inactive'}</td>
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
