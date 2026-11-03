// Econex Planer WebApp (PWA) Logic
const STORAGE_KEY = 'econex_planer_einsatze_v1';
const THEME_KEY = 'econex_planer_theme';
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

let einsatze = [];
let currentTab = 'AKTIV'; // 'AKTIV' or 'ARCHIV'
let currentStatusFilter = null;
let searchQuery = '';
let isDarkTheme = true;

// DOM Elements
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  loadData();
  registerServiceWorker();
  renderApp();
});

// Theme Management
function initTheme() {
  const savedTheme = localStorage.getItem(THEME_KEY);
  isDarkTheme = savedTheme !== 'light';
  applyTheme();
}

function toggleTheme() {
  isDarkTheme = !isDarkTheme;
  localStorage.setItem(THEME_KEY, isDarkTheme ? 'dark' : 'light');
  applyTheme();
}

function applyTheme() {
  if (isDarkTheme) {
    document.body.classList.remove('light-theme');
    document.getElementById('theme-icon').textContent = '☀️';
  } else {
    document.body.classList.add('light-theme');
    document.getElementById('theme-icon').textContent = '🌙';
  }
}

// Data Persistence & 30-Day Auto Cleanup
function loadData() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      einsatze = JSON.parse(raw);
    }
  } catch (e) {
    einsatze = [];
  }
  cleanup30DaysOld();
}

function saveData() {
  cleanup30DaysOld();
  localStorage.setItem(STORAGE_KEY, JSON.stringify(einsatze));
  renderApp();
}

function cleanup30DaysOld() {
  const now = Date.now();
  const initialCount = einsatze.length;
  einsatze = einsatze.filter(item => {
    const effective = item.datumMillis || item.createdAt || now;
    return (now - effective) <= THIRTY_DAYS_MS;
  });
}

// Date Parsing Helper
function parseGermanDateToMillis(dateStr) {
  if (!dateStr) return Date.now();
  const clean = dateStr.replace(/^[A-Za-z]{2},\s*/, '').trim();
  const parts = clean.split('.');
  if (parts.length === 3) {
    const day = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    const year = parseInt(parts[2], 10);
    return new Date(year, month, day).getTime();
  }
  return Date.now();
}

// Render Main App UI
function renderApp() {
  renderStats();
  renderList();
}

function renderStats() {
  const activeItems = einsatze.filter(i => i.status !== 'ABGESCHLOSSEN');
  const totalSum = einsatze
    .filter(i => i.status === 'OFFEN' || i.status === 'ABGESCHLOSSEN')
    .reduce((sum, item) => sum + (parseFloat(item.betragEuro) || 0), 0);

  document.getElementById('total-earnings').textContent = totalSum.toFixed(2).replace('.', ',') + ' €';
  document.getElementById('active-count').textContent = activeItems.length;

  document.getElementById('tab-aktiv-label').textContent = `Aktive Einsätze (${activeItems.length})`;
  document.getElementById('tab-archiv-label').textContent = `Archiv (${einsatze.length - activeItems.length})`;
}

function getFilteredList() {
  let list = einsatze;

  // Filter by Tab
  if (currentTab === 'AKTIV') {
    list = list.filter(i => i.status !== 'ABGESCHLOSSEN');
  } else {
    list = list.filter(i => i.status === 'ABGESCHLOSSEN');
  }

  // Filter by Status Chip
  if (currentStatusFilter) {
    list = list.filter(i => i.status === currentStatusFilter);
  }

  // Filter by Search Query
  if (searchQuery.trim() !== '') {
    const q = searchQuery.toLowerCase().trim();
    list = list.filter(i =>
      (i.datumStr && i.datumStr.toLowerCase().includes(q)) ||
      (i.abBahnhof && i.abBahnhof.toLowerCase().includes(q)) ||
      (i.anBahnhof && i.anBahnhof.toLowerCase().includes(q)) ||
      (i.tourNr && i.tourNr.toLowerCase().includes(q)) ||
      (i.linie && i.linie.toLowerCase().includes(q))
    );
  }

  // Sort Chronologically (Earliest First)
  list.sort((a, b) => {
    const timeA = a.datumMillis || 0;
    const timeB = b.datumMillis || 0;
    if (timeA !== timeB) return timeA - timeB;
    return (a.abZeit || '').localeCompare(b.abZeit || '');
  });

  return list;
}

function renderList() {
  const listContainer = document.getElementById('shifts-list');
  listContainer.innerHTML = '';

  const items = getFilteredList();

  if (items.length === 0) {
    listContainer.innerHTML = `
      <div class="empty-state">
        <p><strong>Keine Einsätze gefunden</strong></p>
        <p>Füge einen Screenshot oder eine CSV-Datei ein!</p>
      </div>
    `;
    return;
  }

  items.forEach(item => {
    const card = document.createElement('div');
    card.className = `shift-card ${item.status === 'ABGESCHLOSSEN' ? 'completed' : ''}`;

    const isDone = item.status === 'ABGESCHLOSSEN';

    card.innerHTML = `
      <div class="shift-card-header">
        <div style="display: flex; align-items: center; gap: 8px;">
          <input type="checkbox" ${isDone ? 'checked' : ''} onchange="toggleStatus('${item.id}')" style="width: 20px; height: 20px; cursor: pointer;">
          <div class="date-badge">📅 ${item.datumStr || 'Kein Datum'}</div>
          <span class="status-chip status-${item.status}" onclick="cycleStatus('${item.id}')">${getStatusLabel(item.status)}</span>
        </div>
        <button class="icon-btn danger" onclick="deleteItem('${item.id}')">🗑️</button>
      </div>
      <div class="route-box">
        <div class="station-col">
          <span class="label">Abfahrt</span>
          <span class="time">${item.abZeit || '--:--'}</span>
          <span class="name">${item.abBahnhof || 'Unbekannt'}</span>
        </div>
        <span>➔</span>
        <div class="station-col">
          <span class="label">Ankunft</span>
          <span class="time">${item.anZeit || '--:--'}</span>
          <span class="name">${item.anBahnhof || 'Unbekannt'}</span>
        </div>
      </div>
      <div class="pills-row">
        ${item.tourNr ? `<span class="pill">Tour-Nr: ${item.tourNr}</span>` : ''}
        ${item.linie ? `<span class="pill" style="color: var(--primary); font-weight: bold;">Linie: ${item.linie}</span>` : ''}
        ${item.betragEuro ? `<span class="pill pill-honorar">Honorar: ${parseFloat(item.betragEuro).toFixed(2).replace('.', ',')} €</span>` : ''}
        <button class="pill" onclick="openRescheduleModal('${item.id}')">📅 Verschieben</button>
      </div>
    `;

    listContainer.appendChild(card);
  });
}

function getStatusLabel(s) {
  switch (s) {
    case 'ABGESCHLOSSEN': return 'Abgeschlossen';
    case 'VERSCHOBEN': return 'Verschoben';
    case 'ABGEBROCHEN': return 'Abgebrochen';
    default: return 'Offen';
  }
}

function toggleStatus(id) {
  const item = einsatze.find(i => i.id === id);
  if (item) {
    item.status = item.status === 'ABGESCHLOSSEN' ? 'OFFEN' : 'ABGESCHLOSSEN';
    saveData();
  }
}

function cycleStatus(id) {
  const item = einsatze.find(i => i.id === id);
  if (!item) return;
  const statuses = ['OFFEN', 'ABGESCHLOSSEN', 'VERSCHOBEN', 'ABGEBROCHEN'];
  const nextIdx = (statuses.indexOf(item.status) + 1) % statuses.length;
  item.status = statuses[nextIdx];
  saveData();
}

function deleteItem(id) {
  if (confirm("Diesen Einsatz löschen?")) {
    einsatze = einsatze.filter(i => i.id !== id);
    saveData();
  }
}

function purgeAll() {
  document.getElementById('purge-modal').style.display = 'flex';
}

function confirmPurgeAll() {
  einsatze = [];
  saveData();
  closeModal('purge-modal');
}

// CSV Parser
function handleCsvUpload(file) {
  const reader = new FileReader();
  reader.onload = (e) => {
    parseAndSaveCsv(e.target.result);
  };
  reader.readAsText(file);
}

function parseAndSaveCsv(csvContent) {
  if (!csvContent) return;
  const lines = csvContent.split('\n').map(l => l.trim()).filter(l => l.length > 0);
  if (lines.length === 0) return;

  const delimiter = lines[0].includes(';') ? ';' : (lines[0].includes('\t') ? '\t' : ',');
  const headers = lines[0].split(delimiter).map(h => h.trim().toLowerCase());

  const idxDatum = headers.findIndex(h => h.includes('datum'));
  const idxAbZeit = headers.findIndex(h => h.includes('ab-zeit') || h.includes('abzeit'));
  const idxAbBhf = headers.findIndex(h => h.includes('ab-bhf') || h.includes('abbhf'));
  const idxAnZeit = headers.findIndex(h => h.includes('an-zeit') || h.includes('anzeit'));
  const idxAnBhf = headers.findIndex(h => h.includes('an-bhf') || h.includes('anbhf'));
  const idxTour = headers.findIndex(h => h.includes('tour'));
  const idxLinie = headers.findIndex(h => h.includes('linie'));
  const idxEuro = headers.findIndex(h => h.includes('euro') || h.includes('honorar'));

  const parsed = [];

  for (let i = 1; i < lines.length; i++) {
    const tokens = lines[i].split(delimiter).map(t => t.trim().replace(/^"|"$/g, ''));
    if (tokens.length < 2) continue;

    const datumStr = idxDatum !== -1 ? tokens[idxDatum] : tokens[0];
    const abZeit = idxAbZeit !== -1 ? tokens[idxAbZeit] : tokens[2] || '';
    const abBahnhof = idxAbBhf !== -1 ? tokens[idxAbBhf] : tokens[3] || '';
    const anZeit = idxAnZeit !== -1 ? tokens[idxAnZeit] : tokens[4] || '';
    const anBahnhof = idxAnBhf !== -1 ? tokens[idxAnBhf] : tokens[5] || '';
    const tourNr = idxTour !== -1 ? tokens[idxTour] : tokens[6] || '';
    const linie = idxLinie !== -1 ? tokens[idxLinie] : tokens[8] || '';
    const euroVal = idxEuro !== -1 ? tokens[idxEuro] : tokens[13] || '0';

    const euroClean = parseFloat(euroVal.replace(/[^\d,.+]/g, '').replace(',', '.')) || 0;

    parsed.push({
      id: 'csv_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
      datumStr: datumStr,
      datumMillis: parseGermanDateToMillis(datumStr),
      abZeit: abZeit,
      abBahnhof: abBahnhof,
      anZeit: anZeit,
      anBahnhof: anBahnhof,
      tourNr: tourNr,
      linie: linie,
      betragEuro: euroClean,
      status: 'OFFEN',
      createdAt: Date.now()
    });
  }

  if (parsed.length > 0) {
    einsatze.push(...parsed);
    saveData();
    alert(`${parsed.length} Einsätze erfolgreich importiert!`);
  }
}

// Reschedule Modal
let rescheduleTargetId = null;
function openRescheduleModal(id) {
  rescheduleTargetId = id;
  const item = einsatze.find(i => i.id === id);
  if (item) {
    document.getElementById('reschedule-date-input').value = item.datumStr;
    document.getElementById('reschedule-modal').style.display = 'flex';
  }
}

function saveReschedule() {
  const newDate = document.getElementById('reschedule-date-input').value;
  if (newDate && rescheduleTargetId) {
    const item = einsatze.find(i => i.id === rescheduleTargetId);
    if (item) {
      item.datumStr = newDate;
      item.datumMillis = parseGermanDateToMillis(newDate);
      item.status = 'VERSCHOBEN';
      saveData();
    }
  }
  closeModal('reschedule-modal');
}

function closeModal(id) {
  document.getElementById(id).style.display = 'none';
}

function setTab(tab) {
  currentTab = tab;
  document.getElementById('tab-aktiv').classList.toggle('active', tab === 'AKTIV');
  document.getElementById('tab-archiv').classList.toggle('active', tab === 'ARCHIV');
  renderApp();
}

function setStatusFilter(status) {
  currentStatusFilter = status;
  document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
  event.target.classList.add('active');
  renderList();
}

function onSearchInput(val) {
  searchQuery = val;
  renderList();
}

function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(e => console.log(e));
  }
}
