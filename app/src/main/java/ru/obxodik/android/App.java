package ru.obxodik.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String CHANNEL_VPN = "obxodik_vpn";
    public static final String CHANNEL_TG = "obxodik_tgws";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel vpn = new NotificationChannel(
                CHANNEL_VPN,
                getString(R.string.notification_channel_vpn),
                NotificationManager.IMPORTANCE_LOW);
        vpn.setDescription(getString(R.string.notification_vpn_text));
        nm.createNotificationChannel(vpn);

        NotificationChannel tg = new NotificationChannel(
                CHANNEL_TG,
                getString(R.string.notification_channel_tg),
                NotificationManager.IMPORTANCE_LOW);
        tg.setDescription(getString(R.string.notification_tg_text));
        nm.createNotificationChannel(tg);
    }
}
