# SmartTraffic AI — Independent Admin Web Panel

This `admin/` folder is a **100% standalone and independent** web application. It can be separated from the Android app repository and hosted anywhere (Firebase Hosting, GitHub Pages, Vercel, Netlify, Apache, Nginx, or opened locally in any browser).

---

## 📁 Files in this Folder

- **`index.html`**: The complete Master Police Command & Control Hub. Contains:
  - Live IoT telemetry and junction congestion monitors.
  - Signal Grid Overrides (All-Red Lockdown, Emergency Green Corridor, 100 dB Siren).
  - Personnel & Vehicle Owner account manager (Role toggle, suspend/activate, add officer).
  - E-Challan & Citations registry (issue manual tickets, waive fines, print receipts).
  - Firebase Firestore & Realtime Cloud sync integration.
- **`login.html`**: Standalone Police Admin login portal with Firebase Auth integration and one-tap demo accounts.

---

## ⚡ How to Run or Separate

### 1. Run Locally (Direct in Browser)
Simply double-click `index.html` or `login.html` to open it in Chrome, Firefox, Safari, or Edge. No node_modules, no npm build, no web server required!

### 2. Deploy Independently on Firebase Hosting
If you wish to host this admin panel on your Firebase project (`trafficeye-99d78`):
```bash
# Inside this folder
npm install -g firebase-tools
firebase login
firebase init hosting
# Select project: trafficeye-99d78
# Set public directory: . (current directory)
firebase deploy --only hosting
```

### 3. Deploy to GitHub Pages or Netlify
Simply drag and drop this `admin` folder into [Netlify Drop](https://app.netlify.com/drop) or push to a GitHub repository and enable GitHub Pages.

---

## 🔒 Integrated Firebase Credentials

Pre-configured with your Firebase Project:
- **Project ID**: `trafficeye-99d78`
- **Realtime Database**: `https://trafficeye-99d78-default-rtdb.firebaseio.com`
- **Storage Bucket**: `trafficeye-99d78.firebasestorage.app`
- **Auth Domain**: `trafficeye-99d78.firebaseapp.com`
