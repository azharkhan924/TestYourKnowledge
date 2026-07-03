// Change this if the backend runs on a different host/port.
const API_BASE = 'http://localhost:8080/api';

async function apiRequest(path, { method = 'GET', body, isForm = false, auth = false } = {}) {
  const headers = {};
  if (!isForm) headers['Content-Type'] = 'application/json';
  if (auth) {
    const token = localStorage.getItem('adminToken');
    if (token) headers['Authorization'] = 'Bearer ' + token;
  }

  const res = await fetch(API_BASE + path, {
    method,
    headers,
    body: isForm ? body : (body ? JSON.stringify(body) : undefined)
  });

  let data = null;
  try { data = await res.json(); } catch (e) { /* no body */ }

  if (!res.ok) {
    const message = (data && data.error) ? data.error : ('Request failed (' + res.status + ')');
    if (res.status === 401 && auth) {
      localStorage.removeItem('adminToken');
    }
    throw new Error(message);
  }
  return data;
}
