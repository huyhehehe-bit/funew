import { useState } from 'react'
import Modal from '../components/Modal'
import Toolbar from '../components/Toolbar'
import LoadStatus from '../components/LoadStatus'
import useAction from '../../Huy/useAction'

// Lấy ngày hiện tại định dạng YYYY-MM-DD làm mặc định khi tạo mới
const getToday = () => new Date().toISOString().split('T')[0]
const empty = { name: '', description: '', parentId: 0, isActive: true, date: getToday() }

export default function Categories({ store }) {
  const { items, add, update, remove } = store
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [form, setForm] = useState(null) // null = đóng popup form thêm/sửa
  const [detail, setDetail] = useState(null) // null = đóng popup xem chi tiết
  const { pending, error, run, clearError } = useAction()

  const open = (category) => {
    clearError()
    setForm({ ...category, date: category.date || getToday() })
  }

  const openDetail = (category) => {
    setDetail(category)
  }

  const shown = items.filter((c) => {
    const matchesKeyword = c.name
        .toLowerCase()
        .includes(keyword.toLowerCase())

    const matchesStatus =
        statusFilter === 'all' ||
        c.isActive === (statusFilter === '1')

    return matchesKeyword && matchesStatus
  })

  const save = (e) => {
    e.preventDefault()
    run(async () => {
      if (form.id) await update(form)
      else await add(form)
      setForm(null)
    })
  }

  const del = (c) => {
    if (window.confirm(`Xóa category "${c.name}"?`)) run(() => remove(c.id))
  }

  const parentName = (id) => items.find((c) => c.id === id)?.name || '-'

  return (
      <>
        <h2>Category Management</h2>
        <Toolbar keyword={keyword} onSearch={setKeyword} onCreate={() => open(empty)} disabled={pending || store.loading || !!store.error} />
        <div className="category-filters">
          <label htmlFor="statusFilter">Filter by Status: </label>
          <select
              id="statusFilter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              disabled={pending || store.loading}
          >
            <option value="all">All Status</option>
            <option value="1">Active</option>
            <option value="0">Inactive</option>
          </select>
        </div>
        <LoadStatus store={store} />
        {!form && !detail && error && <p className="error" role="alert">{error}</p>}

        <table className="block">
          <thead>
          <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Parent</th>
            <th>Date</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
          </thead>
          <tbody>
          {shown.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td>{c.description}</td>
                <td>{parentName(c.parentId)}</td>
                <td>{c.date || '-'}</td>
                <td>{c.isActive ? 'Active' : 'Inactive'}</td>
                <td>
                  <button onClick={() => openDetail(c)} disabled={pending || store.loading}>View</button>{' '}
                  <button onClick={() => open(c)} disabled={pending || store.loading}>Edit</button>{' '}
                  <button onClick={() => del(c)} disabled={pending || store.loading}>Delete</button>
                </td>
              </tr>
          ))}
          {!store.loading && !store.error && shown.length === 0 && (
              <tr>
                <td colSpan="6">Không có dữ liệu</td>
              </tr>
          )}
          </tbody>
        </table>

        {/* Modal Xem chi tiết (View Details) */}
        {detail && (
            <Modal
                title="Category Details"
                onClose={() => setDetail(null)}
            >
              <div className="category-details">
                <p><strong>ID:</strong> {detail.id}</p>
                <p><strong>Name:</strong> {detail.name}</p>
                <p><strong>Slug:</strong> {detail.slug || '-'}</p>
                <p><strong>Description:</strong> {detail.description || '(No description)'}</p>
                <p><strong>Parent Category:</strong> {parentName(detail.parentId)}</p>
                <p><strong>Date:</strong> {detail.date || '-'}</p>
                <p><strong>Display Order:</strong> {detail.displayOrder ?? 0}</p>
                <p><strong>Status:</strong> {detail.isActive ? 'Active' : 'Inactive'}</p>
              </div>
              <div className="modal-foot" style={{ marginTop: '20px' }}>
                <button type="button" onClick={() => setDetail(null)}>
                  Close
                </button>
              </div>
            </Modal>
        )}

        {/* Modal Thêm / Sửa */}
        {form && (
            <Modal
                title={form.id ? 'Update category' : 'Create category'}
                onClose={() => setForm(null)}
                busy={pending}
            >
              <form onSubmit={save}>
                <fieldset disabled={pending}>
                  <label>
                    Name
                    <input
                        required
                        value={form.name}
                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                    />
                  </label>
                  <label>
                    Description
                    <input
                        value={form.description}
                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                    />
                  </label>
                  <label>
                    Parent category
                    <select
                        value={form.parentId}
                        onChange={(e) => setForm({ ...form, parentId: Number(e.target.value) })}
                    >
                      <option value={0}>-- Không có --</option>
                      {items
                          .filter((c) => c.id !== form.id)
                          .map((c) => (
                              <option key={c.id} value={c.id}>
                                {c.name}
                              </option>
                          ))}
                    </select>
                  </label>
                  <label>
                    Date
                    <input
                        type="date"
                        value={form.date || ''}
                        onChange={(e) => setForm({ ...form, date: e.target.value })}
                    />
                  </label>
                  <label>
                    Status
                    <select
                        value={String(form.isActive)}
                        onChange={(e) => setForm({ ...form, isActive: e.target.value === 'true' })}
                    >
                      <option value="true">Active</option>
                      <option value="false">Inactive</option>
                    </select>
                  </label>
                  <div className="modal-foot">
                    <button type="submit">{pending ? 'Đang lưu...' : 'Save'}</button>
                    <button type="button" onClick={() => setForm(null)}>
                      Cancel
                    </button>
                  </div>
                </fieldset>
                {error && <p className="error" role="alert">{error}</p>}
              </form>
            </Modal>
        )}
      </>
  )
}