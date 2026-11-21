# VaultTune (Desktop)

Настольный порт [ArchiveTune](https://github.com/rukamori/ArchiveTune) — клиента YouTube Music
для Android — на **Windows x64**. Это форк: код на Kotlin + Compose Multiplatform, бэкенд
(клиент InnerTube, youtubei.js, провайдеры текстов) переиспользован как есть.

## Статус

Рабочий **MVP**: поиск → воспроизведение → вход в аккаунт.

| Функция | Статус |
|---|---|
| Поиск YouTube Music | ✅ |
| Воспроизведение (VLCJ/libVLC) | ✅ |
| Резолв потока (youtubei.js + poToken) | ✅ |
| Вход в Google-аккаунт (Playwright) | ✅ |
| Библиотека / плейлисты / тексты / EQ | ❌ ещё не перенесены |

## Требования

- **JDK 21** (напр. Temurin): `JAVA_HOME` должен указывать на него.
- **Chrome** (для входа в аккаунт — используется реальный Chrome).
- **Chromium для Playwright** (для чеканки poToken при воспроизведении).

## Первый запуск

```powershell
# один раз — скачать Chromium для Playwright (~150 МБ)
.\gradlew.bat installPlaywright

# запустить приложение
.\gradlew.bat run
```

`JAVA_HOME` на Windows можно прописать раз и навсегда:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\path\to\jdk", "User")
```

## Сборка `.exe`

```powershell
.\gradlew.bat createDistributable
```

Результат: `desktop\build\compose\binaries\main\app\VaultTune\VaultTune.exe`
(приложение + встроенная JRE).

> ⚠️ Chromium для Playwright сейчас **не вшивается** в `.exe` автоматически. Для переноса на
> другой ПК нужно положить папку с Chromium в `VaultTune\browsers\` — тогда приложение найдёт её
> через `ChromeLocator` (ищет `browsers\**\chrome.exe`).

## Архитектура

```
desktop/                    — Compose Multiplatform-приложение (Windows)
  src/main/kotlin/com/vaulttune/
    Main.kt                 — окно, поиск, Play, Login
    PlaybackService.kt      — поиск → poToken → резолв потока
    YouTubeLoginService.kt  — вход в аккаунт (Playwright + cookies)
    AudioPlayer/VlcjPlayer  — воспроизведение через libVLC
    potoken/                — BotGuard-чеканка poToken (Playwright)
core/                       — клиент InnerTube (submodule, чистый JVM)
lyrics/                     — провайдеры текстов (submodule, чистый JVM)
morideobfuscator/           — youtubei.js-движок (submodule, портирован на JVM)
```

## Как это работает (кратко)

1. **Поиск** — `core` (InnerTube) ходит в YouTube Music.
2. **Воспроизведение** — `morideobfuscator` (youtubei.js через QuickJS) резолвит поток; для
   анонимного плея нужен **poToken**, который чеканит `PlaywrightPoTokenMinter`, запуская
   BotGuard-челлендж YouTube в Chromium.
3. **Звук** — `VlcjPlayer` (libVLC) стримит полученный `googlevideo.com` URL.
4. **Логин** — `YouTubeLoginService` открывает реальный Chrome, ждёт входа, извлекает cookies и
   применяет их к клиенту InnerTube.

## Лицензия

GPLv3 (как ArchiveTune). Это независимый форк, не связанный с официальным ArchiveTune.
