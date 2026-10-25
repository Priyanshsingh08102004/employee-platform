const $ = id => document.getElementById(id);
const money = n => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

function toast(msg) {
  const t = $('toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toast.timer); toast.timer = setTimeout(() => t.classList.remove('show'), 2500);
}

// Every call goes to THIS app (/api/payroll/...), never straight to the .NET service.
async function call(path, options) {
  const res = await fetch(path, options);
  if (res.redirected && res.url.includes('/login')) { location.href = '/login.html'; return null; }
  let data = null;
  try { data = await res.json(); } catch (e) { /* empty or non-JSON body */ }
  return { ok: res.ok, status: res.status, data };
}

function explain(r) {
  if (r.status === 403) return 'Only Admin accounts can use payroll.';
  if (r.status === 503) return 'The payroll service is not running right now. Start it and try again.';
  return (r.data && (r.data.message || r.data.detail || r.data.title)) || 'Something went wrong. Please try again.';
}

function showMsg(text) { $('msg').textContent = text; $('msg').hidden = !text; }

async function loadRuns() {
  const r = await call('/api/payroll/runs');
  if (!r) return;
  if (!r.ok || !Array.isArray(r.data)) { showMsg(explain(r)); $('runsBody').innerHTML = ''; $('runsEmpty').hidden = true; return; }
  showMsg('');
  $('runsEmpty').hidden = r.data.length > 0;
  $('runsBody').innerHTML = r.data.map(run => `
    <tr>
      <td><strong>${esc(run.month)}</strong></td>
      <td class="num">${esc(run.employeeCount)}</td>
      <td class="num">${money(run.totalNetPay)}</td>
      <td>${esc(String(run.createdAtUtc).replace('T', ' ').slice(0, 16))}</td>
      <td class="actions"><button class="btn small" data-view="${run.id}">View</button></td>
    </tr>`).join('');
}

async function openRun(id) {
  const r = await call(`/api/payroll/runs/${id}`);
  if (!r) return;
  if (!r.ok) { showMsg(explain(r)); return; }
  showMsg('');
  $('detailTitle').textContent = `Payslips for ${r.data.month}`;
  $('detailBody').innerHTML = r.data.records.map(p => `
    <tr>
      <td><strong>${esc(p.employeeName)}</strong><span class="email">${esc(p.employeeEmail)}</span></td>
      <td>${esc(p.department)}</td>
      <td class="num">${money(p.monthlyGross)}</td>
      <td class="num">${money(p.providentFund)}</td>
      <td class="num">${money(p.incomeTax)}</td>
      <td class="num"><strong>${money(p.netPay)}</strong></td>
    </tr>`).join('');
  $('detail').hidden = false;
  $('detail').scrollIntoView({ behavior: 'smooth' });
}

$('runBtn').addEventListener('click', async () => {
  const month = $('month').value;
  if (!month) { showMsg('Pick a month first.'); return; }
  $('runBtn').disabled = true;
  const r = await call(`/api/payroll/runs?month=${encodeURIComponent(month)}`, { method: 'POST' });
  $('runBtn').disabled = false;
  if (!r) return;
  if (!r.ok) { showMsg(explain(r)); return; }
  showMsg('');
  toast(`Payroll for ${month} completed`);
  await loadRuns();
  openRun(r.data.id);
});

$('runsBody').addEventListener('click', ev => {
  const id = ev.target.dataset.view;
  if (id) openRun(id);
});

$('month').value = new Date().toISOString().slice(0, 7);
loadRuns();
