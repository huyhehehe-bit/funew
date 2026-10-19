import { useState } from 'react'
import { ROLES } from '../../QA/data'
import useAction from '../../Huy/useAction'

export default function Settings({ user, onSave }) {
  const [form, setForm] = useState(user)
  const [saved, setSaved] = useState(false)
  const { pending, error, run } = useAction()

  const submit = (e) => {
    e.preventDefault()
    setSaved(false)
    run(async () => {
      setForm(await onSave(form))
      setSaved(true)
    })
  }

  return (
    <>
      <h2>Settings</h2>
      <form className="block form-block" onSubmit={submit}>
        <fieldset disabled={pending}>
          <label>
            Name
            <input
              required
              value={form.name}
              onChange={(e) => { setSaved(false); setForm({ ...form, name: e.target.value }) }}
            />
          </label>
          <label>
            Email
            <input
              required
              type="email"
              value={form.email}
              onChange={(e) => { setSaved(false); setForm({ ...form, email: e.target.value }) }}
            />
          </label>
          <label>
            Role
            <input value={ROLES[form.role]} readOnly />
          </label>
          <button type="submit">{pending ? 'Đang lưu...' : 'Save'}</button>
        </fieldset>
        {error && <p className="error" role="alert">{error}</p>}
        {saved && <p>Da luu.</p>}
      </form>
    </>
  )
}
