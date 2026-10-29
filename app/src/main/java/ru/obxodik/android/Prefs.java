package ru.obxodik.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.ServerSocket;
import java.security.SecureRandom;

public final class Prefs {
    private static final String FILE = "obxodik";
    private static final String KEY_YT = "youtube";
    private static final String KEY_DC = "discord";
    private static final String KEY_TG = "telegram";
    private static final String KEY_TGWS = "tgws";
    private static final String KEY_MASTER = "master";
    private static final String KEY_STRATEGY = "strategy";
    private static final String KEY_TG_PORT = "tg_port";
    private static final String KEY_TG_SECRET = "tg_secret";

    private Prefs() {
    }

    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean youtube(Context c) {
        return sp(c).getBoolean(KEY_YT, true);
    }

    public static boolean discord(Context c) {
        return sp(c).getBoolean(KEY_DC, true);
    }

    public static boolean telegram(Context c) {
        return sp(c).getBoolean(KEY_TG, true);
    }

    public static boolean tgws(Context c) {
        return sp(c).getBoolean(KEY_TGWS, true);
    }

    public static boolean master(Context c) {
        return sp(c).getBoolean(KEY_MASTER, false);
    }

    public static int strategy(Context c) {
        return sp(c).getInt(KEY_STRATEGY, 0);
    }

    public static int tgPort(Context c) {
        return sp(c).getInt(KEY_TG_PORT, 1443);
    }

    public static void setYoutube(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_YT, value).apply();
    }

    public static void setDiscord(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_DC, value).apply();
    }

    public static void setTelegram(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_TG, value).apply();
    }

    public static void setTgws(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_TGWS, value).apply();
    }

    public static void setMaster(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_MASTER, value).apply();
    }

    public static void setStrategy(Context c, int value) {
        sp(c).edit().putInt(KEY_STRATEGY, value).apply();
    }

    public static void setTgPort(Context c, int port) {
        sp(c).edit().putInt(KEY_TG_PORT, port).apply();
    }

    public static String telegramSecret(Context c) {
        String secret = sp(c).getString(KEY_TG_SECRET, "");
        if (secret != null && secret.matches("[0-9a-f]{32}")) return secret;
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        String value = sb.toString();
        sp(c).edit().putString(KEY_TG_SECRET, value).apply();
        return value;
    }

    public static int findFreePort(int preferred) {
        for (int port = preferred; port < preferred + 30 && port < 65535; port++) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.setReuseAddress(false);
                socket.bind(new java.net.InetSocketAddress("127.0.0.1", port));
                return port;
            } catch (Exception ignored) {
                // try next port
            }
        }
        return 1443;
    }
}
