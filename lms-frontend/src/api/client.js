// Every request to the backend goes through api(). It adds the login token,
// reads JSON, and turns failures into an ApiError with a readable message.

const TOKEN_KEY = 'lms.token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

let onUnauthorized = () => {}
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn }

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

function fallbackMessage(status) {
  if (status === 401) return 'Your session has ended. Please sign in again.'
  if (status === 403) return 'You do not have permission to do that.'
  if (status === 404) return 'Not found.'
  if (status >= 502) return 'The server is not responding. Check that the backend is running.'
  return `Something went wrong (error ${status}).`
}

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  const token = tokenStore.get()
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth && token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check that the gateway is running on port 8080.')
  }

  const text = await res.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { /* not JSON */ }

  if (res.ok) return data
  if (res.status === 401 && token) onUnauthorized()   // an expired token signs the user out
  throw new ApiError(res.status, data?.error || fallbackMessage(res.status))
}
