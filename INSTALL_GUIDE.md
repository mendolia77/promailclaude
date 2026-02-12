# 📱 Guida Installazione SmartMail sul Telefono

## 🎯 Metodo: Trasferimento APK Manuale

### Passo 1: Trova l'APK Compilato

Dopo la compilazione, l'APK si trova qui:
```
C:\Users\G_mendolia\Desktop\promail\ProMail\app\build\outputs\apk\debug\app-debug.apk
```

### Passo 2: Trasferisci sul Telefono

#### Opzione A: Tramite Cavo USB
1. Collega il telefono al PC con cavo USB
2. Sul telefono: tocca la notifica USB
3. Seleziona "Trasferimento file" / "MTP"
4. Sul PC: apri "Questo PC" → vedrai il tuo telefono
5. Copia `app-debug.apk` in una cartella del telefono (es. Download)

#### Opzione B: Tramite Google Drive / Cloud
1. Carica `app-debug.apk` su Google Drive / OneDrive
2. Sul telefono: apri Drive e scarica il file

#### Opzione C: Tramite Email
1. Invia l'APK a te stesso via email
2. Sul telefono: apri l'email e scarica l'allegato

#### Opzione D: Tramite ADB (se hai Android Studio)
```bash
adb install app-debug.apk
```

---

### Passo 3: Abilita Installazione da Fonti Sconosciute

Sul telefono:

**Android 12+ (più recente):**
1. Quando tenti di installare l'APK
2. Appare popup: "Installa app sconosciuta?"
3. Tocca "Impostazioni"
4. Attiva l'interruttore per la tua app di file manager
5. Torna indietro e riprova installazione

**Android 8-11:**
1. Impostazioni → Sicurezza
2. Trova "Installa app sconosciute"
3. Seleziona l'app che userai (File Manager, Chrome, Gmail, ecc.)
4. Attiva "Consenti da questa fonte"

**Android 7 e precedenti:**
1. Impostazioni → Sicurezza
2. Attiva "Sorgenti sconosciute"

---

### Passo 4: Installa l'APK

1. Sul telefono, apri File Manager
2. Vai in "Download" (o dove hai copiato l'APK)
3. Tocca `app-debug.apk`
4. Appare schermata: "Installare SmartMail?"
5. Tocca "Installa"
6. Attendi... (5-10 secondi)
7. Tocca "Apri" quando finisce

---

## 🎉 Primo Avvio SmartMail

### Cosa Vedrai:

#### 1. Splash Screen (3 secondi)
```
✨ Animazione spettacolare
📱 Logo SmartMail
🌈 Gradient viola-blu animato
💫 Particelle fluttuanti
```

#### 2. Onboarding (Swipe 4 pagine)
```
📄 Pagina 1: Organizzazione Automatica
📄 Pagina 2: Categorie Personalizzate
📄 Pagina 3: Focus sull'Importante
📄 Pagina 4: Design Spettacolare
```

#### 3. Inbox Principale
```
📊 Quick Stats in alto
📁 Categorie smart scrollabili
📧 Lista email (vuota la prima volta)
➕ FAB "Scrivi" in basso a destra
```

---

## ⚙️ Setup Primo Account Email

### Per Gmail:

1. Tocca ⚙️ **Impostazioni** (bottom bar)
2. Tocca **"Aggiungi Account"**
3. Seleziona **Gmail**
4. Inserisci email: `tuoemail@gmail.com`
5. **IMPORTANTE**: Usa una **"Password per le app"**, NON la password normale

#### Come Ottenere Password per le App (Gmail):
```
1. Vai su: https://myaccount.google.com/security
2. Attiva "Verifica in 2 passaggi" (se non attiva)
3. Cerca "Password per le app"
4. Seleziona app: "Posta"
5. Seleziona dispositivo: "Android"
6. Click "Genera"
7. Copia la password a 16 caratteri (es: abcd efgh ijkl mnop)
8. Inseriscila in SmartMail
```

6. Tocca **"Connetti"**
7. SmartMail scarica le email
8. Le categorie smart si popolano automaticamente! ✨

### Per Virgilio:

1. Aggiungi Account → **Virgilio**
2. Email: `tuoemail@virgilio.it`
3. Password: **password normale** (non serve app password)
4. Connetti

### Per Altri Provider IMAP:

1. Aggiungi Account → **Altro (IMAP)**
2. Inserisci:
   - Email
   - Password
   - Server IMAP (es: `imap.provider.com`)
   - Porta IMAP (solitamente `993`)
   - Server SMTP (es: `smtp.provider.com`)
   - Porta SMTP (solitamente `587` o `465`)

---

## 🎯 Testa le Funzionalità

### 1. Categorie Smart
```
✅ Le email si categorizzano automaticamente
✅ Tocca una categoria per vedere solo quelle email
✅ Badge con contatore email non lette
```

### 2. Swipe Gestures
```
👉 Swipe DESTRA → Archivia
👉 Swipe DESTRA lungo → Importante (stella)
👈 Swipe SINISTRA → Elimina
```

### 3. Dashboard Analytics
```
📊 Menu → Dashboard
✅ Vedi tempo risparmiato
✅ Grafico distribuzione categorie
✅ Insights produttività
```

### 4. Gestione Filtri
```
⚙️ Impostazioni → Gestione Filtri
📋 Tab "Suggerimenti IA" → Vedi suggerimenti intelligenti
📋 Tab "I Miei Filtri" → Crea filtri custom
```

### 5. Composizione Email
```
✏️ Tocca FAB "+" in basso a destra
✅ Scrivi email con editor
✅ Aggiungi allegati
✅ Invia
```

---

## 🐛 Risoluzione Problemi

### L'app non si installa
**Causa**: Fonti sconosciute non abilitate
**Soluzione**: Vedi Passo 3 sopra

### L'app crasha all'avvio
**Causa**: Incompatibilità Android
**Soluzione**: Verifica Android 8.0+ (API 26+)

### Non vedo le email
**Causa**: Account non configurato
**Soluzione**:
1. Vai in Impostazioni
2. Aggiungi Account
3. Inserisci credenziali corrette

### Gmail: Errore autenticazione
**Causa**: Usi password normale invece di "Password per le app"
**Soluzione**: Genera Password per le App (vedi sopra)

### Le categorie sono vuote
**Causa**: Nessuna email scaricata ancora
**Soluzione**:
1. Pulldown per refresh
2. Attendi sync (vedi spinner in alto)
3. Le email appaiono automaticamente categorizzate

### L'IA non suggerisce filtri
**Causa**: Serve usare l'app per qualche giorno
**Soluzione**:
1. Usa l'app normalmente
2. Sposta email manualmente nelle categorie
3. Dopo 3+ azioni simili, l'IA suggerisce filtri automatici

---

## 📊 Performance

### Dimensione APK
```
APK Debug: ~15-20 MB
(compresso con tutte le librerie)
```

### Requisiti
```
✅ Android 8.0+ (API 26+)
✅ 50 MB spazio disponibile
✅ Connessione internet (per sync email)
```

### Permessi Richiesti
```
📧 Internet (per IMAP/SMTP)
📁 Storage (per allegati)
🔔 Notifiche (opzionale)
```

---

## 🎨 Personalizzazione

### Cambia Tema
```
Impostazioni → Aspetto
- Light Mode
- Dark Mode (AMOLED black)
- Auto (segue sistema)
```

### Crea Categorie Custom
```
Impostazioni → Gestione Filtri → "+"
1. Nome categoria
2. Emoji icona
3. Colore
4. Regole filtro
5. Salva
```

---

## 🚀 Prossimi Passi

1. **Usa l'app per 2-3 giorni**
2. **Sposta email manualmente** nelle categorie
3. **L'IA impara** dai tuoi comportamenti
4. **Accetta i suggerimenti** quando appaiono
5. **Goditi l'automazione!** ✨

---

## 📞 Debug Mode

Se hai problemi, abilita debug:

```
Impostazioni → Info App
Tocca 7 volte su "Versione"
→ Appare "Debug Mode attivato"
→ Vedi log dettagliati
```

---

## 🎉 Pronto!

**SmartMail è installato e pronto all'uso!**

Domande? Controlla:
- `README.md` - Setup generale
- `FEATURES.md` - Tutte le funzionalità
- `ICON_README.md` - Info sull'icona

**Buon utilizzo! ⚡📧**

---

**Creato con ❤️**
*SmartMail - Email organizzate, tempo risparmiato*
