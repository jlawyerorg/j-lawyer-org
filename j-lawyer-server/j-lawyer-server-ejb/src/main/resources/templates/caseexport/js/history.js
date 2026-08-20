const THEME_STORAGE_KEY = 'jlw-export-theme';

function readTheme() {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return stored === 'light' ? 'light' : 'dark';
  } catch (e) {
    return 'dark';
  }
}

function applyHistoryTheme(theme) {
  const normalized = theme === 'light' ? 'light' : 'dark';
  document.body.classList.remove('theme-dark', 'theme-light');
  document.body.classList.add(`theme-${normalized}`);
  document.body.dataset.theme = normalized;
}

document.addEventListener('DOMContentLoaded', () => {
  applyHistoryTheme(readTheme());
});

window.addEventListener('storage', evt => {
  if (evt.key === THEME_STORAGE_KEY && evt.newValue) {
    applyHistoryTheme(evt.newValue);
  }
});

window.addEventListener('message', evt => {
  if (evt.data && evt.data.type === 'JLW_THEME') {
    applyHistoryTheme(evt.data.theme);
  }
});
