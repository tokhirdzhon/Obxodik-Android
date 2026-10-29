package ru.obxodik.android;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class StatusStore {
    private static final String VPN_FILE = "vpn_status.properties";
    private static final String TG_FILE = "tgws_status.properties";
    private static final Object LOCK = new Object();

    public static final class VpnStatus {
        public final boolean running;
        public final String error;

        VpnStatus(boolean running, String error) {
            this.running = running;
            this.error = error;
        }
    }

    public static final class TgStatus {
        public final boolean running;
        public final String link;
        public final int port;
        public final String error;

        TgStatus(boolean running, String link, int port, String error) {
            this.running = running;
            this.link = link;
            this.port = port;
            this.error = error;
        }
    }

    private StatusStore() {
    }

    public static void setVpn(Context context, boolean running, String error) {
        Properties p = new Properties();
        p.setProperty("running", Boolean.toString(running));
        p.setProperty("error", error == null ? "" : error);
        write(context, VPN_FILE, p);
    }

    public static void setTg(Context context, boolean running, String link, int port, String error) {
        Properties p = new Properties();
        p.setProperty("running", Boolean.toString(running));
        p.setProperty("link", link == null ? "" : link);
        p.setProperty("port", Integer.toString(port));
        p.setProperty("error", error == null ? "" : error);
        write(context, TG_FILE, p);
    }

    public static VpnStatus vpn(Context context) {
        Properties p = read(context, VPN_FILE);
        return new VpnStatus(
                Boolean.parseBoolean(p.getProperty("running", "false")),
                p.getProperty("error", ""));
    }

    public static TgStatus tg(Context context) {
        Properties p = read(context, TG_FILE);
        int port;
        try {
            port = Integer.parseInt(p.getProperty("port", "0"));
        } catch (NumberFormatException e) {
            port = 0;
        }
        return new TgStatus(
                Boolean.parseBoolean(p.getProperty("running", "false")),
                p.getProperty("link", ""),
                port,
                p.getProperty("error", ""));
    }

    private static void write(Context context, String name, Properties p) {
        synchronized (LOCK) {
            File target = new File(context.getFilesDir(), name);
            File temp = new File(context.getFilesDir(), name + ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                p.store(out, null);
            } catch (Exception e) {
                return;
            }
            if (!temp.renameTo(target)) {
                //noinspection ResultOfMethodCallIgnored
                target.delete();
                //noinspection ResultOfMethodCallIgnored
                temp.renameTo(target);
            }
        }
    }

    private static Properties read(Context context, String name) {
        Properties p = new Properties();
        synchronized (LOCK) {
            File file = new File(context.getFilesDir(), name);
            if (!file.isFile()) return p;
            try (FileInputStream in = new FileInputStream(file)) {
                p.load(in);
            } catch (Exception ignored) {
                // return empty defaults
            }
        }
        return p;
    }
}
