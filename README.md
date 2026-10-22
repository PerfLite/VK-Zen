<div align="center">

<img src="assets/banner.svg" alt="VK Zen" width="100%">

<p>
  <a href="#english">🇬🇧 English</a> ·
  <a href="#русский">🇷🇺 Русский</a>
</p>

<p>
  <img src="https://img.shields.io/github/v/release/PerfLite/VK-Zen?style=flat-square" alt="release">
  <img src="https://img.shields.io/github/license/PerfLite/VK-Zen?style=flat-square" alt="license">
  <img src="https://img.shields.io/github/languages/top/PerfLite/VK-Zen?style=flat-square" alt="language">
  <img src="https://img.shields.io/github/stars/PerfLite/VK-Zen?style=flat-square" alt="stars">
  <img src="https://img.shields.io/github/downloads/PerfLite/VK-Zen/total?style=flat-square" alt="downloads">
</p>

<p>
  <a href="https://github.com/PerfLite/VK-Zen/releases/latest"><b>⬇️ Download / Скачать</b></a> ·
  <a href="https://github.com/PerfLite/VK-Zen/releases">📦 All releases / Все релизы</a>
</p>

</div>

## ⬇️ Download / Скачать

| File / Файл | Description / Описание | Link / Ссылка |
|---|---|---|
| `patches-1.0.0.mpp` | Morphe patch bundle · Бандл патчей Morphe | [Download](https://github.com/PerfLite/VK-Zen/releases/latest/download/patches-1.0.0.mpp) |

> Older versions and changelogs: [Releases](https://github.com/PerfLite/VK-Zen/releases) ·  
> Старые версии и список изменений: [Релизы](https://github.com/PerfLite/VK-Zen/releases)

---

## English

Morphe patches for VK (VKontakte) Android client — ad blocking, background music playback, cache download, video quality priority and interface customization.

### Features

- **VK Video:** Preferred playback quality selection (Auto, 1080p Full HD, 720p HD, 480p, 360p), in-stream ad blocking (preroll, midroll, postroll), clean player interface without install prompts.
- **Ad Blocking:** Clean feed without sponsored or promoted posts (`marked_as_ads`, `author_ad`), no ads in VK Clips, no audio ads between tracks in VK Music, hidden promotional banners in Services.
- **VK Music:** Background playback without restrictions or subscription, offline cache download enabled, hidden promo subscription banners.
- **Interface Customization:** Full bottom navigation bar customization (configure all 5 slots), dedicated VK Zen settings screen inside VK.
- **Privacy & Protection:** Native anti-tamper bypass for `libvkcore.so` and `libvkmedia.so` (fixes voice messages crash in clone mode), telemetry and tracking blocking, independent clone app support.

### Install

Add the repository to Morphe Manager as a remote source:

```
https://github.com/PerfLite/VK-Zen
```

Or install manually:
1. Download `patches-1.0.0.mpp` from [Releases](https://github.com/PerfLite/VK-Zen/releases/latest).
2. Import the bundle into Morphe Manager.
3. Select the official VK APK (version 8.195 recommended) and patch it.

### Build

```bash
# Build patch bundle (.mpp)
./gradlew :patches:bundleRelease

# Or build patched APK directly
./gradlew :patches:patchVk
```

### License

GPL-3.0 — see [LICENSE](LICENSE).

<p align="right"><a href="#english">↑ Back to top</a> · <a href="#русский">Русский ↓</a></p>

---

## Русский

Патчи Morphe для Android-клиента ВКонтакте — блокировка рекламы, фоновое воспроизведение музыки, скачивание в кэш, выбор качества видео и кастомизация интерфейса.

### Возможности

- **VK Видео:** Выбор предпочтительного качества по умолчанию (Авто, 1080p Full HD, 720p HD, 480p, 360p), блокировка рекламы в плеере (preroll, midroll, postroll), скрытие навязчивых баннеров установки отдельного приложения.
- **Блокировка рекламы:** Полное удаление рекламных и спонсорских записей из новостной ленты (`marked_as_ads`, `author_ad`), отключение рекламы в VK Клипах и VK Музыке, скрытие промо-баннеров в Сервисах.
- **VK Музыка:** Воспроизведение треков в фоне и при выключенном экране без подписки, сохранение треков в офлайн-кэш («Скачанные»), скрытие баннеров подписки.
- **Кастомизация интерфейса:** Настройка всех 5 вкладок нижней панели (Tabbar), удобный раздел настроек «VK Zen» прямо в приложении.
- **Приватность и защита:** Обход нативных проверок целостности и подписи в `libvkcore.so` и `libvkmedia.so` (голосовые сообщения работают стабильно), блокировка трекеров и телеметрии, режим независимого клона.

### Установка

Добавьте репозиторий в Morphe Manager как удалённый источник:

```
https://github.com/PerfLite/VK-Zen
```

Либо вручную:
1. Скачайте `patches-1.0.0.mpp` со страницы [Релизы](https://github.com/PerfLite/VK-Zen/releases/latest).
2. Импортируйте файл бандла в Morphe Manager.
3. Выберите исходный APK ВКонтакте (рекомендуется версия 8.195) и запустите сборку.

### Сборка

```bash
# Сборка бандла патчей (.mpp)
./gradlew :patches:bundleRelease

# Или сборка патченного APK напрямую
./gradlew :patches:patchVk
```

### Лицензия

GPL-3.0 — см. [LICENSE](LICENSE).

<p align="right"><a href="#русский">↑ Наверх</a> · <a href="#english">English ↑</a></p>
