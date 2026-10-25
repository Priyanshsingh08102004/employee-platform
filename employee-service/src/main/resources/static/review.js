const $ = id => document.getElementById(id);
let rejectId = null;
let currentList = [];

function toast(msg) {
  const t = $('toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toast.timer); toast.timer = setTimeout(() => t.classList.remove('show'), 2200);
}
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

async function loadDepartments() {
  const res = await fetch('/api/departments');
  const departments = await res.json();
  $('editDepartmentId').innerHTML = departments.map(d => `<option value="${d.id}">${esc(d.name)}</option>`).join('');
}

async function load() {
  const status = $('statusFilter').value;
  const res = await fetch(`/api/applications?status=${status}`);
  if (res.status === 403) {
    document.body.innerHTML = '<main style="padding:60px 20px;text-align:center;"><p>You need an Admin account to view this page.</p><a class="btn" href="/index.html">Back to dashboard</a></main>';
    return;
  }
  currentList = await res.json();
  $('count').textContent = `${currentList.length} ${currentList.length === 1 ? 'application' : 'applications'}`;
  $('empty').hidden = currentList.length > 0;

  $('rows').innerHTML = currentList.map(s => `
    <tr>
      <td><strong>${esc(s.name)}</strong></td>
      <td>${esc(s.email)}</td>
      <td>${esc(s.department ? s.department.name : '')}</td>
      <td>${esc(s.jobTitle)}</td>
      <td class="num">${money(s.expectedSalary)}</td>
      <td>${s.resumeFilePath ? `<a href="/uploads/${s.resumeFilePath}" target="_blank" rel="noopener">View</a>` : '—'}</td>
      <td class="actions">
        ${status === 'PENDING' ? `
          <button class="btn small" data-edit="${s.id}">Correct</button>
          <button class="btn small" data-approve="${s.id}">Approve</button>
          <button class="btn small" data-reject="${s.id}">Reject</button>
        ` : `<span>${esc(s.managerNote || '')}</span>`}
      </td>
    </tr>`).join('');
}

function openEdit(s) {
  $('editId').value = s.id;
  $('editName').value = s.name;
  $('editEmail').value = s.email;
  $('editPhone').value = s.phone || '';
  $('editDepartmentId').value = s.department ? s.department.id : '';
  $('editJobTitle').value = s.jobTitle;
  $('editSalary').value = s.expectedSalary;
  $('editDialog').showModal();
}

$('rows').addEventListener('click', async (ev) => {
  const editId = ev.target.dataset.edit;
  const approveId = ev.target.dataset.approve;
  const rejectBtnId = ev.target.dataset.reject;

  if (editId) {
    const s = currentList.find(x => String(x.id) === editId);
    if (s) openEdit(s);
  } else if (approveId) {
    const res = await fetch(`/api/applications/${approveId}/approve`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({})
    });
    if (res.ok) { toast('Approved — employee record created and email sent'); load(); }
    else { toast('Could not approve this application'); }
  } else if (rejectBtnId) {
    rejectId = rejectBtnId;
    $('rejectReason').value = '';
    $('rejectDialog').showModal();
  }
});

$('editForm').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const id = $('editId').value;
  const body = {
    name: $('editName').value.trim(),
    email: $('editEmail').value.trim(),
    phone: $('editPhone').value.trim(),
    jobTitle: $('editJobTitle').value.trim(),
    expectedSalary: Number($('editSalary').value),
    departmentId: Number($('editDepartmentId').value)
  };
  const res = await fetch(`/api/applications/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  if (res.ok) { $('editDialog').close(); toast('Correction saved'); load(); }
  else { toast('Could not save the correction'); }
});
$('editCancel').addEventListener('click', () => $('editDialog').close());

$('rejectConfirm').addEventListener('click', async () => {
  const reason = $('rejectReason').value.trim();
  const res = await fetch(`/api/applications/${rejectId}/reject`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason })
  });
  $('rejectDialog').close();
  if (res.ok) { toast('Rejected — applicant notified by email'); load(); }
  else { toast('Could not reject this application'); }
});
$('rejectCancel').addEventListener('click', () => $('rejectDialog').close());

$('statusFilter').addEventListener('change', load);

(async function init() {
  await loadDepartments();
  await load();
})();
