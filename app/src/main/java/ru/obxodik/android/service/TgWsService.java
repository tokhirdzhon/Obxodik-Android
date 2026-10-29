package ru.obxodik.android.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;

import ru.obxodik.android.App;
import ru.obxodik.android.LogStore;
import ru.obxodik.android.MainActivity;
import ru.obxodik.android.R;
import ru.obxodik.android.ServiceController;
import ru.obxodik.android.StatusStore;

public class TgWsService extends Service {
    private static final int NOTIFICATION_ID = 1201;
    private volatile boolean running;
    private int port;
    private String secret = "";
    private String link = "";
    private String lastError = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ServiceController.ACTION_STOP.equals(action)) {
            LogStore.append(this, "tgws", "Остановка WS-прокси");
            stopEverything();
            return START_NOT_STICKY;
        }
        if (!ServiceController.ACTION_START.equals(action)) {
            return START_NOT_STICKY;
        }

        if (running) {
            LogStore.append(this, "tgws", "WS-прокси уже запущен");
            return START_STICKY;
        }

        port = intent.getIntExtra(ServiceController.EXTRA_PORT, 1443);
        String incomingSecret = intent.getStringExtra(ServiceController.EXTRA_SECRET);
        secret = incomingSecret == null ? "" : incomingSecret;
        link = "tg://proxy?server=127.0.0.1&port=" + port + "&secret=dd" + secret;

        startForegroundCompat();
        running = true;
        lastError = "";
        StatusStore.setTg(this, true, link, port, "");
        broadcast(true, "");

        File logFile = new File(getFilesDir(), "tgws-proxy.log");
        LogStore.append(this, "tgws", "Запуск WS-прокси на 127.0.0.1:" + port);
        new Thread(() -> {
            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getApplicationContext()));
                }
                Python.getInstance()
                        .getModule("tgws_entry")
                        .callAttr("start", "127.0.0.1", port, secret, logFile.getAbsolutePath());
                LogStore.append(this, "tgws", "Python-прокси завершился");
            } catch (Throwable t) {
                lastError = String.valueOf(t.getMessage());
                running = false;
                LogStore.append(this, "tgws", "Ошибка Python-прокси: " + lastError);
                StatusStore.setTg(this, false, link, port, lastError);
                broadcast(false, lastError);
                try {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
                } catch (Throwable ignored) {
                    // already stopped
                }
                stopSelf();
            }
        }, "tgws-python").start();

        return START_STICKY;
    }

    private void startForegroundCompat() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(
                this, 11, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(
                this, 12,
                new Intent(this, TgWsService.class).setAction(ServiceController.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_TG)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_tg_title))
                .setContentText(getString(R.string.notification_tg_text))
                .setContentIntent(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, getString(R.string.notification_stop), stop)
                .build();

        ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    }

    private synchronized void stopEverything() {
        running = false;
        StatusStore.setTg(this, false, link, port, lastError);
        broadcast(false, lastError);
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        } catch (Throwable ignored) {
            // already stopped
        }
        stopSelf();
    }

    private void broadcast(boolean isRunning, String error) {
        Bundle extras = new Bundle();
        extras.putBoolean(ServiceController.EXTRA_RUNNING, isRunning);
        extras.putString(ServiceController.EXTRA_ERROR, error == null ? "" : error);
        extras.putInt(ServiceController.EXTRA_PORT, port);
        ServiceController.broadcast(this, extras);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        StatusStore.setTg(this, false, link, port, lastError);
        broadcast(false, lastError);
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        } catch (Throwable ignored) {
            // already stopped
        }
        super.onDestroy();
        // The Python asyncio loop cannot be cancelled cleanly; this service lives
        // in a dedicated :tgws process, so killing it guarantees a clean stop.
        Process.killProcess(Process.myPid());
    }
}
