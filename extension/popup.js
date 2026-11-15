import { extractAcademicExport } from './extractor.js';

const resultEl = document.getElementById('result');
const extractBtn = document.getElementById('extract');
const downloadBtn = document.getElementById('download');
let latest = null;

const SRM_HOST = 'student.srmap.edu.in';

function show(html, cls) {
  resultEl.className = cls || '';
  resultEl.innerHTML = html;
}

extractBtn.addEventListener('click', async () => {
  show('Reading this page…');
  downloadBtn.disabled = true;
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab || !tab.url || !tab.url.includes(SRM_HOST)) {
      show('Open your SRM portal report tab first, then click Extract.', 'warn');
      return;
    }
    const [res] = await chrome.scripting.executeScript({ target: { tabId: tab.id }, func: extractAcademicExport });
    const data = res && res.result;
    const total = data ? data.attendance.length + data.ledger.length : 0;
    if (!data || (total === 0 && !data.cgpa)) {
      show('No academic data found on this page. Open the actual attendance or marks report (not the dashboard) and try again.', 'warn');
      return;
    }
    latest = data;
    downloadBtn.disabled = false;
    show(`Found <b>${data.attendance.length}</b> attendance + <b>${data.ledger.length}</b> result rows${data.cgpa ? `, CGPA ${data.cgpa}` : ''}. Personal details removed. Download it, then upload the file in EduPulse → “Import my records”.`, 'ok');
  } catch (e) {
    show('Could not read this page. Make sure you are on the SRM portal report and try again.', 'err');
  }
});

downloadBtn.addEventListener('click', () => {
  if (!latest) return;
  const blob = new Blob([JSON.stringify(latest, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'edupulse-academic-export.json';
  a.click();
  URL.revokeObjectURL(url);
});
