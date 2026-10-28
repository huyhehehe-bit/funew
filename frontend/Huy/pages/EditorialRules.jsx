import { useEffect, useState } from 'react'
import { api } from '../../QA/api'
import useAction from '../useAction'
import LoadStatus from '../../Long/components/LoadStatus'
import { formatMoment } from '../newsStatus'

/**
 * Quy tac bien tap - SRS muc 3.2, Administrator cau hinh.
 *
 * Ba quy tac nay deu co hieu luc that o backend chu khong chi hien thi: tat
 * autoPublishScheduled la tac vu hen gio ngung dang bai, bat
 * requireNoteOnApprove la duyet bai thieu ghi chu se bi tu choi, va
 * minSummaryLength chan ngay luc nop bai.
 */
export default function EditorialRules() {
  const [state, setState] = useState({ loading: true, error: '', data: null })
  const [form, setForm] = useState(null)
  const [saved, setSaved] = useState('')
  const { pending, error, run, clearError } = useAction()

  useEffect(() => {
    const controller = new AbortController()
    api('/api/editorial-rules', { signal: controller.signal })
      .then((data) => {
        setState({ loading: false, error: '', data })
        setForm({
          autoPublishScheduled: data.autoPublishScheduled,
          requireNoteOnApprove: data.requireNoteOnApprove,
          minSummaryLength: data.minSummaryLength,
        })
      })
      .catch((failure) => {
        if (failure.name !== 'AbortError') setState({ loading: false, error: failure.message, data: null })
      })
    return () => controller.abort()
  }, [])

  const save = (e) => {
    e.preventDefault()
    clearError()
    setSaved('')
    run(async () => {
      const result = await api('/api/editorial-rules', {
        method: 'PUT',
        body: { ...form, minSummaryLength: Number(form.minSummaryLength) },
      })
      setState((current) => ({ ...current, data: result }))
      setSaved('Đã lưu lúc ' + formatMoment(result.updatedAt))
    })
  }

  return (
    <>
      <h2>Quy tắc biên tập</h2>
      <p><small>Chỉ Administrator sửa được. Mọi quy tắc ở đây được backend áp dụng thật.</small></p>
      <LoadStatus store={state} />
      {form && (
        <form className="block" onSubmit={save}>
          <fieldset disabled={pending}>
            <label>
              <input
                type="checkbox"
                checked={form.autoPublishScheduled}
                onChange={(e) => setForm({ ...form, autoPublishScheduled: e.target.checked })}
              />
              {' '}Tự động đăng bài khi tới giờ hẹn
              <br />
              <small>Tắt thì bài đã duyệt nằm chờ biên tập viên bấm Đăng ngay.</small>
            </label>
            <label>
              <input
                type="checkbox"
                checked={form.requireNoteOnApprove}
                onChange={(e) => setForm({ ...form, requireNoteOnApprove: e.target.checked })}
              />
              {' '}Duyệt bài phải ghi chú
              <br />
              <small>Mặc định chỉ khi trả bài mới bắt buộc có lý do.</small>
            </label>
            <label>
              Số ký tự tóm tắt tối thiểu để được nộp bài
              <input
                type="number"
                min="0"
                max="1000"
                value={form.minSummaryLength}
                onChange={(e) => setForm({ ...form, minSummaryLength: e.target.value })}
              />
              <small>Để 0 nghĩa là không bắt buộc.</small>
            </label>
            <div className="modal-foot">
              <button type="submit">{pending ? 'Đang lưu...' : 'Lưu'}</button>
            </div>
          </fieldset>
          {error && <p className="error" role="alert">{error}</p>}
          {saved && <p role="status">{saved}</p>}
        </form>
      )}
    </>
  )
}
