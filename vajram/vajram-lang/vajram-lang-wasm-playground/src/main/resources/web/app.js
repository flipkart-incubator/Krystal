const files = new Map([['main.vajram', `package playground;

vajram hello(string name) out string
    permit callers \`outsideProcess public{
  { "Hello, " + name }
}
`]]);
let active = 'main.vajram';
let artifact;
let publicVajrams = [];
const $ = (selector) => document.querySelector(selector);
function escapeHtml(text) {
  return text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
}
const KEYWORDS = 'callers dependers else err false for from if import in inject loop new nil out package permit public skip switch this throw true vajram void yield';
const DATATYPES = 'bool float32 float64 int128 int32 int64 string uint128 uint32 uint64';
CodeMirror.defineSimpleMode('vajram', {
  start: [
    {regex: /\/\/.*/, token: 'comment'},
    {regex: /`[A-Za-z_][A-Za-z0-9_]*/, token: 'annotation'},
    {regex: new RegExp(`\\b(?:${KEYWORDS.replaceAll(' ', '|')})\\b|[~*]`), token: 'keyword'},
    {regex: new RegExp(`\\b(?:${DATATYPES.replaceAll(' ', '|')})\\b`), token: 'datatype'},
    {regex: /"(?:[^\\]|\\.)*?"/, token: 'string'},
  ],
});
const editor = CodeMirror($('#editor'), {
  value: files.get(active),
  mode: 'vajram',
  lineNumbers: true,
  indentUnit: 2,
  tabSize: 2,
  viewportMargin: Infinity,
});
let updatingEditor = false;
function updateEditor(source) {
  updatingEditor = true;
  editor.setValue(source);
  updatingEditor = false;
}
function renderMarkdown(source) {
  const lines = source.replaceAll('\r\n', '\n').split('\n');
  const output = []; let inCode = false; let inList = false; let paragraph = [];
  const flushParagraph = () => { if (paragraph.length) { output.push(`<p>${inlineMarkdown(paragraph.join(' '))}</p>`); paragraph = []; } };
  const closeList = () => { if (inList) { output.push('</ul>'); inList = false; } };
  for (const line of lines) {
    if (line.startsWith('```')) { flushParagraph(); closeList(); output.push(inCode ? '</code></pre>' : '<pre><code>'); inCode = !inCode; continue; }
    if (inCode) { output.push(`${escapeHtml(line)}\n`); continue; }
    const heading = line.match(/^(#{1,3})\s+(.+)$/);
    if (heading) { flushParagraph(); closeList(); const level = heading[1].length; output.push(`<h${level}>${inlineMarkdown(heading[2])}</h${level}>`); continue; }
    const item = line.match(/^[-*]\s+(.+)$/);
    if (item) { flushParagraph(); if (!inList) { output.push('<ul>'); inList = true; } output.push(`<li>${inlineMarkdown(item[1])}</li>`); continue; }
    if (!line.trim()) { flushParagraph(); closeList(); continue; }
    paragraph.push(line.trim());
  }
  flushParagraph(); closeList(); if (inCode) { output.push('</code></pre>'); }
  return output.join('');
}
function inlineMarkdown(text) { return escapeHtml(text).replace(/`([^`]+)`/g, '<code>$1</code>').replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>'); }
function renderTabs() {
  editor.setOption('readOnly', active.endsWith('.sample.vajram'));
  $('#tabs').replaceChildren(...[...files.keys()].map((name) => {
    const button = document.createElement('button');
    button.className = `tab ${name === active ? 'active' : ''}`;
    button.textContent = name;
    button.onclick = () => { save(); active = name; updateEditor(files.get(active)); renderTabs(); };
    return button;
  }));
}
function save() { files.set(active, editor.getValue()); }
function diagnostics(items) {
  $('#diagnostics').replaceChildren(...items.map((item) => {
    const element = document.createElement('div'); element.className = 'diagnostic';
    element.textContent = `${item.file || 'compiler'}:${item.line}:${item.column} ${item.message}`;
    return element;
  }));
}
function setState(text) { $('#state').textContent = text; }
function selectVajram() {
  const select = $('#vajram-select');
  select.disabled = publicVajrams.length === 0;
  select.replaceChildren(...publicVajrams.map((vajram) => new Option(vajram.name, vajram.function)));
}
function invalidateCompilation() {
  artifact = undefined;
  publicVajrams = [];
  $('#run').disabled = true;
  $('#vajram-select').disabled = true;
  $('#vajram-select').replaceChildren(new Option('Compile source first'));
  setState('Edited - compile required');
}
async function compile() {
  save(); diagnostics([]); setState('Compiling...'); $('#output').textContent = 'Building WASM artifact locally...';
  const response = await fetch('/api/compile', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({files: [...files].map(([name, content]) => ({name, content}))}) });
  const payload = await response.json();
  if (!response.ok) { diagnostics(payload.diagnostics || [{file: '', line: 1, column: 1, message: payload.error || 'Compilation failed'}]); setState('Compilation failed'); $('#output').textContent = 'Fix the diagnostics above and run again.'; return; }
  artifact = payload.artifact; publicVajrams = payload.publicVajrams; selectVajram();
  $('#run').disabled = publicVajrams.length === 0;
  setState('Compiled');
  $('#output').textContent = publicVajrams.length ? 'Choose a public Vajram and run it in the browser.' : 'No public `outsideProcess` Vajram was compiled.';
}
async function run() {
  if (!artifact) { return; }
  try {
    setState('Running in browser...');
    const inputs = JSON.parse($('#inputs').value);
    const selected = publicVajrams.find((item) => item.function === $('#vajram-select').value);
    const module = await import(`/api/artifacts/${artifact}/vajram_playground.js`);
    await module.default();
    const values = selected.inputs.map((input) => input.type === 'i64' ? BigInt(inputs[input.name]) : String(inputs[input.name] ?? ''));
    const result = await module[selected.function](...values);
    $('#output').textContent = typeof result === 'bigint' ? result.toString() : result;
    setState('Browser execution complete');
  } catch (error) { $('#output').textContent = error.stack || error.message; setState('Runtime error'); }
}

document.querySelectorAll('.nav-item').forEach((button) => button.onclick = async () => {
  document.querySelectorAll('.nav-item, .section').forEach((element) => element.classList.remove('active'));
  button.classList.add('active'); $(`#${button.dataset.section}`).classList.add('active');
  if (button.dataset.section === 'spec' && !$('#specification').dataset.loaded) { $('#specification').innerHTML = renderMarkdown(await (await fetch('/api/spec')).text()); $('#specification').dataset.loaded = 'true'; }
});
$('#new-file').onclick = () => { const name = prompt('File name', 'new.vajram'); if (name && !files.has(name)) { save(); files.set(name, ''); active = name; updateEditor(''); renderTabs(); invalidateCompilation(); } };
$('#copy-file').onclick = async () => { await navigator.clipboard.writeText(editor.getValue()); $('#copy-file').textContent = 'Copied'; setTimeout(() => { $('#copy-file').textContent = 'Copy'; }, 900); };
$('#compile').onclick = compile;
$('#run').onclick = run;
editor.on('change', () => { if (!updatingEditor) { invalidateCompilation(); } });
renderTabs();
fetch('/api/health').then((response) => { $('#server-status').textContent = response.ok ? 'Local compiler ready' : 'Local compiler unavailable'; }).catch(() => { $('#server-status').textContent = 'Local compiler unavailable'; });
fetch('/api/samples').then((response) => response.json()).then((samples) => { $('#sample-list').replaceChildren(...Object.entries(samples).map(([name, source]) => { const article = document.createElement('article'); article.innerHTML = `<p class="eyebrow">${name.replace('-', ' ').toUpperCase()}</p><h3>${name}</h3><p>Open as a read-only reference, then copy it into an editor tab.</p>`; const button = document.createElement('button'); button.textContent = 'Open in Playground'; button.onclick = () => { const tab = `${name}.sample.vajram`; files.set(tab, source); active = tab; updateEditor(source); renderTabs(); invalidateCompilation(); document.querySelector('[data-section="playground"]').click(); }; article.append(button); return article; })); });
