const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const TOKEN_STORAGE_KEY = 'funews.accessToken'
// Hai duong nay tra ve accessToken moi, va khong duoc gui token cu kem theo:
// token cu da het han se bi bo loc JWT tu choi truoc khi toi controller.
const TOKEN_ISSUING = ['/api/auth/login', '/api/auth/register']

// Keep a tab-local copy so authentication also works when browser storage is blocked.
let accessToken = readStoredToken()

function readStoredToken() {
  try {
    return globalThis.sessionStorage?.getItem(TOKEN_STORAGE_KEY) || null
  } catch {
    return null
  }
}

function setAccessToken(token) {
  accessToken = token
  try {
    if (token) globalThis.sessionStorage?.setItem(TOKEN_STORAGE_KEY, token)
    else globalThis.sessionStorage?.removeItem(TOKEN_STORAGE_KEY)
  } catch {
    // The in-memory copy remains available for this page when storage is blocked.
  }
}

function isAccountResource(path) {
  return path === '/api/profile' || /^\/api\/accounts(?:\/[^/]+)?$/.test(path)
}

function normalizeAccount(account) {
  if (!account || typeof account !== 'object') return account
  return {
    ...account,
    ...(account.fullName === undefined ? {} : { name: account.fullName }),
    ...(account.roleId === undefined ? {} : { role: account.roleId }),
  }
}

function normalizeAccountRequest(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) return body
  const { name, role, ...payload } = body
  // Existing account/profile forms edit these aliases after spreading the response.
  if (name !== undefined) payload.fullName = name
  if (role !== undefined) payload.roleId = role
  return payload
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, options = {}) {
  const resource = path.split(/[?#]/, 1)[0]
  const token = TOKEN_ISSUING.includes(resource) ? null : accessToken
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: 'omit',
      ...options,
      headers: {
        Accept: 'application/json',
        ...options.headers,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('Không thể kết nối backend. Hãy chạy Spring Boot tại localhost:8080.', 0)
  }

  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 && token && token === accessToken) {
      setAccessToken(null)
      globalThis.window?.dispatchEvent(new Event('session-expired'))
    }
    const fields = body?.errors && typeof body.errors === 'object'
      ? Object.entries(body.errors).map(([field, message]) => `${field}: ${message}`).join('; ')
      : ''
    const message = body?.message || `Yêu cầu thất bại (${response.status}).`
    throw new ApiError(fields ? `${message} ${fields}` : message, response.status)
  }
  return body
}

export async function api(path, { method = 'GET', body, signal } = {}) {
  const resource = path.split(/[?#]/, 1)[0]
  const payload = isAccountResource(resource) ? normalizeAccountRequest(body) : body
  const headers = {}
  if (payload !== undefined) headers['Content-Type'] = 'application/json'
  let result
  try {
    result = await request(path, {
      method,
      headers,
      signal,
      ...(payload === undefined ? {} : { body: JSON.stringify(payload) }),
    })
  } finally {
    if (resource === '/api/auth/logout') setAccessToken(null)
  }

  if (TOKEN_ISSUING.includes(resource)) {
    if (typeof result?.accessToken !== 'string' || !result.accessToken || !result.user) {
      throw new ApiError('Phản hồi đăng nhập không hợp lệ từ backend.', 502)
    }
    setAccessToken(result.accessToken)
    return normalizeAccount(result.user)
  }
  if (resource === '/api/auth/me' || isAccountResource(resource)) {
    return Array.isArray(result) ? result.map(normalizeAccount) : normalizeAccount(result)
  }
  return result
}
