// ---- Auth guard ----
if (!localStorage.getItem('adminToken')) {
  window.location.href = 'admin-login.html';
}

let allTests = [];
let waitingPollHandle = null;
let activeTab = 'create';

window.addEventListener('DOMContentLoaded', init);

function init() {
  document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('adminToken');
    window.location.href = 'admin-login.html';
  });

  document.querySelectorAll('#tabs .tab').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  document.querySelectorAll('#create-mode-tabs .tab').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#create-mode-tabs .tab').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const mode = btn.dataset.mode;
      document.getElementById('mode-json').classList.toggle('hidden', mode !== 'json');
      document.getElementById('mode-text').classList.toggle('hidden', mode !== 'text');
    });
  });

  document.getElementById('create-json-btn').addEventListener('click', handleCreateJson);
  document.getElementById('create-upload-btn').addEventListener('click', handleCreateUpload);
  document.getElementById('waiting-test-select').addEventListener('change', onWaitingTestChange);
  document.getElementById('results-test-select').addEventListener('change', onResultsTestChange);
  document.getElementById('approve-all-btn').addEventListener('click', handleApproveAll);

  loadTests();
}

function switchTab(tab) {
  activeTab = tab;
  document.querySelectorAll('#tabs .tab').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.add('hidden'));
  document.getElementById('panel-' + tab).classList.remove('hidden');

  clearInterval(waitingPollHandle);
  if (tab === 'waiting') {
    const sel = document.getElementById('waiting-test-select');
    if (sel.value) {
      loadWaitingRoom(sel.value);
      waitingPollHandle = setInterval(() => loadWaitingRoom(sel.value), 5000);
    }
  }
}

// ---- Tests list / dropdowns ----
async function loadTests() {
  try {
    allTests = await apiRequest('/admin/tests', { auth: true });
  } catch (e) {
    if (e.message.includes('Unauthorized')) {
      window.location.href = 'admin-login.html';
      return;
    }
    allTests = [];
  }
  renderTestsList();
  renderTestSelect('waiting-test-select');
  renderTestSelect('results-test-select');
}

function renderTestsList() {
  const el = document.getElementById('tests-list');
  if (allTests.length === 0) {
    el.innerHTML = '<div class="empty-state">No tests yet — create one above.</div>';
    return;
  }
  el.innerHTML = allTests.map(t => {
    const link = window.location.origin + window.location.pathname.replace('admin.html', '') + 'take-test.html?test=' + t.id;
    return `
      <div class="spread" style="padding:14px 0; border-bottom:1px solid var(--border);">
        <div>
          <strong>${escapeHtml(t.title)}</strong>
          <div class="muted mono" style="font-size:12px; margin-top:2px;">ID ${t.id} · ${t.questions.length} questions · ${t.durationMinutes} min</div>
        </div>
        <button class="btn btn-outline btn-sm copy-link-btn" data-link="${link}">Copy Student Link</button>
      </div>
    `;
  }).join('');
  el.querySelectorAll('.copy-link-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      navigator.clipboard.writeText(btn.dataset.link).then(() => {
        const original = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(() => { btn.textContent = original; }, 1500);
      });
    });
  });
}

function renderTestSelect(selectId) {
  const sel = document.getElementById(selectId);
  const current = sel.value;
  sel.innerHTML = allTests.map(t => `<option value="${t.id}">${escapeHtml(t.title)} (ID ${t.id})</option>`).join('');
  if (current && allTests.some(t => String(t.id) === current)) {
    sel.value = current;
  } else if (allTests.length > 0) {
    sel.value = allTests[0].id;
    sel.dispatchEvent(new Event('change'));
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : str;
  return div.innerHTML;
}

// ---- Create test ----
async function handleCreateJson() {
  const errEl = document.getElementById('create-error');
  const okEl = document.getElementById('create-success');
  errEl.classList.add('hidden');
  okEl.classList.add('hidden');

  const title = document.getElementById('new-title').value.trim();
  const description = document.getElementById('new-desc').value.trim();
  const durationMinutes = parseInt(document.getElementById('new-duration').value, 10);
  const raw = document.getElementById('questions-json').value.trim();

  let questions;
  try {
    questions = JSON.parse(raw);
    if (!Array.isArray(questions)) throw new Error('JSON must be an array of questions');
  } catch (e) {
    errEl.textContent = 'Invalid JSON: ' + e.message;
    errEl.classList.remove('hidden');
    return;
  }

  try {
    const test = await apiRequest('/admin/tests', {
      method: 'POST', auth: true,
      body: { title, description, durationMinutes, questions }
    });
    okEl.textContent = `Created "${test.title}" with ${test.questions.length} question(s) — Test ID ${test.id}.`;
    okEl.classList.remove('hidden');
    document.getElementById('new-title').value = '';
    document.getElementById('new-desc').value = '';
    document.getElementById('new-duration').value = '';
    document.getElementById('questions-json').value = '';
    loadTests();
  } catch (e) {
    errEl.textContent = e.message;
    errEl.classList.remove('hidden');
  }
}

async function handleCreateUpload() {
  const errEl = document.getElementById('create-error');
  const okEl = document.getElementById('create-success');
  errEl.classList.add('hidden');
  okEl.classList.add('hidden');

  const title = document.getElementById('new-title').value.trim();
  const description = document.getElementById('new-desc').value.trim();
  const durationMinutes = document.getElementById('new-duration').value;
  const fileInput = document.getElementById('questions-file');
  const file = fileInput.files[0];

  if (!file) {
    errEl.textContent = 'Please choose a .txt file to upload.';
    errEl.classList.remove('hidden');
    return;
  }

  const formData = new FormData();
  formData.append('title', title);
  formData.append('description', description);
  formData.append('durationMinutes', durationMinutes);
  formData.append('file', file);

  try {
    const test = await apiRequest('/admin/tests/upload', {
      method: 'POST', auth: true, isForm: true, body: formData
    });
    okEl.textContent = `Created "${test.title}" with ${test.questions.length} question(s) — Test ID ${test.id}.`;
    okEl.classList.remove('hidden');
    document.getElementById('new-title').value = '';
    document.getElementById('new-desc').value = '';
    document.getElementById('new-duration').value = '';
    fileInput.value = '';
    loadTests();
  } catch (e) {
    errEl.textContent = e.message;
    errEl.classList.remove('hidden');
  }
}

// ---- Waiting room ----
function onWaitingTestChange() {
  clearInterval(waitingPollHandle);
  const id = document.getElementById('waiting-test-select').value;
  if (!id) return;
  loadWaitingRoom(id);
  if (activeTab === 'waiting') {
    waitingPollHandle = setInterval(() => loadWaitingRoom(id), 5000);
  }
}

async function loadWaitingRoom(testId) {
  let list;
  try {
    list = await apiRequest(`/admin/tests/${testId}/waiting-room`, { auth: true });
  } catch (e) {
    return;
  }
  const el = document.getElementById('waiting-list');
  if (list.length === 0) {
    el.innerHTML = '<div class="empty-state">No one is waiting right now.</div>';
    return;
  }
  el.innerHTML = list.map(a => `
    <div class="spread" style="padding:14px 0; border-bottom:1px solid var(--border);">
      <div>
        <strong>${escapeHtml(a.studentName)}</strong>
        <div class="muted mono" style="font-size:12px; margin-top:2px;">${escapeHtml(a.contactNumber)}</div>
      </div>
      <div class="row" style="flex:0 0 auto; gap:8px;">
        <button class="btn btn-success btn-sm approve-btn" data-id="${a.id}">Approve</button>
        <button class="btn btn-outline btn-sm reject-btn" data-id="${a.id}">Reject</button>
      </div>
    </div>
  `).join('');

  el.querySelectorAll('.approve-btn').forEach(btn => {
    btn.addEventListener('click', () => actOnAttempt(btn.dataset.id, 'approve', testId));
  });
  el.querySelectorAll('.reject-btn').forEach(btn => {
    btn.addEventListener('click', () => actOnAttempt(btn.dataset.id, 'reject', testId));
  });
}

async function actOnAttempt(attemptId, action, testId) {
  try {
    await apiRequest(`/admin/attempts/${attemptId}/${action}`, { method: 'POST', auth: true });
    loadWaitingRoom(testId);
  } catch (e) {
    alert('Failed: ' + e.message);
  }
}

async function handleApproveAll() {
  const id = document.getElementById('waiting-test-select').value;
  if (!id) return;
  try {
    const res = await apiRequest(`/admin/tests/${id}/waiting-room/approve-all`, { method: 'POST', auth: true });
    loadWaitingRoom(id);
  } catch (e) {
    alert('Failed: ' + e.message);
  }
}

// ---- Results ----
function onResultsTestChange() {
  const id = document.getElementById('results-test-select').value;
  if (id) loadResults(id);
}

async function loadResults(testId) {
  let list;
  try {
    list = await apiRequest(`/admin/tests/${testId}/results`, { auth: true });
  } catch (e) {
    return;
  }
  const el = document.getElementById('results-table');
  if (list.length === 0) {
    el.innerHTML = '<div class="empty-state">No attempts yet.</div>';
    return;
  }
  el.innerHTML = `
    <table>
      <thead>
        <tr><th>Name</th><th>Contact</th><th>Status</th><th>Score</th><th>Ended</th></tr>
      </thead>
      <tbody>
        ${list.map(a => `
          <tr>
            <td>${escapeHtml(a.studentName)}</td>
            <td class="mono">${escapeHtml(a.contactNumber)}</td>
            <td>${statusBadge(a.status)}</td>
            <td>${a.score != null ? a.score + ' / ' + a.totalQuestions : '—'}</td>
            <td class="muted" style="font-size:12px;">${a.endReason || '—'}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

function statusBadge(status) {
  const map = {
    PENDING: 'badge-pending',
    APPROVED: 'badge-approved',
    REJECTED: 'badge-rejected',
    SUBMITTED: 'badge-submitted'
  };
  return `<span class="badge ${map[status] || ''}">${status}</span>`;
}
