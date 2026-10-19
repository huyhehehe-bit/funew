import { useState } from 'react'
import Modal from '../../Long/components/Modal'
import Toolbar from '../../Long/components/Toolbar'
import { ROLES } from '../data'
import LoadStatus from '../../Long/components/LoadStatus'
import useAction from '../../Huy/useAction'

const empty = { name: '', email: '', password: '', role: 2 }

export default function Users({ store, onSaved }) {
  const { items, add, update, remove } = store
  const [keyword, setKeyword] = useState('')
  const [form, setForm] = useState(null)
  const { pending, error, run, clearError } = useAction()

  const open = (account) => {
    clearError()
    setForm({ ...account, password: '' })
  }

  const shown = items.filter(
    (a) =>
      a.name.toLowerCase().includes(keyword.toLowerCase()) ||
      a.email.toLowerCase().includes(keyword.toLowerCase())
  )

  const save = (e) => {
    e.preventDefault()
    run(async () => {
      const saved = form.id ? await update(form) : await add(form)
      onSaved(saved)
      setForm(null)
    })
  }

  const del = (a) => {
    if (window.confirm(`Xoa tai khoan "${a.name}"?`)) run(() => remove(a.id))
  }

  return (
    <>
      <h2>Account Management</h2>
      <Toolbar keyword={keyword} onSearch={setKeyword} onCreate={() => open(empty)} disabled={pending || store.loading || !!store.error} />
      <LoadStatus store={store} />
      {!form && error && <p className="error" role="alert">{error}</p>}

      <table className="block">
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {shown.map((a) => (
            <tr key={a.id}>
              <td>{a.name}</td>
              <td>{a.email}</td>
              <td>{ROLES[a.role]}</td>
              <td>
                <button onClick={() => open(a)} disabled={pending || store.loading}>Edit</button>{' '}
                <button onClick={() => del(a)} disabled={pending || store.loading}>Delete</button>
              </td>
            </tr>
          ))}
          {!store.loading && !store.error && shown.length === 0 && (
            <tr>
              <td colSpan="4">Khong co du lieu</td>
            </tr>
          )}
        </tbody>
      </table>

      {form && (
        <Modal
          title={form.id ? 'Update account' : 'Create account'}
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
                Email
                <input
                  required
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  autoComplete="new-password"
                  required={!form.id}
                  placeholder={form.id ? 'Để trống để giữ mật khẩu hiện tại' : ''}
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
              </label>
              <label>
                Role
                <select
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: Number(e.target.value) })}
                >
                  <option value={1}>Admin</option>
                  <option value={2}>Staff</option>
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
