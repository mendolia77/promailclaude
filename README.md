# SmartMail - Email Client Android

Un client email Android moderno e completo, sviluppato con Jetpack Compose e Kotlin.

## 🚀 Caratteristiche

### Email Management
- ✉️ **Multi-account**: Supporto per account IMAP/SMTP multipli (Gmail, Outlook, Yahoo, Custom)
- 📥 **Sincronizzazione automatica**: Sincronizzazione in background con WorkManager
- 🔍 **Ricerca avanzata**: Ricerca full-text nelle email
- ⭐ **Organizzazione**: Segna come importante, aggiungi a stellati
- 📎 **Allegati**: Visualizza e scarica allegati

### Smart Features
- 📁 **Cartelle Smart**: Organizzazione automatica delle email con filtri personalizzabili
- 🎯 **Filtri email**: Crea regole automatiche per organizzare le email
- ✍️ **Bozze**: Auto-save ogni 30 secondi
- ↩️ **Rispondi e Inoltra**: Con citazione del messaggio originale
- 🔔 **Notifiche**: Notifiche push con suono e vibrazione per nuove email

### Backup & Cloud
- 💾 **Backup Locale**: Crea backup del database in locale
- ☁️ **Google Drive**: Sincronizza i backup su Google Drive
- 🔄 **Ripristino**: Ripristina facilmente da backup locale o cloud

### Personalizzazione
- 🎨 **Temi**: Modalità Chiara, Scura o Segui Sistema
- 🔧 **Intervallo sincronizzazione**: Personalizza la frequenza di sincronizzazione
- 🎨 **Colori account**: Assegna colori personalizzati a ogni account

## 🛠️ Tecnologie Utilizzate

### Framework & Libraries
- **Kotlin** - Linguaggio principale
- **Jetpack Compose** - UI moderna e dichiarativa
- **Material Design 3** - Design system
- **Coroutines & Flow** - Programmazione asincrona

### Architecture
- **MVVM** - Architecture pattern
- **Room Database** - Database locale con migrazioni
- **DataStore** - Gestione preferenze
- **WorkManager** - Sincronizzazione in background

### Network & Email
- **JavaMail API** - Protocolli IMAP/SMTP
- **Google Drive API** - Backup cloud
- **Google Sign-In** - Autenticazione Google

## 📋 Requisiti

- Android SDK 26+ (Android 8.0 Oreo)
- Target SDK 34 (Android 14)
- Kotlin 1.9+
- Gradle 8.14+

## 🔧 Setup Progetto

1. Clone del repository
2. Apri con Android Studio Hedgehog o superiore
3. Sincronizza Gradle
4. Build & Run

## 📱 Funzionalità Principali

### Gestione Account
- Aggiungi account email con wizard guidato
- Preset per Gmail, Outlook, Yahoo
- Configurazione IMAP/SMTP personalizzata
- Test connessione prima del salvataggio

### Cartelle Smart
- Crea cartelle con regole automatiche
- Filtri per mittente, oggetto, contenuto
- Organizzazione automatica delle email

### Backup System
1. **Locale**: Backup salvati in Downloads/SmartMail/Backups/
2. **Google Drive**: Upload/download automatico da Drive
3. **Ripristino**: Un click per ripristinare tutto

## 👨‍💻 Autore

Sviluppato da G. Mendolia

## 📄 Licenza

Questo progetto è sotto licenza MIT
