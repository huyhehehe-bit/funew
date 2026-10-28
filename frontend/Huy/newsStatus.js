// May trang thai 6 buoc cua bai viet - FR-ED-001. Ten hang phai khop enum
// ArticleStatus ben backend; doi mot ben la phai doi ca hai.
export const STATUS_LABEL = {
  DRAFT: 'Nháp',
  PENDING_REVIEW: 'Chờ duyệt',
  CHANGES_REQUESTED: 'Bị trả lại',
  SCHEDULED: 'Đã duyệt, chờ đăng',
  PUBLISHED: 'Đang công khai',
  ARCHIVED: 'Đã gỡ',
}

/** "2026-10-28T14:30:00" -> "28/10/2026 14:30". Để trống thì trả về dấu gạch. */
export function formatMoment(value) {
  if (!value) return '-'
  const [date, time = ''] = String(value).split('T')
  const [year, month, day] = date.split('-')
  if (!year || !month || !day) return String(value)
  return `${day}/${month}/${year} ${time.slice(0, 5)}`.trim()
}
