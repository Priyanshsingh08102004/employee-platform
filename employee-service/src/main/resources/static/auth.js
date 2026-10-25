document.getElementById('registerForm').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const box = document.getElementById('authMsg');
  box.hidden = true;

  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;

  const res = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  if (res.ok) {
    location.href = '/login.html?registered';
    return;
  }
  const data = await res.json().catch(() => ({}));
  box.textContent = data.message || 'Could not create the account. Try a different username.';
  box.className = 'auth-msg';
  box.hidden = false;
});
