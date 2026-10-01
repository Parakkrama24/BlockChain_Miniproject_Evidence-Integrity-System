const API_BASE = 'http://localhost:8080';

const casesSection = document.getElementById('casesSection');
const auditBody = document.getElementById('auditBody');
const investigatorList = document.getElementById('investigatorList');
const refreshBtn = document.getElementById('refreshBtn');
const investigatorForm = document.getElementById('investigatorForm');
const addressInput = document.getElementById('addressInput');
const caseCountEl = document.getElementById('caseCount');
const evidenceCountEl = document.getElementById('evidenceCount');
const investigatorCountEl = document.getElementById('investigatorCount');

function formatTimestamp(ts) {
  return new Date(ts * 1000).toLocaleString();
}

async function fetchJson(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });

  if (!res.ok) {
    throw new Error(await res.text() || `Request failed: ${res.status}`);
  }

  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function loadCases() {
  const cases = await fetchJson(`${API_BASE}/api/cases`);

  caseCountEl.textContent = cases.length;

  const allCaseCards = await Promise.all(
    cases.map(async (item) => {
      const evidence = await fetchJson(`${API_BASE}/api/cases/${encodeURIComponent(item.caseId)}/evidence`);
      return { ...item, evidence };
    })
  );

  const totalEvidence = allCaseCards.reduce((sum, item) => sum + item.evidence.length, 0);
  evidenceCountEl.textContent = totalEvidence;

  casesSection.innerHTML = allCaseCards
    .map(
      (caseItem) => `
        <article class="case-card">
          <div class="case-header">
            <h3>${caseItem.caseId}</h3>
            <span>${caseItem.count} item(s)</span>
          </div>
          <ul class="evidence-list">
            ${
              caseItem.evidence.length
                ? caseItem.evidence
                    .map(
                      (e) => `
                        <li class="evidence-item">
                          <strong>${e.fileName}</strong>
                          <div class="meta">
                            <span>ID ${e.evidenceId}</span>
                            <span>${formatTimestamp(e.timestamp)}</span>
                          </div>
                          <div class="hash">${e.sha256Hash}</div>
                        </li>
                      `
                    )
                    .join('')
                : '<li class="evidence-item">No evidence registered for this case.</li>'
            }
          </ul>
        </article>
      `
    )
    .join('');
}

async function loadAuditTrail() {
  const trail = await fetchJson(`${API_BASE}/api/audit-trail`);

  auditBody.innerHTML = trail
    .map(
      (item) => `
        <tr>
          <td>${item.evidenceId}</td>
          <td>${item.caseId}</td>
          <td class="small-code">${item.uploader}</td>
          <td>${item.fileName}</td>
          <td>${formatTimestamp(item.timestamp)}</td>
          <td class="small-code">${item.txHash}</td>
        </tr>
      `
    )
    .join('');
}

async function loadInvestigators() {
  const investigators = await fetchJson(`${API_BASE}/api/investigators`);

  investigatorCountEl.textContent = investigators.length;
  investigatorList.innerHTML = investigators
    .map((address) => `<span class="chip">${address}</span>`)
    .join('');
}

async function authorizeInvestigator(event) {
  event.preventDefault();
  const errorEl = document.getElementById('investigatorError');
  errorEl.textContent = '';
  const address = addressInput.value.trim();

  if (!address) {
    return;
  }

  try {
    await fetchJson(`${API_BASE}/api/investigators`, {
      method: 'POST',
      body: JSON.stringify({ address })
    });

    addressInput.value = '';
    loadInvestigators();
  } catch (exception) {
    errorEl.textContent = exception.message || 'Authorization failed.';
  }
}

async function loadDemoSigners() {
  const signers = await fetchJson(`${API_BASE}/api/demo-signers`);
  const demoSignersList = document.getElementById('demoSigners');
  demoSignersList.innerHTML = signers
    .map((s) => `<option value="${s.address}">${s.label}</option>`)
    .join('');
}

refreshBtn.addEventListener('click', async () => {
  await Promise.all([loadCases(), loadAuditTrail(), loadInvestigators()]);
});

investigatorForm.addEventListener('submit', authorizeInvestigator);

loadCases();
loadAuditTrail();
loadInvestigators();
loadDemoSigners();
