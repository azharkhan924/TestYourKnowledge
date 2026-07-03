// ---- State ----
let testId = null, attemptId = null, durationMinutes = null;
let questions = [], answers = {}, currentIndex = 0;
let examInProgress = false;
let submitted = false;
let intentionalExit = false;
let pollHandle, timerHandle, deadline;
let lastViolationAt = 0;

window.addEventListener('DOMContentLoaded', init);

function init() {
  const params = new URLSearchParams(window.location.search);
  const t = params.get('test');
  if (t) {
    document.getElementById('test-code').value = t;
    loadTestInfo(t);
  }
  document.getElementById('test-code').addEventListener('change', (e) => {
    const val = e.target.value.trim();
    if (val) loadTestInfo(val);
  });
  document.getElementById('join-btn').addEventListener('click', handleJoin);
  document.getElementById('start-test-btn').addEventListener('click', handleStartTest);
  document.getElementById('submit-btn').addEventListener('click', () => {
    document.getElementById('overlay-submit-confirm').classList.remove('hidden');
  });
  document.getElementById('confirm-cancel-btn').addEventListener('click', () => {
    document.getElementById('overlay-submit-confirm').classList.add('hidden');
  });
  document.getElementById('confirm-submit-btn').addEventListener('click', () => {
    document.getElementById('overlay-submit-confirm').classList.add('hidden');
    submitTest('MANUAL');
  });
  document.getElementById('tab-warning-dismiss').addEventListener('click', () => {
    document.getElementById('overlay-tab-warning').classList.add('hidden');
  });

  document.addEventListener('fullscreenchange', () => {
    if (!examInProgress || submitted) return;
    if (!document.fullscreenElement && !intentionalExit) {
      document.getElementById('overlay-fullscreen-exit').classList.remove('hidden');
      submitTest('FULLSCREEN_EXIT');
    }
  });

  document.addEventListener('visibilitychange', () => {
    if (!examInProgress || submitted) return;
    if (document.hidden) {
      const now = Date.now();
      if (now - lastViolationAt < 1000) return;
      lastViolationAt = now;
      handleViolation();
    }
  });
}

function showView(id) {
  ['view-join', 'view-waiting', 'view-instructions', 'view-exam', 'view-result'].forEach(v => {
    document.getElementById(v).classList.toggle('hidden', v !== id);
  });
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : str;
  return div.innerHTML;
}

// ---- Join ----
async function loadTestInfo(id) {
  try {
    const info = await apiRequest(`/tests/${id}`);
    document.getElementById('join-title').textContent = info.title;
    document.getElementById('join-desc').textContent = info.description || '';
    document.getElementById('join-meta').innerHTML = `
      <div class="badge badge-approved"><span class="dot"></span>${info.durationMinutes} min</div>
      <div class="badge badge-submitted"><span class="dot"></span>${info.questionCount} questions</div>
    `;
  } catch (e) {
    document.getElementById('join-title').textContent = 'Test not found';
    document.getElementById('join-desc').textContent = '';
    document.getElementById('join-meta').innerHTML = '';
  }
}

async function handleJoin() {
  const errEl = document.getElementById('join-error');
  errEl.classList.add('hidden');

  const id = document.getElementById('test-code').value.trim();
  const name = document.getElementById('student-name').value.trim();
  const contact = document.getElementById('student-contact').value.trim();

  if (!id || !name || !contact) {
    errEl.textContent = 'Please fill in the test ID, your name and contact number.';
    errEl.classList.remove('hidden');
    return;
  }

  const btn = document.getElementById('join-btn');
  btn.disabled = true;
  btn.textContent = 'Joining…';

  try {
    const res = await apiRequest('/student/join', {
      method: 'POST',
      body: { testId: parseInt(id, 10), studentName: name, contactNumber: contact }
    });
    testId = id;
    attemptId = res.attemptId;
    showView('view-waiting');
    pollStatus();
  } catch (e) {
    errEl.textContent = e.message;
    errEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Request to Join';
  }
}

// ---- Waiting room ----
function pollStatus() {
  checkStatus();
  pollHandle = setInterval(checkStatus, 3000);
}

async function checkStatus() {
  try {
    const res = await apiRequest(`/student/attempts/${attemptId}/status`);
    if (res.status === 'APPROVED') {
      clearInterval(pollHandle);
      durationMinutes = res.durationMinutes;
      document.getElementById('instr-duration').textContent = durationMinutes;
      showView('view-instructions');
    } else if (res.status === 'REJECTED') {
      clearInterval(pollHandle);
      document.getElementById('waiting-rejected').classList.remove('hidden');
      document.getElementById('waiting-status-text').textContent = 'Not approved';
    }
  } catch (e) {
    // transient network hiccup — keep polling silently
  }
}

// ---- Start exam ----
async function handleStartTest() {
  const btn = document.getElementById('start-test-btn');
  btn.disabled = true;
  btn.textContent = 'Starting…';

  try {
    if (document.documentElement.requestFullscreen) {
      await document.documentElement.requestFullscreen();
    }
  } catch (e) {
    // some environments block programmatic fullscreen — proceed anyway
  }

  try {
    questions = await apiRequest(`/student/attempts/${attemptId}/questions`);
  } catch (e) {
    alert('Could not load questions: ' + e.message);
    btn.disabled = false;
    btn.textContent = 'Enter Full-Screen & Start Test';
    return;
  }

  document.getElementById('exam-title').textContent = document.getElementById('join-title').textContent;
  showView('view-exam');
  examInProgress = true;
  renderQuestion(0);
  startTimer(durationMinutes);
}

// ---- Question rendering ----
function renderQuestion(idx) {
  currentIndex = idx;
  const q = questions[idx];
  const opts = [['A', q.optionA], ['B', q.optionB], ['C', q.optionC], ['D', q.optionD]];
  const panel = document.getElementById('question-panel');

  panel.innerHTML = `
    <div class="omr-qnum">Question ${idx + 1} of ${questions.length}</div>
    <div class="omr-qtext">${escapeHtml(q.text)}</div>
    <div class="omr-options">
      ${opts.map(([letter, text]) => `
        <div class="omr-option ${answers[q.id] === letter ? 'selected' : ''}" data-letter="${letter}">
          <div class="omr-bubble">${letter}</div>
          <div class="omr-option-text">${escapeHtml(text)}</div>
        </div>
      `).join('')}
    </div>
    <div class="row mt-24">
      <button class="btn btn-outline" id="prev-btn" ${idx === 0 ? 'disabled' : ''}>Previous</button>
      <button class="btn btn-outline" id="next-btn" ${idx === questions.length - 1 ? 'disabled' : ''}>Next</button>
    </div>
  `;

  panel.querySelectorAll('.omr-option').forEach(el => {
    el.addEventListener('click', () => {
      answers[q.id] = el.dataset.letter;
      renderQuestion(idx);
    });
  });
  document.getElementById('prev-btn').addEventListener('click', () => renderQuestion(idx - 1));
  document.getElementById('next-btn').addEventListener('click', () => renderQuestion(idx + 1));

  renderQnav();
}

function renderQnav() {
  const grid = document.getElementById('qnav-grid');
  grid.innerHTML = questions.map((q, i) => `
    <div class="qnav-cell ${answers[q.id] ? 'answered' : ''} ${i === currentIndex ? 'current' : ''}" data-idx="${i}">${i + 1}</div>
  `).join('');
  grid.querySelectorAll('.qnav-cell').forEach(el => {
    el.addEventListener('click', () => renderQuestion(parseInt(el.dataset.idx, 10)));
  });
}

// ---- Timer ----
function startTimer(minutes) {
  deadline = Date.now() + minutes * 60 * 1000;
  updateTimer();
  timerHandle = setInterval(updateTimer, 1000);
}

function updateTimer() {
  const remainingMs = deadline - Date.now();
  const el = document.getElementById('exam-timer');
  if (remainingMs <= 0) {
    el.textContent = '00:00';
    clearInterval(timerHandle);
    submitTest('TIME_UP');
    return;
  }
  const totalSec = Math.floor(remainingMs / 1000);
  const mm = String(Math.floor(totalSec / 60)).padStart(2, '0');
  const ss = String(totalSec % 60).padStart(2, '0');
  el.textContent = `${mm}:${ss}`;
  el.classList.toggle('low', totalSec <= 120);
}

// ---- Anti-cheat ----
async function handleViolation() {
  try {
    const res = await apiRequest(`/student/attempts/${attemptId}/violation`, {
      method: 'POST',
      body: { type: 'TAB_SWITCH' }
    });
    if (res.limitReached) {
      submitTest('TAB_SWITCH_LIMIT');
    } else {
      showTabWarning(res.violationCount);
    }
  } catch (e) {
    // don't block the exam over a logging failure
  }
}

function showTabWarning(count) {
  const remaining = Math.max(0, 3 - count);
  document.getElementById('tab-warning-title').textContent = 'Tab switch detected';
  document.getElementById('tab-warning-body').textContent =
    `Warning ${count} of 2. Switching away ${remaining} more time${remaining === 1 ? '' : 's'} will auto-submit your test.`;
  document.getElementById('overlay-tab-warning').classList.remove('hidden');
}

// ---- Submit ----
async function submitTest(reason) {
  if (submitted) return;
  submitted = true;
  examInProgress = false;
  clearInterval(timerHandle);

  intentionalExit = true;
  if (document.fullscreenElement) {
    try { await document.exitFullscreen(); } catch (e) { /* ignore */ }
  }

  try {
    const res = await apiRequest(`/student/attempts/${attemptId}/submit`, {
      method: 'POST',
      body: { answers, endReason: reason }
    });
    showResult(res, reason);
  } catch (e) {
    alert('Submit failed: ' + e.message + ' — please contact the admin.');
  }
}

function showResult(res, reason) {
  showView('view-result');
  document.getElementById('result-score').textContent = res.score;
  document.getElementById('result-sub').textContent = 'out of ' + res.total;

  const reasonText = {
    MANUAL: '',
    TIME_UP: 'Auto-submitted — time was up.',
    FULLSCREEN_EXIT: 'Auto-submitted — full-screen mode was exited.',
    TAB_SWITCH_LIMIT: 'Auto-submitted — too many tab switches.'
  };
  document.getElementById('result-reason').textContent = reasonText[reason] || '';
  document.getElementById('overlay-fullscreen-exit').classList.add('hidden');
  document.getElementById('overlay-tab-warning').classList.add('hidden');
}
