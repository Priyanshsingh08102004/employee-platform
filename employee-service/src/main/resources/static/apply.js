const $ = id => document.getElementById(id);

function toast(msg) {
  const t = $('toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toast.timer); toast.timer = setTimeout(() => t.classList.remove('show'), 2500);
}

async function loadDepartments() {
  const res = await fetch('/api/departments');
  const departments = await res.json();
  $('departmentId').innerHTML = departments.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
}

$('applyForm').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const box = $('applyMsg');
  box.hidden = true;

  const fd = new FormData();
  fd.append('name', $('name').value.trim());
  fd.append('email', $('email').value.trim());
  fd.append('phone', $('phone').value.trim());
  fd.append('jobTitle', $('jobTitle').value.trim());
  fd.append('expectedSalary', $('expectedSalary').value);
  fd.append('departmentId', $('departmentId').value);
  const file = $('resume').files[0];
  if (file) fd.append('resume', file);

  const res = await fetch('/api/applications', { method: 'POST', body: fd });

  if (res.ok) {
    document.querySelector('.auth-card').innerHTML =
      '<div class="auth-logo">EM</div><h1>Application submitted</h1>' +
      '<p class="sub">A manager will review your details. You will get an email once a decision is made.</p>' +
      '<p class="auth-switch"><a href="/login.html">Back to sign in</a></p>';
    return;
  }
  const data = await res.json().catch(() => ({}));
  box.textContent = data.message || 'Could not submit. Please check your details and try again.';
  box.hidden = false;
});

loadDepartments();
