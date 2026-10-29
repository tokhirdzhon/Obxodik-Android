package ru.obxodik.android.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import hev.htproxy.TProxyService;
import io.github.romanvht.byedpi.core.ByeDpiProxy;
import ru.obxodik.android.App;
import ru.obxodik.android.LogStore;
import ru.obxodik.android.MainActivity;
import ru.obxodik.android.R;
import ru.obxodik.android.ServiceController;
import ru.obxodik.android.StatusStore;

public class ObxodikVpnService extends VpnService {
    private static final int NOTIFICATION_ID = 1101;
    private static final int SOCKS_PORT = 1080;
    private static final String TAG = "ObxodikVpn";

    private volatile boolean running;
    private static volatile boolean proxyStarted;
    private ParcelFileDescriptor tun;
    private Thread proxyThread;
    private String lastError = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ServiceController.ACTION_STOP.equals(action)) {
            LogStore.append(this, "vpn", "Получена команда остановки");
            stopEverything();
            return START_NOT_STICKY;
        }
        if (!ServiceController.ACTION_START.equals(action)) {
            return START_NOT_STICKY;
        }

        startForegroundCompat();
        ArrayList<String> packages = intent.getStringArrayListExtra(ServiceController.EXTRA_PACKAGES);
        String[] strategyArgs = intent.getStringArrayExtra(ServiceController.EXTRA_ARGS);
        if (packages == null) packages = new ArrayList<>();
        if (strategyArgs == null) strategyArgs = new String[0];

        final ArrayList<String> allowed = packages;
        final String[] args = strategyArgs;
        new Thread(() -> startVpn(allowed, args), "vpn-start").start();
        return START_STICKY;
    }

    private void startForegroundCompat() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(
                this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(
                this, 2,
                new Intent(this, ObxodikVpnService.class).setAction(ServiceController.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_VPN)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_vpn_title))
                .setContentText(getString(R.string.notification_vpn_text))
                .setContentIntent(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, getString(R.string.notification_stop), stop)
                .build();

        ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
    }

    private synchronized void startVpn(List<String> packages, String[] strategyArgs) {
        stopInternal(false);
        lastError = "";
        LogStore.append(this, "vpn", "Запуск обхода для " + packages.size() + " приложений");
        StatusStore.setVpn(this, false, "");
        broadcast(false, "");

        String[] fullArgs = new String[4 + strategyArgs.length];
        fullArgs[0] = "--ip";
        fullArgs[1] = "127.0.0.1";
        fullArgs[2] = "--port";
        fullArgs[3] = Integer.toString(SOCKS_PORT);
        System.arraycopy(strategyArgs, 0, fullArgs, 4, strategyArgs.length);

        running = true;
        if (!proxyStarted) {
            proxyStarted = true;
            proxyThread = new Thread(() -> {
                int code = ByeDpiProxy.jniStartProxy(fullArgs);
                proxyStarted = false;
                LogStore.append(this, "vpn", "ByeDPI завершился, код " + code);
                if (running) {
                    fail("ByeDPI остановился, код " + code);
                }
            }, "byedpi");
            proxyThread.start();
        } else {
            LogStore.append(this, "vpn", "ByeDPI уже запущен, переиспользую");
        }

        if (!waitPort(SOCKS_PORT, 6000)) {
            fail("ByeDPI не начал слушать порт " + SOCKS_PORT);
            return;
        }

        Builder builder = new Builder();
        builder.setSession("Obxodik");
        builder.setMtu(1500);
        builder.addAddress("198.18.0.1", 32);
        builder.addRoute("0.0.0.0", 0);
        builder.addAddress("fc00::1", 128);
        builder.addRoute("::", 0);
        builder.addDnsServer("1.1.1.1");
        builder.addDnsServer("8.8.8.8");

        int added = 0;
        for (String pkg : packages) {
            try {
                builder.addAllowedApplication(pkg);
                added++;
            } catch (Exception e) {
                LogStore.append(this, "vpn", "Приложение не добавлено: " + pkg + ": " + e);
                android.util.Log.e(TAG, "addAllowedApplication failed for " + pkg, e);
            }
        }
        if (added == 0) {
            fail("Не выбрано ни одного установленного приложения");
            return;
        }

        builder.setBlocking(true);
        try {
            tun = builder.establish();
        } catch (Exception e) {
            fail("Не удалось создать VPN: " + e.getMessage());
            return;
        }
        if (tun == null) {
            fail("VPN не разрешён. Выдайте разрешение и повторите.");
            return;
        }

        File config = new File(getFilesDir(), "hev.yml");
        File hevLog = new File(getFilesDir(), "hev.log");
        String yaml = "tunnel:\n"
                + "  mtu: 1500\n"
                + "  ipv4: 198.18.0.1\n"
                + "  ipv6: 'fc00::1'\n"
                + "socks5:\n"
                + "  address: 127.0.0.1\n"
                + "  port: " + SOCKS_PORT + "\n"
                + "  udp: 'udp'\n"
                + "misc:\n"
                + "  log-file: '" + hevLog.getAbsolutePath().replace("'", "''") + "'\n"
                + "  log-level: warn\n";
        try (FileOutputStream out = new FileOutputStream(config)) {
            out.write(yaml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            fail("Не удалось записать конфиг hev: " + e.getMessage());
            return;
        }

        boolean started;
        try {
            started = TProxyService.TProxyStartService(config.getAbsolutePath(), tun.getFd());
        } catch (Throwable t) {
            fail("Ошибка запуска tun2socks: " + t.getMessage());
            return;
        }
        if (!started) {
            fail("tun2socks не запустился");
            return;
        }

        running = true;
        lastError = "";
        LogStore.append(this, "vpn", "Обход запущен, приложений: " + added);
        StatusStore.setVpn(this, true, "");
        broadcast(true, "");
    }

    private boolean waitPort(int port, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (!running) return false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
                return true;
            } catch (Exception ignored) {
                try {
                    Thread.sleep(120);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private synchronized void fail(String message) {
        lastError = message;
        LogStore.append(this, "vpn", "Ошибка: " + message);
        StatusStore.setVpn(this, false, message);
        broadcast(false, message);
        stopInternal(false);
        stopForegroundCompat();
        stopSelf();
    }

    private synchronized void stopEverything() {
        stopInternal(true);
        stopForegroundCompat();
        stopSelf();
    }

    private synchronized void stopInternal(boolean updateStatus) {
        running = false;
        try {
            TProxyService.TProxyStopService();
        } catch (Throwable ignored) {
            // library may not have started
        }
        if (tun != null) {
            try {
                tun.close();
            } catch (Exception ignored) {
                // already closed
            }
            tun = null;
        }
        if (proxyThread != null) {
            try {
                proxyThread.join(1200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            proxyThread = null;
        }
        if (updateStatus) {
            StatusStore.setVpn(this, false, lastError);
            broadcast(false, lastError);
            LogStore.append(this, "vpn", "Обход остановлен");
        }
    }

    private void stopForegroundCompat() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        } catch (Throwable ignored) {
            // already stopped
        }
    }

    private void broadcast(boolean isRunning, String error) {
        Bundle extras = new Bundle();
        extras.putBoolean(ServiceController.EXTRA_RUNNING, isRunning);
        extras.putString(ServiceController.EXTRA_ERROR, error == null ? "" : error);
        ServiceController.broadcast(this, extras);
    }

    @Override
    public void onRevoke() {
        LogStore.append(this, "vpn", "VPN отозван системой");
        stopEverything();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopInternal(true);
        super.onDestroy();
        // ByeDPI's JNI shim can only be started once per process, so a clean
        // process is required for the next run.
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
