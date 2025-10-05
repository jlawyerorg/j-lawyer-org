const THEME_STORAGE_KEY = 'jlw-export-theme';

function readPersistedTheme() {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return stored === 'light' ? 'light' : 'dark';
  } catch (e) {
    return 'dark';
  }
}

function persistTheme(theme) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch (e) {
    // ignore persistence issues (storage might be unavailable)
  }
}

let currentTheme = 'dark';

function applyTheme(theme, { persist = true } = {}) {
  currentTheme = theme === 'light' ? 'light' : 'dark';
  document.body.classList.remove('theme-dark', 'theme-light');
  document.body.classList.add(`theme-${currentTheme}`);
  document.body.dataset.theme = currentTheme;
  if (persist) {
    persistTheme(currentTheme);
  }
  updateThemeToggleLabel();
  notifyHistoryFrame();
}

function updateThemeToggleLabel() {
  const toggle = document.getElementById('themeToggle');
  if (!toggle) {
    return;
  }
  const label = toggle.querySelector('.label');
  if (label) {
    label.textContent = currentTheme === 'dark' ? 'Hell' : 'Dunkel';
  }
  toggle.setAttribute('aria-pressed', currentTheme === 'dark' ? 'false' : 'true');
  toggle.setAttribute('aria-label', currentTheme === 'dark' ? 'Zu hellem Theme wechseln' : 'Zu dunklem Theme wechseln');
}

function notifyHistoryFrame() {
  const frame = document.getElementById('historyFrame');
  if (frame && frame.contentWindow) {
    try {
      frame.contentWindow.postMessage({ type: 'JLW_THEME', theme: currentTheme }, '*');
    } catch (e) {
      // ignore messaging errors (file:// or sandbox limitations)
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.DOC_STATE = {
    q: '',
    sortBy: 'name',
    sortDir: 'asc',
    hidden: { medien: false, bilder: false, docs: false, pdf: false, html: false, eml: false, bea: false, md: false }
  };

  window.FILE_TYPE_GROUPS = {
    medien: ['mp3', 'wav', 'flac', 'aac', 'm4a', 'ogg', 'wma', 'mp4', 'mkv', 'avi', 'mov', 'wmv', 'webm', 'mpeg', 'mpg', 'm4v'],
    bilder: ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'tif', 'tiff', 'webp'],
    docs: ['odt', 'doc', 'docx', 'rtf', 'txt', 'xls', 'xlsx', 'ppt', 'pptx', 'odp', 'ods'],
    pdf: ['pdf'],
    html: ['html', 'htm'],
    eml: ['eml'],
    bea: ['bea'],
    md: ['md']
  };

  // Tab navigation
  const tabs = document.querySelectorAll('.tabs .tab');
  const views = document.querySelectorAll('.content .view');
  if (tabs.length && views.length) {
    tabs.forEach(tab => {
      tab.addEventListener('click', () => {
        const targetId = tab.dataset.tab;
        if (!targetId) return;
        
        tabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        
        views.forEach(v => {
          v.classList.toggle('active', v.id === targetId);
        });
      });
    });
  }

  initThemeToggle();
  renderDashboard(CASE_DATA);
  renderDocumentsSection(CASE_DATA);
  renderDeadlinesSection(CASE_DATA);
  renderFinanceSection(CASE_DATA);
  renderHistorySection();
});

function initThemeToggle() {
  applyTheme(readPersistedTheme(), { persist: false });

  const toggle = document.getElementById('themeToggle');
  if (toggle) {
    toggle.addEventListener('click', () => {
      const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
      applyTheme(nextTheme);
    });
  }

  window.addEventListener('storage', evt => {
    if (evt.key === THEME_STORAGE_KEY && evt.newValue) {
      applyTheme(evt.newValue, { persist: false });
    }
  });
}

function renderDashboard(data) {
  const container = document.getElementById('dashboard');
  if (!container) {
    return;
  }
  const tags = (data.tags || []).map(t => `<span class="chip">${escapeHtml(t.name)}</span>`).join(' ');
  const parties = (data.parties || []).map(p => {
    const reference = p.reference ? `<br>Zeichen: ${escapeHtml(p.reference)}` : '';
    const phone = p.phone ? ` · Tel: ${escapeHtml(p.phone)}` : '';
    const mobile = p.mobile ? ` · Mob: ${escapeHtml(p.mobile)}` : '';
    return `<div class="party-item"><div class="title">${escapeHtml(p.name)} (${escapeHtml(p.type)})</div><div class="when">${escapeHtml(p.address)}${phone}${mobile}${reference}</div></div>`;
  }).join('');
  const notes = escapeHtml(data.notice || '');

  container.innerHTML = `
    <div class="cards overview">
      <div class="card"><h3>Grunddaten</h3><div class="kv">
        <div class="label">Aktenzeichen</div><div>${escapeHtml(data.fileNumber || '')}</div>
        <div class="label">Kurzrubrum</div><div>${escapeHtml(data.name || '')}</div>
        <div class="label">Grund</div><div>${escapeHtml(data.reason || '')}</div>
        <div class="label">Sachgebiet</div><div>${escapeHtml(data.subjectField || '')}</div>
        <div class="label">Anwalt</div><div>${escapeHtml(data.lawyer || '')}</div>
        <div class="label">Assistenz</div><div>${escapeHtml(data.assistant || '')}</div>
        <div class="label">Streitwert</div><div>${escapeHtml(data.claimValue || '')}</div>
        <div class="label">Erstellt</div><div>${escapeHtml(data.dateCreated || '')}</div>
        <div class="label">Geändert</div><div>${escapeHtml(data.dateChanged || '')}</div>
      </div></div>
      <div class="card"><h3>Beteiligte</h3><div>${parties || '<span class="when">—</span>'}</div></div>
      <div class="card"><h3>Etiketten</h3><div>${tags || '<span class="when">—</span>'}</div></div>
      <div class="card"><h3>Notizen</h3><div class="note">${notes || '<span class="when">—</span>'}</div></div>
    </div>`;
}

function renderDocumentsSection(data) {
  const docsContainer = document.getElementById('documents');
  if (!docsContainer) {
    return;
  }
  docsContainer.innerHTML = renderDocuments(data, window.DOC_STATE);
  bindDocControls(data);
}

function renderFinanceSection(data) {
  const fin = document.getElementById('finance');
  if (!fin) {
    return;
  }
  fin.innerHTML = renderFinance(data);
}

function renderDeadlinesSection(data) {
  const dl = document.getElementById('deadlines');
  if (!dl) {
    return;
  }
  dl.innerHTML = renderDeadlines(data);
}

function renderHistorySection() {
  const hist = document.getElementById('history');
  if (!hist) {
    return;
  }
  hist.innerHTML = '<iframe id="historyFrame" class="history-frame" src="history.html" title="Aktenhistorie"></iframe>';
  const frame = document.getElementById('historyFrame');
  if (frame) {
    frame.addEventListener('load', () => notifyHistoryFrame());
  }
}

function renderFinance(data) {
  const invoices = (data.invoices || []).map(i => `
    <tr>
      <td data-label="Beleg-Nr.">${escapeHtml(i.invoiceNumber)}</td>
      <td data-label="Name">${escapeHtml(i.name)}</td>
      <td data-label="Erstellt">${escapeHtml(i.creationDate)}</td>
      <td data-label="Fällig">${escapeHtml(i.dueDate)}</td>
      <td data-label="Betrag" class="num">${(i.totalGross || 0).toFixed(2)} ${escapeHtml(i.currency)}</td>
      <td data-label="Status">${escapeHtml(i.status)}</td>
      <td data-label="Kontakt">${escapeHtml(i.contact)}</td>
    </tr>`).join('');

  const accountEntries = (data.accountEntries || []).map(e => `
    <tr>
      <td data-label="Datum">${escapeHtml(e.entryDate)}</td>
      <td data-label="Buchungstext">${escapeHtml(e.description)}</td>
      <td data-label="Einnahmen" class="num">${(e.earnings || 0).toFixed(2)}</td>
      <td data-label="Ausgaben" class="num">${(e.spendings || 0).toFixed(2)}</td>
      <td data-label="Fremdgeld Ein" class="num">${(e.escrowIn || 0).toFixed(2)}</td>
      <td data-label="Fremdgeld Aus" class="num">${(e.escrowOut || 0).toFixed(2)}</td>
      <td data-label="Auslagen Ein" class="num">${(e.expendituresIn || 0).toFixed(2)}</td>
      <td data-label="Auslagen Aus" class="num">${(e.expendituresOut || 0).toFixed(2)}</td>
      <td data-label="Beleg">${escapeHtml(e.invoice)}</td>
      <td data-label="Kontakt">${escapeHtml(e.contact)}</td>
    </tr>`).join('');

  return `
    <h3>Belege</h3>
    <div class="table-wrapper">
      <table class="data-table">
        <thead><tr><th>Beleg-Nr.</th><th>Name</th><th>Erstellt</th><th>Fällig</th><th>Betrag</th><th>Status</th><th>Kontakt</th></tr></thead>
        <tbody>${invoices || '<tr><td colspan="7">Keine Belege vorhanden.</td></tr>'}</tbody>
      </table>
    </div>
    <h3 style="margin-top:24px">Aktenkonto</h3>
    <div class="table-wrapper">
      <table class="data-table">
        <thead><tr><th>Datum</th><th>Buchungstext</th><th>Einnahmen</th><th>Ausgaben</th><th>Fremdgeld Ein</th><th>Fremdgeld Aus</th><th>Auslagen Ein</th><th>Auslagen Aus</th><th>Beleg</th><th>Kontakt</th></tr></thead>
        <tbody>${accountEntries || '<tr><td colspan="10">Keine Buchungen vorhanden.</td></tr>'}</tbody>
      </table>
    </div>`;
}

function renderDocuments(data, state) {
  const query = (state.q || '').trim().toLowerCase();
  const sortBy = state.sortBy || 'name';
  const sortDir = state.sortDir === 'desc' ? 'desc' : 'asc';
  const dir = sortDir === 'asc' ? 1 : -1;

  const filtered = (data.documents || []).filter(d => {
    const folderPath = (FOLDER_PATHS[d.folderId] || '');
    const name = d.name || '';
    const text = (name + ' ' + folderPath).toLowerCase();
    if (query && !fuzzy(text, query)) {
      return false;
    }
    const dot = name.lastIndexOf('.');
    const ext = dot > 0 ? name.substring(dot + 1).toLowerCase() : '';
    for (const group in FILE_TYPE_GROUPS) {
      if (FILE_TYPE_GROUPS[group].indexOf(ext) > -1) {
        if (state.hidden && state.hidden[group]) {
          return false;
        }
        break;
      }
    }
    return true;
  });

  function compareDocs(a, b) {
    let result = 0;
    if (sortBy === 'date') {
      result = (a.creationTs || 0) - (b.creationTs || 0);
    } else if (sortBy === 'size') {
      result = (a.size || 0) - (b.size || 0);
    } else {
      result = (a.name || '').localeCompare(b.name || '', 'de', { sensitivity: 'base' });
    }
    return result * dir;
  }

  function renderFolder(node) {
    if (!node) {
      return '';
    }
    const docs = filtered.filter(d => d.folderId === node.id).sort(compareDocs).map(d => {
      const href = (FILE_LINKS[d.id] || '');
      return `<div class="doc"><div class="name"><a href="${href}" target="_blank" rel="noopener">${escapeHtml(d.name)}</a></div><div class="meta">${escapeHtml(d.creationDate || '')} · ${escapeHtml(d.sizeHuman || '')}</div></div>`;
    }).join('');
    const children = (node.children || []).slice().sort((a, b) => (a.name || '').localeCompare(b.name || '', 'de', { sensitivity: 'base' })).map(c => {
      const content = renderFolder(c);
      return content ? `<li><details open><summary>${escapeHtml(c.name || '(Ordner)')}</summary>${content}</details></li>` : '';
    }).join('');
    return (docs || children) ? `<div class="tree">${docs ? `<div>${docs}</div>` : ''}${children ? `<ul>${children}</ul>` : ''}</div>` : '';
  }

  return `<div class="toolbar"><div class="grow"><input id="docSearch" type="search" placeholder="Dokumente suchen…" value="${escapeHtml(state.q || '')}" /></div><div class="controls"><label>Sortieren:</label><select id="docSortBy"><option value="name" ${sortBy === 'name' ? 'selected' : ''}>Name</option><option value="date" ${sortBy === 'date' ? 'selected' : ''}>Datum</option><option value="size" ${sortBy === 'size' ? 'selected' : ''}>Größe</option></select><button id="docSortDir" title="Richtung">${sortDir === 'asc' ? '▲' : '▼'}</button><button id="docExpandAll" class="icon-btn" title="Alle Ordner öffnen" aria-label="Alle Ordner öffnen"><svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M12 4a8 8 0 110 16 8 8 0 010-16zm1 7h4v2h-4v4h-2v-4H7v-2h4V7h2z"/></svg></button><button id="docCollapseAll" class="icon-btn" title="Alle Ordner schließen" aria-label="Alle Ordner schließen"><svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M12 4a8 8 0 110 16 8 8 0 010-16zm4 7H8v2h8v-2z"/></svg></button></div><div class="controls" id="docFilterBar"><label>Ausblenden:</label><button data-ftype="medien" class="toggle">Medien</button><button data-ftype="bilder" class="toggle">Bilder</button><button data-ftype="docs" class="toggle">Dokumente</button><button data-ftype="pdf" class="toggle">PDF</button><button data-ftype="html" class="toggle">HTML</button><button data-ftype="eml" class="toggle">EML</button><button data-ftype="bea" class="toggle">BEA</button><button data-ftype="md" class="toggle">MD</button></div></div>` + renderFolder(data.folders);
}

function bindDocControls(data) {
  const qInput = document.getElementById('docSearch');
  const sortSelect = document.getElementById('docSortBy');
  const dirBtn = document.getElementById('docSortDir');
  const expandBtn = document.getElementById('docExpandAll');
  const collapseBtn = document.getElementById('docCollapseAll');
  const filterBar = document.getElementById('docFilterBar');

  if (qInput) {
    qInput.addEventListener('input', () => {
      DOC_STATE.q = qInput.value || '';
      renderDocumentsSection(data);
      const newInput = document.getElementById('docSearch');
      if (newInput) {
        try {
          newInput.focus();
          const length = newInput.value.length;
          newInput.setSelectionRange(length, length);
        } catch (e) {
          // ignore selection issues
        }
      }
    });
  }

  if (sortSelect) {
    sortSelect.addEventListener('change', () => {
      DOC_STATE.sortBy = sortSelect.value;
      renderDocumentsSection(data);
    });
  }

  if (dirBtn) {
    dirBtn.addEventListener('click', () => {
      DOC_STATE.sortDir = DOC_STATE.sortDir === 'asc' ? 'desc' : 'asc';
      renderDocumentsSection(data);
    });
  }

  if (expandBtn) {
    expandBtn.addEventListener('click', () => {
      document.querySelectorAll('#documents details').forEach(d => { d.open = true; });
      if (qInput) {
        qInput.focus();
      }
    });
  }

  if (collapseBtn) {
    collapseBtn.addEventListener('click', () => {
      document.querySelectorAll('#documents details').forEach(d => { d.open = false; });
      if (qInput) {
        qInput.focus();
      }
    });
  }

  if (filterBar) {
    filterBar.querySelectorAll('button[data-ftype]').forEach(btn => {
      const key = btn.getAttribute('data-ftype');
      if (DOC_STATE.hidden && DOC_STATE.hidden[key]) {
        btn.classList.add('active');
      }
      btn.addEventListener('click', () => {
        DOC_STATE.hidden[key] = !DOC_STATE.hidden[key];
        btn.classList.toggle('active', !!DOC_STATE.hidden[key]);
        renderDocumentsSection(data);
      });
    });
  }
}

function renderDeadlines(data) {
  const items = (data.reviews || []).slice().sort((a, b) => (b.beginTs || 0) - (a.beginTs || 0));
  if (items.length === 0) {
    return '<div class="when">Keine Fälligkeiten vorhanden.</div>';
  }
  return items.map(r => {
    const cls = cssForType(r.type);
    const stateLabel = r.done ? 'erledigt' : 'offen';
    const when = `${escapeHtml(r.beginDate || '')}${r.endDate ? ' – ' + escapeHtml(r.endDate) : ''}${r.assignee ? ' · ' + escapeHtml(r.assignee) : ''}`;
    return `<div class="review ${cls} ${r.done ? 'done' : 'open'}"><div class="title"><span class="chip ${cls}">${escapeHtml(r.typeName)}</span> ${escapeHtml(r.summary || '')} <span class="state">${stateLabel}</span></div><div class="when">${when}</div></div>`;
  }).join('');
}

function cssForType(t) {
  switch (t) {
    case ${EVENTTYPE_FOLLOWUP}: return 'followup';
    case ${EVENTTYPE_RESPITE}: return 'respite';
    case ${EVENTTYPE_EVENT}: return 'event';
    default: return 'followup';
  }
}

function fuzzy(text, query) {
  if (!query) {
    return true;
  }
  if (text.indexOf(query) >= 0) {
    return true;
  }
  let ti = 0;
  let qi = 0;
  while (ti < text.length && qi < query.length) {
    if (text.charCodeAt(ti) === query.charCodeAt(qi)) {
      qi++;
    }
    ti++;
  }
  return qi === query.length;
}

function escapeHtml(str) {
  if (str == null) {
    return '';
  }
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
