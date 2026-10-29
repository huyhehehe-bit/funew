import { useState } from 'react'
import Modal from '../../Long/components/Modal'
import Toolbar from '../../Long/components/Toolbar'
import LoadStatus from '../../Long/components/LoadStatus'
import useAction from '../useAction'
import { api } from '../../QA/api'
import { STATUS_LABEL, formatMoment } from '../newsStatus'
import Stars from '../components/Stars'

const BEFORE_REVIEW = ['DRAFT', 'CHANGES_REQUESTED']
const AFTER_REVIEW = ['SCHEDULED', 'PUBLISHED', 'ARCHIVED']

const ROLE_HINT = {
  ADMIN: 'ghi đè được mọi bước của luồng duyệt bài.',
  EDITOR: 'duyệt bài, trả bài lại kèm lý do, hẹn giờ đăng và gỡ bài.',
  CONTRIBUTOR: 'tạo và nộp bài của chính mình, không tự đăng được.',
  READER: 'chỉ đọc bài đã xuất bản.',
}

export default function News({ store, categories, accounts, categoryStore, authorStore, user }) {
  const { items, add, update, remove, replace } = store
  const [keyword, setKeyword] = useState('')
  const [form, setForm] = useState(null)
  const [review, setReview] = useState(null)
  const [history, setHistory] = useState(null)
  // Diem cua chinh minh, nho lai sau moi lan cham trong phien nay.
  const [myScores, setMyScores] = useState({})
  const { pending, error, run, clearError } = useAction()

  const roleCode = user?.roleCode || ''
  const isAdmin = roleCode === 'ADMIN'
  const isStaff = isAdmin || roleCode === 'EDITOR'
  const isReader = roleCode === 'READER'

  // Cac hàm dưới đây chỉ quyết định hiện hay ẩn nút. Chặn thật nằm ở
  // SecurityConfig và NewsService - ẩn nút không phải là phân quyền.
  const owns = (n) => n.authorId === user?.id
  const canEdit = (n) => {
    if (isAdmin) return true
    if (n.status === 'PENDING_REVIEW') return false
    if (AFTER_REVIEW.includes(n.status)) return isStaff
    return owns(n) || isStaff
  }
  const canSubmit = (n) => (owns(n) || isAdmin) && BEFORE_REVIEW.includes(n.status)
  const canReview = (n) => isStaff && n.status === 'PENDING_REVIEW'
  const canPublish = (n) => isStaff && n.status === 'SCHEDULED'
  const canArchive = (n) => isStaff && n.status === 'PUBLISHED'
  const canDelete = (n) => isAdmin || (owns(n) && n.status !== 'PUBLISHED')

  const empty = {
    title: '',
    slug: '',
    summary: '',
    content: '',
    thumbnailUrl: '',
    source: '',
    categoryId: categories[0]?.id || 0,
    tagsText: '',
    status: 'DRAFT',
    isFeatured: false,
    version: null,
  }

  const open = (article) => {
    clearError()
    setForm(article ? { ...article, tagsText: (article.tags || []).join(', ') } : { ...empty })
  }

  const payload = (f) => ({
    title: f.title,
    slug: f.slug || null,
    summary: f.summary,
    content: f.content,
    thumbnailUrl: f.thumbnailUrl || null,
    source: f.source,
    categoryId: Number(f.categoryId),
    tags: f.tagsText.split(',').map((tag) => tag.trim()).filter(Boolean),
    status: f.status,
    isFeatured: !!f.isFeatured,
    // Gửi lại version đã nhận để server phát hiện người khác vừa sửa bài này.
    version: f.version ?? null,
  })

  const shown = items.filter((n) => {
    const needle = keyword.toLowerCase()
    return n.title.toLowerCase().includes(needle) || (n.summary || '').toLowerCase().includes(needle)
  })

  const save = (e) => {
    e.preventDefault()
    run(async () => {
      if (form.id) await update({ ...payload(form), id: form.id })
      else await add(payload(form))
      setForm(null)
    })
  }

  const act = (article, action, body) => run(async () => {
    replace(await api(`/api/news/${article.id}/${action}`, { method: 'POST', body }))
    setReview(null)
  })

  // Cham diem: chi bai da cong khai, va phai dang nhap - khach vang lai khong goi
  // duoc endpoint nay. Tra ve so luot va diem trung binh moi nen cap nhat luon dong do.
  const rate = (article, score) => run(async () => {
    const saved = await api(`/api/news/${article.id}/rating`, { method: 'PUT', body: { score } })
    setMyScores((current) => ({ ...current, [article.id]: saved.score }))
    replace({ ...article, ratingCount: saved.ratingCount, ratingAvg: saved.ratingAvg })
  })

  const submitReview = (e) => {
    e.preventDefault()
    if (review.mode === 'approve') {
      act(review.article, 'approve', {
        note: review.note || null,
        scheduledPublishAt: review.schedule ? `${review.schedule}:00` : null,
      })
    } else {
      act(review.article, 'request-changes', { note: review.note })
    }
  }

  const del = (n) => {
    if (window.confirm(`Xóa bài viết "${n.title}"?`)) run(() => remove(n.id))
  }

  const openHistory = (n) => run(async () => {
    setHistory({ article: n, rows: await api(`/api/news/${n.id}/audit`) })
  })

  const categoryName = (id) => categories.find((c) => c.id === id)?.name || '-'
  const authorName = (id) => accounts.find((a) => a.id === id)?.name || '-'
  const busy = pending || store.loading

  return (
    <>
      <h2>Bài viết</h2>
      <p>
        <small>
          Vai trò <b>{roleCode || '-'}</b>: {ROLE_HINT[roleCode] || 'không rõ quyền.'}{' '}
          Danh sách này do backend lọc theo vai trò, không phải lọc ở trình duyệt.
        </small>
      </p>
      <Toolbar
        keyword={keyword}
        onSearch={setKeyword}
        onCreate={() => open(null)}
        disabled={busy || isReader || categoryStore.loading || !!store.error
          || !!categoryStore.error || categories.length === 0}
      />
      <LoadStatus store={store} />
      <LoadStatus store={categoryStore} />
      <LoadStatus store={authorStore} />
      {!categoryStore.loading && !categoryStore.error && categories.length === 0 && (
        <p>Cần tạo ít nhất một danh mục trước khi tạo bài viết.</p>
      )}
      {!form && !review && error && <p className="error" role="alert">{error}</p>}

      <table className="block">
        <thead>
          <tr>
            <th>Tiêu đề</th>
            <th>Danh mục</th>
            <th>Thẻ</th>
            <th>Tác giả</th>
            <th>Trạng thái</th>
            <th>Đánh giá</th>
            <th>Cập nhật</th>
            <th>Hành động</th>
          </tr>
        </thead>
        <tbody>
          {shown.map((n) => (
            <tr key={n.id}>
              <td>{n.title}{n.isFeatured ? ' *' : ''}</td>
              <td>{categoryName(n.categoryId)}</td>
              <td>{(n.tags || []).join(', ')}</td>
              <td>{authorName(n.authorId)}</td>
              <td>{STATUS_LABEL[n.status] || n.status}</td>
              <td>
                {n.status === 'PUBLISHED' ? (
                  <>
                    {n.ratingAvg ? `${n.ratingAvg}` : '–'} ({n.ratingCount}){' '}
                    <Stars value={myScores[n.id] || 0} onPick={(score) => rate(n, score)} disabled={busy} />
                  </>
                ) : '–'}
              </td>
              <td>{formatMoment(n.updatedAt)}</td>
              <td>
                {canEdit(n) && <><button onClick={() => open(n)} disabled={busy}>Sửa</button>{' '}</>}
                {canSubmit(n) && <><button onClick={() => act(n, 'submit')} disabled={busy}>Nộp</button>{' '}</>}
                {canReview(n) && (
                  <>
                    <button onClick={() => { clearError(); setReview({ article: n, mode: 'approve', note: '', schedule: '' }) }} disabled={busy}>Duyệt</button>{' '}
                    <button onClick={() => { clearError(); setReview({ article: n, mode: 'request-changes', note: '', schedule: '' }) }} disabled={busy}>Trả lại</button>{' '}
                  </>
                )}
                {canPublish(n) && <><button onClick={() => act(n, 'publish')} disabled={busy}>Đăng ngay</button>{' '}</>}
                {canArchive(n) && <><button onClick={() => act(n, 'archive')} disabled={busy}>Gỡ</button>{' '}</>}
                <button onClick={() => openHistory(n)} disabled={busy}>Lịch sử</button>{' '}
                {canDelete(n) && <button onClick={() => del(n)} disabled={busy}>Xóa</button>}
              </td>
            </tr>
          ))}
          {!store.loading && !store.error && shown.length === 0 && (
            <tr>
              <td colSpan="8">Không có dữ liệu</td>
            </tr>
          )}
        </tbody>
      </table>

      {form && (
        <Modal
          title={form.id ? 'Sửa bài viết' : 'Tạo bài viết'}
          onClose={() => setForm(null)}
          busy={pending}
        >
          <form onSubmit={save}>
            <fieldset disabled={pending}>
              <label>
                Tiêu đề
                <input required value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })} />
              </label>
              <label>
                Slug (để trống thì server tự sinh từ tiêu đề)
                <input value={form.slug || ''}
                  onChange={(e) => setForm({ ...form, slug: e.target.value })} />
              </label>
              <label>
                Tóm tắt
                <input value={form.summary || ''}
                  onChange={(e) => setForm({ ...form, summary: e.target.value })} />
              </label>
              <label>
                Nội dung
                <textarea rows="4" value={form.content || ''}
                  onChange={(e) => setForm({ ...form, content: e.target.value })} />
              </label>
              <label>
                Ảnh đại diện (URL)
                <input value={form.thumbnailUrl || ''}
                  onChange={(e) => setForm({ ...form, thumbnailUrl: e.target.value })} />
              </label>
              <label>
                Nguồn
                <input value={form.source || ''}
                  onChange={(e) => setForm({ ...form, source: e.target.value })} />
              </label>
              <label>
                Danh mục
                <select required value={form.categoryId}
                  onChange={(e) => setForm({ ...form, categoryId: Number(e.target.value) })}>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </label>
              <label>
                Thẻ (cách nhau bằng dấu phẩy)
                <input value={form.tagsText}
                  onChange={(e) => setForm({ ...form, tagsText: e.target.value })} />
              </label>
              {/* Tao bai chi duoc DRAFT hoac PENDING_REVIEW. Sua bai thi khong doi
                  trang thai: cac buoc sau do di qua nut Duyet / Tra lai / Dang. */}
              {form.id ? (
                <p><small>Trạng thái: <b>{STATUS_LABEL[form.status] || form.status}</b> — đổi trạng thái bằng các nút trong bảng.</small></p>
              ) : (
                <label>
                  Lưu thành
                  <select value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value })}>
                    <option value="DRAFT">Nháp</option>
                    <option value="PENDING_REVIEW">Nộp luôn cho biên tập</option>
                  </select>
                </label>
              )}
              {isStaff && (
                <label>
                  <input type="checkbox" checked={!!form.isFeatured}
                    onChange={(e) => setForm({ ...form, isFeatured: e.target.checked })} />
                  {' '}Bài nổi bật
                </label>
              )}
              <div className="modal-foot">
                <button type="submit">{pending ? 'Đang lưu...' : 'Lưu'}</button>
                <button type="button" onClick={() => setForm(null)}>Hủy</button>
              </div>
            </fieldset>
            {error && <p className="error" role="alert">{error}</p>}
          </form>
        </Modal>
      )}

      {review && (
        <Modal
          title={review.mode === 'approve'
            ? `Duyệt bài: ${review.article.title}`
            : `Trả lại bài: ${review.article.title}`}
          onClose={() => setReview(null)}
          busy={pending}
        >
          <form onSubmit={submitReview}>
            <fieldset disabled={pending}>
              <label>
                {review.mode === 'approve' ? 'Ghi chú (không bắt buộc)' : 'Lý do trả lại (bắt buộc)'}
                <textarea rows="3" required={review.mode !== 'approve'} value={review.note}
                  onChange={(e) => setReview({ ...review, note: e.target.value })} />
              </label>
              {review.mode === 'approve' && (
                <label>
                  Hẹn giờ đăng (để trống thì đăng ngay)
                  <input type="datetime-local" value={review.schedule}
                    onChange={(e) => setReview({ ...review, schedule: e.target.value })} />
                </label>
              )}
              <div className="modal-foot">
                <button type="submit">
                  {pending ? 'Đang gửi...' : review.mode === 'approve' ? 'Duyệt' : 'Trả lại'}
                </button>
                <button type="button" onClick={() => setReview(null)}>Hủy</button>
              </div>
            </fieldset>
            {error && <p className="error" role="alert">{error}</p>}
          </form>
        </Modal>
      )}

      {history && (
        <Modal title={`Lịch sử: ${history.article.title}`} onClose={() => setHistory(null)}>
          <table>
            <thead>
              <tr>
                <th>Lúc</th>
                <th>Hành động</th>
                <th>Từ</th>
                <th>Sang</th>
                <th>Người làm</th>
                <th>Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              {history.rows.map((row) => (
                <tr key={row.id}>
                  <td>{formatMoment(row.createdAt)}</td>
                  <td>{row.action}</td>
                  <td>{STATUS_LABEL[row.fromStatus] || '-'}</td>
                  <td>{STATUS_LABEL[row.toStatus] || row.toStatus}</td>
                  <td>{authorName(row.actorId)}</td>
                  <td>{row.note || '-'}</td>
                </tr>
              ))}
              {history.rows.length === 0 && <tr><td colSpan="6">Chưa có dòng nào</td></tr>}
            </tbody>
          </table>
          <div className="modal-foot">
            <button type="button" onClick={() => setHistory(null)}>Đóng</button>
          </div>
        </Modal>
      )}
    </>
  )
}
