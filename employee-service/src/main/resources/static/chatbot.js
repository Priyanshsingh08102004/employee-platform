// Rule-based assistant widget. Talks to POST /api/chatbot/ask (see ChatbotService.java).
(function () {
  const style = document.createElement('style');
  style.textContent = `
    #chatbotToggle { position:fixed; bottom:20px; right:20px; width:56px; height:56px; border-radius:50%;
      background:var(--accent,#0b6e6e); color:var(--accent-ink,#fff); border:none; font-size:1.4rem;
      cursor:pointer; box-shadow:0 4px 14px rgba(0,0,0,.25); z-index:50; }
    #chatbotPanel { position:fixed; bottom:86px; right:20px; width:300px; max-width:86vw; max-height:420px;
      background:var(--surface,#fff); border:1px solid var(--line,#d9e0e8); border-radius:12px;
      display:flex; flex-direction:column; z-index:50; box-shadow:0 10px 30px rgba(0,0,0,.25); overflow:hidden; }
    #chatbotPanel[hidden] { display:none; }
    #chatbotHead { padding:12px 14px; font-weight:700; background:var(--accent,#0b6e6e); color:var(--accent-ink,#fff); }
    #chatbotLog { flex:1; overflow-y:auto; padding:10px 12px; font-size:.85rem; display:flex; flex-direction:column; gap:8px; max-height:280px; }
    .cb-msg { padding:8px 10px; border-radius:8px; max-width:85%; }
    .cb-bot { background:var(--bg,#eef2f6); color:var(--ink,#14202e); align-self:flex-start; }
    .cb-user { background:var(--accent,#0b6e6e); color:var(--accent-ink,#fff); align-self:flex-end; }
    #chatbotForm { display:flex; border-top:1px solid var(--line,#d9e0e8); }
    #chatbotInput { flex:1; border:none; padding:10px; font:inherit; background:var(--surface,#fff); color:var(--ink,#14202e); }
    #chatbotForm button { border:none; background:var(--accent,#0b6e6e); color:var(--accent-ink,#fff); padding:0 14px; cursor:pointer; font:inherit; font-weight:700; }
  `;
  document.head.appendChild(style);

  const toggle = document.createElement('button');
  toggle.id = 'chatbotToggle';
  toggle.textContent = '💬';
  toggle.setAttribute('aria-label', 'Open assistant');

  const panel = document.createElement('div');
  panel.id = 'chatbotPanel';
  panel.hidden = true;
  panel.innerHTML = `
    <div id="chatbotHead">Assistant</div>
    <div id="chatbotLog"></div>
    <form id="chatbotForm">
      <input id="chatbotInput" type="text" placeholder="Ask me anything..." autocomplete="off">
      <button type="submit">Send</button>
    </form>`;

  document.body.appendChild(toggle);
  document.body.appendChild(panel);

  const log = panel.querySelector('#chatbotLog');
  function addMsg(text, who) {
    const div = document.createElement('div');
    div.className = 'cb-msg cb-' + who;
    div.textContent = text;
    log.appendChild(div);
    log.scrollTop = log.scrollHeight;
  }

  let greeted = false;
  toggle.addEventListener('click', () => {
    panel.hidden = !panel.hidden;
    if (!panel.hidden && !greeted) {
      addMsg('Hi! Ask me about applying, checking your status, or using the dashboard.', 'bot');
      greeted = true;
    }
  });

  panel.querySelector('#chatbotForm').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const input = panel.querySelector('#chatbotInput');
    const message = input.value.trim();
    if (!message) return;
    addMsg(message, 'user');
    input.value = '';
    try {
      const res = await fetch('/api/chatbot/ask', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message })
      });
      const data = await res.json();
      addMsg(data.reply, 'bot');
    } catch (e) {
      addMsg('Sorry, I could not reach the assistant right now.', 'bot');
    }
  });
})();
