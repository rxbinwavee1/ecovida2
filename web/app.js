// Econex Planer WebApp (PWA) Logic
const STORAGE_KEY = 'econex_planer_einsatze_v1';
const THEME_KEY = 'econex_planer_theme';
const AUTODELETE_KEY = 'econex_planer_autodelete';
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

let einsatze = [];
let currentTab = 'AKTIV'; // 'AKTIV' or 'ARCHIV'
let currentStatusFilter = null;
let searchQuery = '';
let isDarkTheme = true;
let isAutoDeleteEnabled = true;

// DOM Elements
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  loadData();
  registerServiceWorker();
  checkNotificationPermission();
  renderApp();

  setTimeout(() => {
    const splash = document.getElementById('splash-overlay');
    if (splash) {
      splash.classList.add('fly-out');
      setTimeout(() => { splash.style.display = 'none'; }, 400);
    }
  }, 1600);

  // Periodically check shift reminders
  checkShiftReminders();
  setInterval(checkShiftReminders, 60000);
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

// Auto Delete Toggle
function toggleAutoDelete() {
  isAutoDeleteEnabled = !isAutoDeleteEnabled;
  localStorage.setItem(AUTODELETE_KEY, isAutoDeleteEnabled ? 'true' : 'false');
  updateAutoDeleteBtn();
  if (isAutoDeleteEnabled) {
    saveData();
    alert('30-Tage Auto-Löschung aktiviert ⏱️');
  } else {
    alert('30-Tage Auto-Löschung deaktiviert ⏸️');
  }
}

function updateAutoDeleteBtn() {
  const btn = document.getElementById('autodelete-btn');
  if (btn) {
    btn.style.opacity = isAutoDeleteEnabled ? '1.0' : '0.35';
    btn.title = isAutoDeleteEnabled ? '30-Tage Auto-Löschung: AKTIV' : '30-Tage Auto-Löschung: DEAKTIVIERT';
  }
}

// Data Persistence & 30-Day Auto Cleanup
function loadData() {
  try {
    const savedAutoDelete = localStorage.getItem(AUTODELETE_KEY);
    isAutoDeleteEnabled = savedAutoDelete !== 'false';
    updateAutoDeleteBtn();

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
  checkShiftReminders();
}

function cleanup30DaysOld() {
  if (!isAutoDeleteEnabled) return;
  const now = Date.now();
  einsatze = einsatze.filter(item => {
    const effective = item.datumMillis || item.createdAt || now;
    return (now - effective) <= THIRTY_DAYS_MS;
  });
}

// Date Parsing Helper
function parseGermanDateToMillis(dateStr) {
  if (!dateStr) return Date.now();
  const match = dateStr.match(/\b(\d{1,2})\.(\d{1,2})\.(\d{4})\b/);
  if (match) {
    const day = parseInt(match[1], 10);
    const month = parseInt(match[2], 10) - 1;
    const year = parseInt(match[3], 10);
    return new Date(year, month, day).getTime();
  }
  return Date.now();
}

// Helper for BS Label
function getBsLabel(bs) {
  if (!bs) return '';
  const clean = String(bs).trim();
  if (clean === '1') return 'Zählung';
  if (clean === '3') return 'Leerfahrt';
  return clean;
}

// Render Main App UI
function renderApp() {
  renderStats();
  renderList();
}

function renderStats() {
  const activeItems = einsatze.filter(i => i.status !== 'ABGESCHLOSSEN');
  const archiveItems = einsatze.filter(i => i.status === 'ABGESCHLOSSEN');

  const isArchiv = (currentTab === 'ARCHIV');
  const labelText = isArchiv ? 'Bereits verdient' : 'Gesamthonorar';
  const countLabelText = isArchiv ? 'Archivierte Einsätze' : 'Aktive Einsätze';

  const totalSum = isArchiv
    ? archiveItems.reduce((sum, item) => sum + (parseFloat(item.betragEuro) || 0), 0)
    : einsatze.filter(i => i.status !== 'ABGESCHLOSSEN' && i.status !== 'ABGEBROCHEN').reduce((sum, item) => sum + (parseFloat(item.betragEuro) || 0), 0);

  const statsLabels = document.querySelectorAll('.stats-card .stat-info .label');
  if (statsLabels.length >= 2) {
    statsLabels[0].textContent = labelText;
    statsLabels[1].textContent = countLabelText;
  }

  document.getElementById('total-earnings').textContent = totalSum.toFixed(2).replace('.', ',') + ' €';
  document.getElementById('active-count').textContent = isArchiv ? archiveItems.length : activeItems.length;

  document.getElementById('tab-aktiv-label').textContent = `Aktive Einsätze (${activeItems.length})`;
  document.getElementById('tab-archiv-label').textContent = `Archiv (${archiveItems.length})`;
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
      (i.linie && i.linie.toLowerCase().includes(q)) ||
      (i.bs && i.bs.toLowerCase().includes(q)) ||
      (getBsLabel(i.bs).toLowerCase().includes(q))
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
        <p>Lade eine CSV-Datei hoch oder trage einen Einsatz manuell ein!</p>
      </div>
    `;
    return;
  }

  items.forEach(item => {
    const card = document.createElement('div');
    card.className = `shift-card ${item.status === 'ABGESCHLOSSEN' ? 'completed' : ''}`;

    const isDone = item.status === 'ABGESCHLOSSEN';
    const bsText = getBsLabel(item.bs);

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
        ${bsText ? `<span class="pill" style="color: ${bsText === 'Zählung' ? 'var(--emerald)' : 'var(--amber)'}; font-weight: bold;">${bsText}</span>` : ''}
        ${item.betragEuro ? `<span class="pill pill-honorar">Honorar: ${parseFloat(item.betragEuro).toFixed(2).replace('.', ',')} €</span>` : ''}
        <button class="pill" onclick="openRescheduleModal('${item.id}')">📅 Verschieben</button>
        <button class="pill" onclick="rescheduleOutage('${item.id}')" style="color: var(--amber);">⚠️ Ausgefallen (Nacherhebung)</button>
        <button class="pill" onclick="cancelShift('${item.id}')" style="color: var(--error);">🚫 Abbrechen</button>
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
    case 'AUSGEFALLEN': return 'Ausgefallen';
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
  const statuses = ['OFFEN', 'ABGESCHLOSSEN', 'VERSCHOBEN', 'ABGEBROCHEN', 'AUSGEFALLEN'];
  const nextIdx = (statuses.indexOf(item.status) + 1) % statuses.length;
  item.status = statuses[nextIdx];
  saveData();
}

function cancelShift(id) {
  const item = einsatze.find(i => i.id === id);
  if (item) {
    item.status = 'ABGEBROCHEN';
    saveData();
  }
}

function rescheduleOutage(id) {
  rescheduleTargetId = id;
  const item = einsatze.find(i => i.id === id);
  if (item) {
    item.status = 'AUSGEFALLEN';
    document.getElementById('reschedule-modal').style.display = 'flex';
  }
}

function saveManualEntry() {
  const datumStr = document.getElementById('m-datum').value.trim();
  const abZeit = document.getElementById('m-abzeit').value.trim();
  const anZeit = document.getElementById('m-anzeit').value.trim();
  const abBahnhof = document.getElementById('m-abbhf').value.trim();
  const anBahnhof = document.getElementById('m-anbhf').value.trim();
  const tourNr = document.getElementById('m-tour').value.trim();
  const linie = document.getElementById('m-linie').value.trim();
  const honorarVal = document.getElementById('m-honorar').value.trim();

  if (!datumStr && !abBahnhof) {
    alert('Bitte mindestens Datum oder Bahnhof eingeben.');
    return;
  }

  const euroClean = parseFloat(honorarVal.replace(/[^\d,.+]/g, '').replace(',', '.')) || 0;
  const datumMillis = parseGermanDateToMillis(datumStr);

  const newItem = {
    id: 'man_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
    datumStr: datumStr,
    datumMillis: datumMillis,
    originalDatumStr: datumStr,
    originalDatumMillis: datumMillis,
    bs: '1',
    abZeit: abZeit,
    abBahnhof: abBahnhof,
    anZeit: anZeit,
    anBahnhof: anBahnhof,
    tourNr: tourNr,
    linie: linie,
    betragEuro: euroClean,
    status: 'OFFEN',
    createdAt: Date.now()
  };

  einsatze.push(newItem);
  saveData();

  // Reset fields
  document.getElementById('m-datum').value = '';
  document.getElementById('m-abzeit').value = '';
  document.getElementById('m-anzeit').value = '';
  document.getElementById('m-abbhf').value = '';
  document.getElementById('m-anbhf').value = '';
  document.getElementById('m-tour').value = '';
  document.getElementById('m-linie').value = '';
  document.getElementById('m-honorar').value = '';

  closeModal('manual-modal');
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
  const idxBs = headers.findIndex(h => h === 'bs' || h.includes('bs'));
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
    const bsVal = idxBs !== -1 ? tokens[idxBs] : tokens[1] || '';
    const abZeit = idxAbZeit !== -1 ? tokens[idxAbZeit] : tokens[2] || '';
    const abBahnhof = idxAbBhf !== -1 ? tokens[idxAbBhf] : tokens[3] || '';
    const anZeit = idxAnZeit !== -1 ? tokens[idxAnZeit] : tokens[4] || '';
    const anBahnhof = idxAnBhf !== -1 ? tokens[idxAnBhf] : tokens[5] || '';
    const tourNr = idxTour !== -1 ? tokens[idxTour] : tokens[6] || '';
    const linie = idxLinie !== -1 ? tokens[idxLinie] : tokens[8] || '';
    const euroVal = idxEuro !== -1 ? tokens[idxEuro] : tokens[13] || '0';

    const euroClean = parseFloat(euroVal.replace(/[^\d,.+]/g, '').replace(',', '.')) || 0;
    const datumMillis = parseGermanDateToMillis(datumStr);

    parsed.push({
      id: 'csv_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
      datumStr: datumStr,
      datumMillis: datumMillis,
      originalDatumStr: datumStr,
      originalDatumMillis: datumMillis,
      bs: bsVal,
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
    document.getElementById('reschedule-modal').style.display = 'flex';
  }
}

function saveReschedule() {
  const rawDate = document.getElementById('reschedule-date-input').value; // YYYY-MM-DD
  if (rawDate && rescheduleTargetId) {
    const parts = rawDate.split('-');
    if (parts.length === 3) {
      const year = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1;
      const day = parseInt(parts[2], 10);
      const d = new Date(year, month, day);

      const days = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];
      const dayName = days[d.getDay()];
      const dayStr = String(d.getDate()).padStart(2, '0');
      const monthStr = String(d.getMonth() + 1).padStart(2, '0');
      const yearStr = d.getFullYear();
      const formattedGermanDate = `${dayName}, ${dayStr}.${monthStr}.${yearStr}`;

      const item = einsatze.find(i => i.id === rescheduleTargetId);
      if (item) {
        const origStr = item.originalDatumStr || item.datumStr;
        const origMillis = item.originalDatumMillis || item.datumMillis;

        item.originalDatumStr = origStr;
        item.originalDatumMillis = origMillis;

        const isBackToOriginal = (formattedGermanDate === origStr || d.getTime() === origMillis);
        item.datumStr = formattedGermanDate;
        item.datumMillis = d.getTime();

        if (item.status !== 'AUSGEFALLEN') {
          item.status = isBackToOriginal ? 'OFFEN' : 'VERSCHOBEN';
        }
        saveData();
      }
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
  if (event && event.target) {
    event.target.classList.add('active');
  }
  renderList();
}

function onSearchInput(val) {
  searchQuery = val;
  renderList();
}

function checkNotificationPermission() {
  const btn = document.getElementById('notif-btn');
  if (btn && 'Notification' in window && Notification.permission === 'granted') {
    btn.style.display = 'none';
  }
}

function requestNotificationPermission() {
  if ('Notification' in window) {
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        checkNotificationPermission();
        sendIosTestNotification();
        alert('Erinnerungen (Vortag 18:00 Uhr & 3 Std. vor Abfahrt) für iPhone / Safari aktiviert!');
      } else {
        alert('Benachrichtigungen wurden im Browser abgelehnt.');
      }
    });
  } else {
    alert('Benachrichtigungen werden von diesem Browser nicht unterstützt. Bitte füge die App zuerst zum Home-Bildschirm hinzu.');
  }
}

function sendIosTestNotification() {
  triggerNotification("Econex Planer aktiviert! 🔔", "Erinnerungen für deine Einsätze sind auf dem iPhone aktiv.");
}

function checkShiftReminders() {
  if (!('Notification' in window) || Notification.permission !== 'granted') return;

  const now = new Date();
  const nowMs = now.getTime();

  einsatze.forEach(item => {
    if (item.status === 'ABGESCHLOSSEN' || item.status === 'ABGEBROCHEN') return;
    if (!item.datumMillis) return;

    // 1: Day before shift at 18:00
    const dayBefore = new Date(item.datumMillis);
    dayBefore.setDate(dayBefore.getDate() - 1);
    dayBefore.setHours(18, 0, 0, 0);
    const dayBeforeMs = dayBefore.getTime();

    // 2: 3 hours before departure
    let threeHoursBeforeMs = 0;
    if (item.abZeit) {
      const parts = item.abZeit.split(':');
      if (parts.length >= 2) {
        const h = parseInt(parts[0], 10);
        const m = parseInt(parts[1], 10);
        const shiftTime = new Date(item.datumMillis);
        shiftTime.setHours(h, m, 0, 0);
        threeHoursBeforeMs = shiftTime.getTime() - (3 * 60 * 60 * 1000);
      }
    }

    const key1 = `notif_vortag_${item.id}`;
    const key2 = `notif_3h_${item.id}`;

    if (nowMs >= dayBeforeMs && nowMs < (dayBeforeMs + 30 * 60 * 1000) && !localStorage.getItem(key1)) {
      localStorage.setItem(key1, 'sent');
      triggerNotification("Erinnerung: Einsatz morgen!", `Morgen (${item.datumStr}) ab ${item.abZeit || '--:--'} Uhr: ${item.abBahnhof || ''} ➔ ${item.anBahnhof || ''}`);
    }

    if (threeHoursBeforeMs > 0 && nowMs >= threeHoursBeforeMs && nowMs < (threeHoursBeforeMs + 30 * 60 * 1000) && !localStorage.getItem(key2)) {
      localStorage.setItem(key2, 'sent');
      triggerNotification("⚡ In 3 Stunden: Einsatz-Abfahrt!", `Abfahrt in 3 Std. um ${item.abZeit || '--:--'} Uhr: ${item.abBahnhof || ''} ➔ ${item.anBahnhof || ''}`);
    }
  });
}

function triggerNotification(title, body) {
  const options = {
    body: body,
    icon: "apple-touch-icon.png",
    badge: "icon-192.png"
  };

  if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
    navigator.serviceWorker.ready.then(reg => {
      reg.showNotification(title, options);
    });
  } else if ('Notification' in window) {
    new Notification(title, options);
  }
}

function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js?v=6').then(reg => {
      reg.update();
    }).catch(e => console.log(e));
  }
}
