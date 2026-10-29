# Сторонние компоненты Obxodik Android

Android-приложение Obxodik использует следующие открытые компоненты.

## ByeDPIAndroid / libbyedpi.so

- Файлы: `app/src/main/jniLibs/arm64-v8a/libbyedpi.so`, `app/src/main/jniLibs/x86_64/libbyedpi.so`.
- Проект: https://github.com/romanvht/ByeDPIAndroid
- Версия: v1.8.2 (universal release APK, 02.10.2026).
- Лицензия: **GPL-3.0**. Именно поэтому всё Android-приложение распространяется по GPL-3.0.
- Бинарник взят без изменений из официального APK; исходники JNI-обёртки и инструкция
  по сборке: `native/README.md`.
- В основе лежит ByeDPI (MIT): https://github.com/hufrea/byedpi

## hev-socks5-tunnel

- Файл: `app/libs/hev-socks5-tunnel.aar`.
- Проект: https://github.com/heiher/hev-socks5-tunnel
- Версия: 2.19.3.
- Лицензия: MIT.
- Используется как tun2socks: перенаправляет трафик из VpnService в локальный SOCKS5 ByeDPI.

## TG WS Proxy

- Исходники: `app/src/main/python/proxy/**`.
- Проект: https://github.com/Flowseal/tg-ws-proxy
- Версия: v1.11.2 (11.10.2026).
- Лицензия: MIT.
- Локальный MTProto → WebSocket прокси для Telegram.

## Chaquopy

- Используется для запуска Python-части (`proxy`, `cryptography`, `httpx`, `h2`, `certifi`)
  внутри Android-приложения.
- Сайт: https://chaquo.com/chaquopy/
- Лицензия и условия: https://chaquo.com/chaquopy/license/

## Python-пакеты

- cryptography — Apache-2.0/BSD; https://github.com/pyca/cryptography
- httpx — BSD-3-Clause; https://github.com/encode/httpx
- h2 — MIT; https://github.com/python-hyper/h2
- certifi — MPL-2.0; https://github.com/certifi/python-certifi

## Simple Icons

- Векторные контуры логотипов YouTube, Discord и Telegram взяты из Simple Icons (CC0 1.0).
- Логотипы принадлежат соответствующим правообладателям и используются для обозначения
  совместимости.

## Obxodik Windows

- https://github.com/tokhirdzhon/Obxodik-Windows
- Windows-часть распространяется по MIT и использует zapret (MIT), WinDivert (LGPL)
  и TG WS Proxy (MIT).

## Полный текст GPL-3.0

См. файл [LICENSE](LICENSE).
