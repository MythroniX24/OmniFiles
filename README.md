# 📁 OmniFiles — Modern Android File Manager

A premium, production-ready Android file manager built with Kotlin and Jetpack Compose. Designed for speed, stability, and future AI integration.

## ✨ Features

| Feature | Description |
|---------|-------------|
| 📂 File Operations | Copy, move, rename, delete, share, duplicate, create |
| 🔍 Fast Search | Indexed search engine with name, extension, date, size filters |
| 📦 Archive Support | ZIP, 7Z, TAR, GZIP — compress and extract |
| 🎨 Material You | Dynamic colors, light/dark/system themes |
| 📱 Tablet Support | Responsive layouts for all screen sizes |
| 💾 Storage Support | Internal, SD Card, USB OTG |
| ⚡ Performance | Lazy loading, background scanning, minimal RAM usage |
| 🔒 Privacy | 100% offline, no ads, no tracking, no analytics |

## 🏗️ Architecture

```
Clean Architecture + MVVM + Hilt + Jetpack Compose

ui/              ← Compose UI + ViewModels
domain/          ← Use Cases + Repository interfaces + Models
data/            ← Repositories + Room DB + DataStore
core/            ← Theme, Common utilities
archive/         ← Archive engine (ZIP, 7Z, TAR, GZIP)
search/          ← Search engine (indexed)
utils/           ← File utilities, formatting
di/              ← Hilt modules
```

## 🚀 Quick Start

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK 35

### Build locally
```bash
git clone https://github.com/<your-username>/OmniFiles.git
cd OmniFiles
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

### Install on device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Clean Architecture |
| DI | Hilt |
| Database | Room |
| Storage | DataStore Preferences |
| Async | Coroutines, Flow |
| Images | Coil |
| Archives | Apache Commons Compress |
| Build | Gradle Kotlin DSL |

## 📱 Permissions

| Permission | Reason |
|-----------|--------|
| READ_EXTERNAL_STORAGE | Browse files on device |
| WRITE_EXTERNAL_STORAGE | File operations (Android 9 and below) |
| MANAGE_EXTERNAL_STORAGE | Full file access (Android 11+) |

## 🔮 Future Plans (Phase 2)

- AI-powered file organization
- Smart search with natural language
- Automatic file categorization
- Intelligent cleanup suggestions
- Duplicate file detection
- Image recognition and tagging

## 📄 License

MIT License — see [LICENSE](LICENSE) file

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.
