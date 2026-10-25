import assert from 'node:assert/strict'
import test from 'node:test'

const TOKEN_KEY = 'funews.accessToken'
const account = {
  id: 10, fullName: 'QA Admin', email: 'admin@fu.edu.vn', roleId: 1,
  roleCode: 'ADMIN', isActive: true, username: 'admin', dateOfBirth: null,
  createdAt: '2026-10-25T09:00:00', status: 1,
}
let moduleId = 0

function loginResponse(token = 'jwt-access-token') {
  return { accessToken: token, tokenType: 'Bearer', expiresIn: 900, user: account }
}

async function client(t, { token, blockedStorage = false } = {}) {
  const descriptors = new Map(['fetch', 'window', 'sessionStorage']
    .map((name) => [name, Object.getOwnPropertyDescriptor(globalThis, name)]))
  t.after(() => {
    for (const [name, descriptor] of descriptors) {
      if (descriptor) Object.defineProperty(globalThis, name, descriptor)
      else delete globalThis[name]
    }
  })
  const values = new Map(token ? [[TOKEN_KEY, token]] : [])
  const storage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  }
  const events = new EventTarget()
  Object.defineProperty(globalThis, 'window', { configurable: true, value: events })
  Object.defineProperty(globalThis, 'sessionStorage', blockedStorage
    ? { configurable: true, get() { throw new DOMException('Storage blocked', 'SecurityError') } }
    : { configurable: true, value: storage })
  const calls = []
  const responses = []
  Object.defineProperty(globalThis, 'fetch', {
    configurable: true,
    value: async (url, options) => {
      calls.push({ url, ...options })
      assert.ok(responses.length, `Unexpected fetch: ${url}`)
      return responses.shift()()
    },
  })
  const freshApi = () => import(`./api.js?test=${++moduleId}`)
  const module = await freshApi()
  return {
    ...module, calls, storage, events, freshApi,
    reply(status, body) {
      responses.push(() => status === 204
        ? new Response(null, { status })
        : Response.json(body, { status }))
    },
    fail(error) { responses.push(() => { throw error }) },
    defer(promise) { responses.push(() => promise) },
  }
}

test('login replaces the token, returns the user contract, and never sends an old token or cookies', async (t) => {
  const c = await client(t, { token: 'old-token' })
  c.reply(200, loginResponse())
  const user = await c.api('/api/auth/login', {
    method: 'POST', body: { username: 'admin', password: 'Admin' },
  })
  assert.deepEqual(user, { ...account, name: account.fullName, role: account.roleId })
  assert.equal(user.accessToken, undefined)
  assert.equal(c.storage.getItem(TOKEN_KEY), 'jwt-access-token')
  assert.equal(c.calls[0].headers.Authorization, undefined)
  assert.equal(c.calls[0].credentials, 'omit')
  assert.deepEqual(JSON.parse(c.calls[0].body), { username: 'admin', password: 'Admin' })
  c.reply(200, { total: 3 })
  assert.deepEqual(await c.api('/api/dashboard'), { total: 3 })
  assert.equal(c.calls[1].headers.Authorization, 'Bearer jwt-access-token')
  assert.equal(c.calls[1].url, 'http://localhost:8080/api/dashboard')
})

test('a refreshed page restores its token from sessionStorage and loads the current user', async (t) => {
  const c = await client(t)
  c.reply(200, loginResponse())
  await c.api('/api/auth/login', { method: 'POST' })
  const refreshed = await c.freshApi()
  c.reply(200, account)
  assert.equal((await refreshed.api('/api/auth/me')).name, 'QA Admin')
  assert.equal(c.calls[1].headers.Authorization, 'Bearer jwt-access-token')
})

test('logout sends the token and clears it before the next request', async (t) => {
  const c = await client(t, { token: 'active-token' })
  c.reply(204)
  assert.equal(await c.api('/api/auth/logout', { method: 'POST' }), null)
  assert.equal(c.calls[0].headers.Authorization, 'Bearer active-token')
  assert.equal(c.storage.getItem(TOKEN_KEY), null)
  c.reply(200, [])
  await c.api('/api/news')
  assert.equal(c.calls[1].headers.Authorization, undefined)
})

test('logout clears local authentication even if revocation fails', async (t) => {
  for (const failure of ['network', 'server']) {
    await t.test(failure, async (t) => {
      const c = await client(t, { token: 'active-token' })
      if (failure === 'network') c.fail(new TypeError('Offline'))
      else c.reply(500, { message: 'Logout unavailable' })
      await assert.rejects(c.api('/api/auth/logout', { method: 'POST' }), c.ApiError)
      assert.equal(c.storage.getItem(TOKEN_KEY), null)
      c.reply(200, [])
      await c.api('/api/news')
      assert.equal(c.calls[1].headers.Authorization, undefined)
    })
  }
})

test('an authenticated 401 clears the token, dispatches expiry once, and preserves API errors', async (t) => {
  const c = await client(t, { token: 'expired-token' })
  let expired = 0
  c.events.addEventListener('session-expired', () => expired++)
  c.reply(401, { message: 'Token expired', errors: { token: 'invalid' } })
  await assert.rejects(c.api('/api/dashboard'), {
    name: 'ApiError', status: 401, message: 'Token expired token: invalid',
  })
  assert.equal(c.storage.getItem(TOKEN_KEY), null)
  assert.equal(expired, 1)
  c.reply(401, { message: 'Authentication required' })
  await assert.rejects(c.api('/api/profile'), { status: 401 })
  assert.equal(c.calls[1].headers.Authorization, undefined)
  assert.equal(expired, 1)
})

test('auth/me detects an expired stored token while an anonymous first visit remains anonymous', async (t) => {
  for (const token of ['expired-token', undefined]) {
    await t.test(token || 'anonymous', async (t) => {
      const c = await client(t, { token })
      let expired = 0
      c.events.addEventListener('session-expired', () => expired++)
      c.reply(401, { message: 'Authentication required' })
      await assert.rejects(c.api('/api/auth/me'), { status: 401 })
      assert.equal(expired, token ? 1 : 0)
    })
  }
})

test('a failed login does not send or expire an already authenticated token', async (t) => {
  const c = await client(t, { token: 'active-token' })
  let expired = 0
  c.events.addEventListener('session-expired', () => expired++)
  c.reply(401, { message: 'Wrong password' })
  await assert.rejects(c.api('/api/auth/login', { method: 'POST' }), {
    status: 401, message: 'Wrong password',
  })
  assert.equal(c.calls[0].headers.Authorization, undefined)
  assert.equal(c.storage.getItem(TOKEN_KEY), 'active-token')
  assert.equal(expired, 0)
})

test('a late 401 from the previous token cannot sign out a newly authenticated user', async (t) => {
  const c = await client(t, { token: 'old-token' })
  let finishOldRequest
  c.defer(new Promise((resolve) => { finishOldRequest = resolve }))
  const pending = c.api('/api/dashboard')
  c.reply(200, loginResponse('new-token'))
  await c.api('/api/auth/login', { method: 'POST' })
  finishOldRequest(Response.json({ message: 'Old token expired' }, { status: 401 }))
  await assert.rejects(pending, { status: 401 })
  assert.equal(c.storage.getItem(TOKEN_KEY), 'new-token')
  c.reply(200, account)
  await c.api('/api/auth/me')
  assert.equal(c.calls[2].headers.Authorization, 'Bearer new-token')
})

test('blocked sessionStorage uses in-memory authentication and still supports logout', async (t) => {
  const c = await client(t, { blockedStorage: true })
  c.reply(200, loginResponse())
  await c.api('/api/auth/login', { method: 'POST' })
  c.reply(200, account)
  await c.api('/api/auth/me')
  assert.equal(c.calls[1].headers.Authorization, 'Bearer jwt-access-token')
  c.reply(204)
  await c.api('/api/auth/logout', { method: 'POST' })
  c.reply(200, [])
  await c.api('/api/news')
  assert.equal(c.calls[3].headers.Authorization, undefined)
})

test('account lists, lookup, and profile retain new fields and provide legacy UI aliases', async (t) => {
  const c = await client(t, { token: 'active-token' })
  c.reply(200, [account])
  assert.deepEqual(await c.api('/api/accounts'), [{ ...account, name: 'QA Admin', role: 1 }])
  c.reply(200, [{ id: 10, fullName: 'QA Admin' }])
  assert.deepEqual(await c.api('/api/accounts/lookup'), [{ id: 10, fullName: 'QA Admin', name: 'QA Admin' }])
  c.reply(200, account)
  assert.equal((await c.api('/api/profile')).roleCode, 'ADMIN')
})

test('only account and profile requests map edited legacy fields to current DTO fields', async (t) => {
  const c = await client(t, { token: 'active-token' })
  c.reply(200, account)
  await c.api('/api/accounts/10', {
    method: 'PUT', body: { fullName: 'Old name', roleId: 1, name: 'Edited name', role: 2, email: account.email },
  })
  assert.deepEqual(JSON.parse(c.calls[0].body), { fullName: 'Edited name', roleId: 2, email: account.email })
  c.reply(200, account)
  await c.api('/api/profile', { method: 'PUT', body: { name: 'New profile', email: account.email } })
  assert.deepEqual(JSON.parse(c.calls[1].body), { fullName: 'New profile', email: account.email })
  c.reply(201, account)
  await c.api('/api/accounts', { method: 'POST', body: { fullName: 'Modern form', roleId: 3 } })
  assert.deepEqual(JSON.parse(c.calls[2].body), { fullName: 'Modern form', roleId: 3 })
  const news = { id: 5, name: 'Keep name', role: 'Keep role', fullName: 'Do not alias' }
  c.reply(200, news)
  assert.deepEqual(await c.api('/api/news/5?preview=true', { method: 'PUT', body: news }), news)
  assert.deepEqual(JSON.parse(c.calls[3].body), news)
  assert.equal(c.calls[3].url, 'http://localhost:8080/api/news/5?preview=true')
  assert.equal(c.calls[3].headers.Authorization, 'Bearer active-token')
})

test('role metadata and account edits retain the database IDs for all four role codes', async (t) => {
  const c = await client(t, { token: 'active-token' })
  const roles = ['ADMIN', 'EDITOR', 'CONTRIBUTOR', 'READER'].map((code, index) => ({
    id: 21 + index, code, name: code, isActive: true,
  }))
  c.reply(200, roles)
  assert.deepEqual(await c.api('/api/roles'), roles)
  for (const role of roles) {
    const response = { ...account, roleId: role.id, roleCode: role.code }
    c.reply(200, response)
    const saved = await c.api('/api/accounts/10', {
      method: 'PUT', body: { name: account.fullName, email: account.email, role: role.id },
    })
    assert.equal(saved.role, role.id)
    assert.equal(saved.roleCode, role.code)
    assert.equal(JSON.parse(c.calls.at(-1).body).roleId, role.id)
  }
})

test('abort errors retain their identity and do not clear authentication', async (t) => {
  const c = await client(t, { token: 'active-token' })
  const abort = new DOMException('Cancelled', 'AbortError')
  const controller = new AbortController()
  c.fail(abort)
  await assert.rejects(c.api('/api/auth/me', { signal: controller.signal }), (error) => error === abort)
  assert.equal(c.calls[0].signal, controller.signal)
  assert.equal(c.storage.getItem(TOKEN_KEY), 'active-token')
})

test('an invalid login response cannot create a token session', async (t) => {
  const c = await client(t)
  c.reply(200, account)
  await assert.rejects(c.api('/api/auth/login', { method: 'POST' }), { status: 502 })
  assert.equal(c.storage.getItem(TOKEN_KEY), null)
})
