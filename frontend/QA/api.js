const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: 'include',
      ...options,
      headers: { Accept: 'application/json', ...options.headers },
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('Không thể kết nối backend. Hãy chạy Spring Boot tại localhost:8080.', 0)
  }

  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/api/auth/')) {
      window.dispatchEvent(new Event('session-expired'))
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
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  return request(path, {
    method,
    headers,
    signal,
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}
