# Нативные библиотеки

`app/src/main/jniLibs/*/libbyedpi.so` — **prebuilt** JNI-библиотека из
[ByeDPIAndroid v1.8.2](https://github.com/romanvht/ByeDPIAndroid/releases/tag/v.1.8.2)
(universal APK, 02.10.2026). Файлы взяты без изменений.

Почему бинарник, а не сборка из исходников:

- ByeDPIAndroid — GPL-3.0, и его JNI-обёртка (`byedpi-jni.c`) лицензируется GPL-3.0.
- Мы используем только эту обёртку и MIT-ядро ByeDPI, поэтому Android-приложение
  распространяется по GPL-3.0.
- Исходники обёртки включены в `native/byedpi-jni.c` и `native/hev-jni.c`.
- Исходники ByeDPI: https://github.com/hufrea/byedpi (MIT).
- Инструкция по сборке: https://github.com/romanvht/ByeDPIAndroid#building
  (нужны NDK и подмодули `byedpi` + `hev-socks5-tunnel`).

Единственный Java-класс, который мы добавляем к этому бинарнику, —
`io.github.romanvht.byedpi.core.ByeDpiProxy`, повторяющий JNI-сигнатуры
`jniStartProxy(String[])` и `jniStopProxy()`.

`app/libs/hev-socks5-tunnel.aar` — официальный AAR из
[hev-socks5-tunnel 2.19.3](https://github.com/heiher/hev-socks5-tunnel/releases/tag/2.19.3)
(MIT). Используется через `hev.htproxy.TProxyService`.

## Ограничение JNI

`libbyedpi.so` допускает только один запуск ByeDPI на процесс. Поэтому
`ObxodikVpnService` переиспользует уже запущенный ByeDPI при смене стратегии и
завершает процесс `:vpn` при полной остановке, чтобы следующий запуск был чистым.
