package ru.obxodik.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Prefs.master(context)) return;

        boolean yt = Prefs.youtube(context);
        boolean dc = Prefs.discord(context);
        boolean tg = Prefs.telegram(context);
        boolean ws = Prefs.tgws(context);

        List<String> packages = AppPackages.vpnPackages(context, yt, dc, tg, ws);
        if (!packages.isEmpty() && VpnService.prepare(context) == null) {
            ServiceController.startVpn(context, packages, Strategies.args(Prefs.strategy(context)));
        }

        if (tg && ws && !AppPackages.telegramPackages(context).isEmpty()) {
            int port = Prefs.findFreePort(Prefs.tgPort(context));
            Prefs.setTgPort(context, port);
            ServiceController.startTgWs(context, port, Prefs.telegramSecret(context));
        }
    }
}
