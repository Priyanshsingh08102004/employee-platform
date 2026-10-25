const API = '/api/employees';
const $ = id => document.getElementById(id);
const fields = ['name', 'email', 'departmentId', 'jobTitle', 'salary', 'joinDate'];

let deleteId = null;
let currentPage = 0;
const pageSize = 10;
let isAdmin = false;
let departments = [];

const money = n => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const initials = name => (name || '?').trim().split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('');

function toast(msg) {
  const t = $('toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toast.timer); toast.timer = setTimeout(() => t.classList.remove('show'), 2200);
}

// Who am I? Used to hide admin-only actions (deleting employees) from regular staff accounts.
async function loadWhoAmI() {
  const res = await fetch('/api/auth/me');
  const data = await res.json();
  isAdmin = !!data.isAdmin;
  const reviewLink = document.getElementById('reviewLink');
  if (reviewLink) reviewLink.hidden = !isAdmin;
  const payrollLink = document.getElementById('payrollLink');
  if (payrollLink) payrollLink.hidden = !isAdmin;
}

// Departments power both the filter dropdown and the add/edit form's dropdown.
async function loadDepartments() {
  const res = await fetch('/api/departments');
  departments = await res.json();

  const filter = $('departmentFilter');
  const formSelect = $('departmentId');
  filter.innerHTML = '<option value="">All departments</option>' +
    departments.map(d => `<option value="${d.id}">${esc(d.name)}</option>`).join('');
  formSelect.innerHTML = departments.map(d => `<option value="${d.id}">${esc(d.name)}</option>`).join('');
}

// READ (paginated + searchable + filterable)
async function load() {
  const q = $('search').value.trim();
  const departmentId = $('departmentFilter').value;
  const params = new URLSearchParams({ page: currentPage, size: pageSize, sort: 'name' });
  if (q) params.set('q', q);
  if (departmentId) params.set('departmentId', departmentId);

  const res = await fetch(`${API}?${params.toString()}`);
  const page = await res.json();
  const list = page.content ?? [];

  $('count').textContent = `${page.totalElements} ${page.totalElements === 1 ? 'employee' : 'employees'}`;
  $('empty').hidden = list.length > 0;
  $('pageInfo').textContent = `Page ${page.number + 1} of ${Math.max(page.totalPages, 1)}`;
  $('prevPage').disabled = page.first;
  $('nextPage').disabled = page.last;

  $('rows').innerHTML = list.map(e => `
    <tr>
      <td>${e.profileImageUrl
          ? `<img class="avatar" src="${e.profileImageUrl}" alt="">`
          : `<span class="avatar initials">${esc(initials(e.name))}</span>`}</td>
      <td><strong>${esc(e.name)}</strong><span class="email">${esc(e.email)}</span></td>
      <td>${esc(e.departmentName ?? '')}</td>
      <td>${esc(e.jobTitle)}</td>
      <td class="num">${money(e.salary)}</td>
      <td>${esc(e.joinDate ?? '')}</td>
      <td class="actions">
        <button class="btn small" data-edit="${e.id}">Edit</button>
        ${isAdmin ? `<button class="btn small" data-del="${e.id}" data-name="${esc(e.name)}">Delete</button>` : ''}
      </td>
    </tr>`).join('');
}

function clearErrors() {
  document.querySelectorAll('.err').forEach(el => el.textContent = '');
}

function openForm(emp) {
  clearErrors();
  $('formTitle').textContent = emp ? 'Edit employee' : 'Add employee';
  $('id').value = emp?.id ?? '';
  $('photo').value = '';
  fields.forEach(f => {
    const key = f === 'departmentId' ? 'departmentId' : f;
    $(f).value = emp?.[key] ?? '';
  });
  $('formDialog').showModal();
  $('name').focus();
}

// CREATE + UPDATE
$('form').addEventListener('submit', async ev => {
  ev.preventDefault();
  clearErrors();
  const id = $('id').value;
  const body = {
    name: $('name').value.trim(),
    email: $('email').value.trim(),
    jobTitle: $('jobTitle').value.trim(),
    salary: Number($('salary').value),
    joinDate: $('joinDate').value || null,
    departmentId: Number($('departmentId').value)
  };

  const res = await fetch(id ? `${API}/${id}` : API, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    if (data.errors) {
      Object.entries(data.errors).forEach(([f, m]) => {
        const el = document.querySelector(`.err[data-for="${f}"]`);
        if (el) el.textContent = m;
      });
    } else {
      $('formErr').textContent = data.message || 'Could not save. Try again.';
    }
    return;
  }

  const saved = await res.json();
  const file = $('photo').files[0];
  if (file) {
    const fd = new FormData();
    fd.append('file', file);
    await fetch(`${API}/${saved.id}/photo`, { method: 'POST', body: fd });
  }

  $('formDialog').close();
  toast(id ? 'Employee updated' : 'Employee added');
  load();
});

// DELETE + EDIT (event delegation on the table body)
$('rows').addEventListener('click', async ev => {
  const editId = ev.target.dataset.edit;
  const delId = ev.target.dataset.del;
  if (editId) {
    const res = await fetch(`${API}/${editId}`);
    openForm(await res.json());
  } else if (delId) {
    deleteId = delId;
    $('confirmText').textContent = `${ev.target.dataset.name} will be removed permanently.`;
    $('confirmDialog').showModal();
  }
});

$('confirmYes').addEventListener('click', async () => {
  const res = await fetch(`${API}/${deleteId}`, { method: 'DELETE' });
  $('confirmDialog').close();
  if (res.ok) {
    toast('Employee deleted');
    load();
  } else {
    toast('You do not have permission to delete employees');
  }
});
$('confirmNo').addEventListener('click', () => $('confirmDialog').close());
$('cancelBtn').addEventListener('click', () => $('formDialog').close());
$('addBtn').addEventListener('click', () => openForm());
$('emptyAdd').addEventListener('click', () => openForm());

$('prevPage').addEventListener('click', () => { if (currentPage > 0) { currentPage--; load(); } });
$('nextPage').addEventListener('click', () => { currentPage++; load(); });
$('departmentFilter').addEventListener('change', () => { currentPage = 0; load(); });

let timer;
$('search').addEventListener('input', () => {
  clearTimeout(timer);
  timer = setTimeout(() => { currentPage = 0; load(); }, 250);
});

(async function init() {
  await loadWhoAmI();
  await loadDepartments();
  await load();
})();
