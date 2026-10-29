package ru.obxodik.android;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public final class AppPackages {
    public static final String YOUTUBE = "com.google.android.youtube";
    public static final String DISCORD = "com.discord";
    public static final String[] TELEGRAM = {
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.messenger.beta",
            "org.telegram.plus"
    };

    private AppPackages() {
    }

    public static boolean installed(Context context, String pkg) {
        try {
            context.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static List<String> telegramPackages(Context context) {
        List<String> result = new ArrayList<>();
        for (String pkg : TELEGRAM) {
            if (installed(context, pkg)) result.add(pkg);
        }
        return result;
    }

    public static List<String> vpnPackages(Context context, boolean youtube, boolean discord,
                                           boolean telegram, boolean tgws) {
        List<String> result = new ArrayList<>();
        if (youtube && installed(context, YOUTUBE)) result.add(YOUTUBE);
        if (discord && installed(context, DISCORD)) result.add(DISCORD);
        if (telegram && !tgws) result.addAll(telegramPackages(context));
        return result;
    }
}
