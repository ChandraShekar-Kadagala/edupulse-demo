# EduPulse Record Importer (Chrome extension)

Exports a student's **own** SRM AP academic records into EduPulse, the safe way.

## What it does / does not do
- Runs **only** in the tab you open, and only on `student.srmap.edu.in`.
- Reads the report that is **already on your screen** (including a same-origin frame it loads into), the
  same data you are looking at. Removes name, DOB, gender and photo before anything leaves your device.
- It does **not** know or store your portal password, does **not** log in for you, and does **not** solve
  the CAPTCHA. You log in yourself; the extension only reads the page you are viewing.

This is why it works when "Save page as HTML" gives an empty file: the report is often loaded in a frame,
so the saved file is just the shell — but the extension reads the live page.

## Install (developer / unpacked)
1. Chrome → `chrome://extensions` → turn on **Developer mode**.
2. **Load unpacked** → select this `extension/` folder.
3. Pin the "EduPulse Record Importer" extension.

## Use
1. Log into the SRM portal yourself and open your **attendance**, **internal marks**, or **exam-ledger** report.
2. Click the extension → **Extract my records** → **Download clean file (.json)**.
3. In EduPulse → **Import my records** → load that `.json` → **Validate** → **Consent & save to my profile**.

## Notes
- `extractor.js` holds the extraction logic and is shared with EduPulse's web parser
  (`frontend/src/features/import/parseReport.ts`); keep the two in sync when portal layouts change.
- No icons are bundled; Chrome shows a default icon. Add `icons` to `manifest.json` if you want branding.
- Only academic data is ever read; see EduPulse `docs/account-linking-architecture.md` and
  `docs/srmap-security-review.md` for the full data/consent model.
