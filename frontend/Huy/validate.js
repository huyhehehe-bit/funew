// Cac ham kiem tra dung chung cho form Them/Sua News va form Dang ky.
// Deu la predicate (tra ve true/false); cau thong bao tieng Viet do tung form
// tu dat, de loi hien dung ngu canh cua tung o.

// O bat buoc: khong null, khong rong, khong toan khoang trang.
export function required(value) {
  return typeof value === 'string' ? value.trim().length > 0 : value != null
}

// Gioi han do dai theo @Size ben backend. O trong coi nhu hop le (de @NotBlank lo).
export function maxLen(value, max) {
  return (value == null ? 0 : String(value).length) <= max
}

// URL hop le: bo trong duoc (truong khong bat buoc), co nhap thi phai co scheme
// http/https - khop dung voi @URL cua Hibernate ben backend (can scheme moi parse).
export function isUrl(value) {
  if (value == null || String(value).trim() === '') return true
  try {
    const parsed = new URL(String(value).trim())
    return parsed.protocol === 'http:' || parsed.protocol === 'https:'
  } catch {
    return false
  }
}

// Email dung dinh dang co ban: co phan truoc @, co ten mien, co dau cham.
export function isEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim())
}
