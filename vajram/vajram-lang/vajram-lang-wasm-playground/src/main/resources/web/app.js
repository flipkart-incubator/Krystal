const files = new Map([['main.vajram', `package playground;

vajram hello(string name) out string
    permit callers \`outsideProcess public{
  { "Hello, " + name }
}
`]]);
let active = 'main.vajram';
let artifact;
let publicVajrams = [];
let selectedSample;
let playgroundOpened = false;
const $ = (selector) => document.querySelector(selector);
const KEYWORDS = 'callers dependers else err false for from if import in inject loop new nil out package permit public skip switch this throw true vajram void yield';
const DATATYPES = 'bool true false float32 float64 int128 int32 int64 string uint128 uint32 uint64';
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
const sampleEditor = CodeMirror($('#sample-editor'), {
  value: 'Select a sample from the left to preview its source.',
  mode: 'vajram',
  lineNumbers: true,
  readOnly: 'nocursor',
  viewportMargin: Infinity,
});
let updatingEditor = false;
function updateEditor(source) {
  updatingEditor = true;
  editor.setValue(source);
  updatingEditor = false;
}
function showSample(name, source) {
  selectedSample = {name, source};
  sampleEditor.setValue(source);
  $('#sample-category').textContent = name.replaceAll('-', ' ').toUpperCase();
  $('#sample-title').textContent = `${name}.vajram`;
  $('#open-sample').disabled = false;
  document.querySelectorAll('.sample-list button').forEach((button) => {
    button.classList.toggle('active', button.dataset.sample === name);
  });
}
function renderTabs() {
  editor.setOption('readOnly', false);
  $('#tabs').replaceChildren(...[...files.keys()].map((name) => {
    const tab = document.createElement('span');
    tab.className = `tab ${name === active ? 'active' : ''}`;
    const select = document.createElement('button');
    select.className = 'tab-select';
    select.textContent = name;
    select.onclick = () => { save(); active = name; updateEditor(files.get(active)); renderTabs(); };
    tab.append(select);
    if (name !== 'main.vajram') {
      const close = document.createElement('button');
      close.className = 'tab-close';
      close.textContent = 'x';
      close.setAttribute('aria-label', `Close ${name}`);
      close.onclick = () => {
        save();
        files.delete(name);
        if (active === name) {
          active = 'main.vajram';
          updateEditor(files.get(active));
        }
        renderTabs();
        invalidateCompilation();
      };
      tab.append(close);
    }
    return tab;
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
  renderInputs();
}
function renderInputs() {
  const selected = publicVajrams.find((item) => item.function === $('#vajram-select').value);
  $('#input-form').replaceChildren(...(selected?.inputs ?? []).map((input) => {
    const label = document.createElement('label');
    label.textContent = `${input.name} (${input.type})`;
    const field = document.createElement('input');
    field.name = input.name;
    field.type = input.type === 'i64' ? 'number' : 'text';
    field.step = input.type === 'i64' ? '1' : 'any';
    field.required = true;
    label.append(field);
    return label;
  }));
}
function invalidateCompilation() {
  artifact = undefined;
  publicVajrams = [];
  $('#run').disabled = true;
  $('#vajram-select').disabled = true;
  $('#vajram-select').replaceChildren(new Option('Compile source first'));
  $('#input-form').replaceChildren();
  setState('Edited - compile required');
}
function activateSection(section) {
  const firstPlaygroundOpen = section === 'playground' && !playgroundOpened;
  if (firstPlaygroundOpen) {
    active = 'main.vajram';
    playgroundOpened = true;
  }
  document.querySelectorAll('.nav-item, .section').forEach((element) => element.classList.remove('active'));
  document.querySelector(`[data-section="${section}"]`).classList.add('active');
  $(`#${section}`).classList.add('active');
  if (section === 'playground') {
    if (firstPlaygroundOpen) {
      updateEditor(files.get(active));
      renderTabs();
    }
    requestAnimationFrame(() => {
      editor.refresh();
      editor.focus();
    });
  }
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
    const selected = publicVajrams.find((item) => item.function === $('#vajram-select').value);
    const valuesByName = Object.fromEntries([...$('#input-form').querySelectorAll('input')].map((field) => [field.name, field.value]));
    const module = await import(`/api/artifacts/${artifact}/vajram_playground.js`);
    await module.default();
    const values = selected.inputs.map((input) => input.type === 'i64' ? BigInt(valuesByName[input.name]) : String(valuesByName[input.name] ?? ''));
    const result = await module[selected.function](...values);
    $('#output').textContent = typeof result === 'bigint' ? result.toString() : result;
    setState('Browser execution complete');
  } catch (error) { $('#output').textContent = error.stack || error.message; setState('Runtime error'); }
}

document.querySelectorAll('.nav-item').forEach((button) => button.onclick = async () => {
  activateSection(button.dataset.section);
  if (button.dataset.section === 'spec' && !$('#specification').dataset.loaded) { $('#specification').innerHTML = marked.parse(await (await fetch('/api/spec')).text()); $('#specification').dataset.loaded = 'true'; }
});
$('#new-file').onclick = () => { const name = prompt('File name', 'new.vajram'); if (name && !files.has(name)) { save(); files.set(name, ''); active = name; updateEditor(''); renderTabs(); invalidateCompilation(); } };
$('#copy-file').onclick = async () => { await navigator.clipboard.writeText(editor.getValue()); $('#copy-file').textContent = 'Copied'; setTimeout(() => { $('#copy-file').textContent = 'Copy'; }, 900); };
$('#compile').onclick = compile;
$('#run').onclick = run;
$('#vajram-select').onchange = renderInputs;
editor.on('change', () => { if (!updatingEditor) { invalidateCompilation(); } });
renderTabs();
fetch('/api/health').then((response) => { $('#server-status').textContent = response.ok ? 'Local compiler ready' : 'Local compiler unavailable'; }).catch(() => { $('#server-status').textContent = 'Local compiler unavailable'; });
$('#open-sample').onclick = () => {
  if (!selectedSample) { return; }
  let fileName = `${selectedSample.name}.vajram`;
  let suffix = 2;
  while (files.has(fileName)) { fileName = `${selectedSample.name}-${suffix}.vajram`; suffix += 1; }
  files.set(fileName, selectedSample.source);
  active = fileName;
  updateEditor(selectedSample.source);
  renderTabs();
  invalidateCompilation();
  playgroundOpened = true;
  activateSection('playground');
};
fetch('/api/samples').then((response) => response.json()).then((samples) => {
  $('#sample-list').replaceChildren(...Object.entries(samples).map(([name, source]) => {
    const button = document.createElement('button');
    button.dataset.sample = name;
    button.textContent = name.replaceAll('-', ' ');
    button.onclick = () => showSample(name, source);
    return button;
  }));
});
