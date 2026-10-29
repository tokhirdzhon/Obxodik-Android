package ru.obxodik.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import ru.obxodik.android.service.ObxodikVpnService;
import ru.obxodik.android.service.TgWsService;

public final class ServiceController {
    public static final String ACTION_STATUS = "ru.obxodik.android.STATUS";
    public static final String EXTRA_RUNNING = "running";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_PORT = "port";

    public static final String ACTION_START = "ru.obxodik.android.START";
    public static final String ACTION_STOP = "ru.obxodik.android.STOP";
    public static final String EXTRA_PACKAGES = "packages";
    public static final String EXTRA_ARGS = "args";
    public static final String EXTRA_SECRET = "secret";

    private ServiceController() {
    }

    public static void startVpn(Context context, List<String> packages, String[] args) {
        Intent intent = new Intent(context, ObxodikVpnService.class);
        intent.setAction(ACTION_START);
        intent.putStringArrayListExtra(EXTRA_PACKAGES, new ArrayList<>(packages));
        intent.putExtra(EXTRA_ARGS, args);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stopVpn(Context context) {
        Intent intent = new Intent(context, ObxodikVpnService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void startTgWs(Context context, int port, String secret) {
        Intent intent = new Intent(context, TgWsService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_PORT, port);
        intent.putExtra(EXTRA_SECRET, secret);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stopTgWs(Context context) {
        Intent intent = new Intent(context, TgWsService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void broadcast(Context context, Bundle extras) {
        Intent intent = new Intent(ACTION_STATUS);
        intent.setPackage(context.getPackageName());
        intent.putExtras(extras);
        context.sendBroadcast(intent);
    }
}
