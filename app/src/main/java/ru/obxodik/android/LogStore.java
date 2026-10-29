package ru.obxodik.android;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class LogStore {
    private static final long MAX_BYTES = 512 * 1024;
    private static final Object LOCK = new Object();
    private static final String[] FILES = {"vpn.log", "tgws.log", "tgws-proxy.log"};

    private LogStore() {
    }

    public static void append(Context context, String name, String line) {
        File dir = new File(context.getFilesDir(), "logs");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File file = new File(dir, name + ".log");
        String stamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String entry = "[" + stamp + "] " + line + "\n";
        synchronized (LOCK) {
            try (FileOutputStream out = new FileOutputStream(file, true)) {
                out.write(entry.getBytes("UTF-8"));
            } catch (Exception ignored) {
                return;
            }
            if (file.length() > MAX_BYTES) {
                List<String> tail = tailLines(file, 1000);
                try (FileOutputStream out = new FileOutputStream(file, false)) {
                    for (String s : tail) out.write((s + "\n").getBytes("UTF-8"));
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    public static String readAll(Context context, int maxLines) {
        List<String> lines = new ArrayList<>();
        synchronized (LOCK) {
            for (String name : FILES) {
                File file = new File(new File(context.getFilesDir(), "logs"), name);
                if (!file.isFile()) continue;
                List<String> part = tailLines(file, maxLines);
                lines.addAll(part);
            }
        }
        int from = Math.max(0, lines.size() - maxLines);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < lines.size(); i++) sb.append(lines.get(i)).append('\n');
        return sb.length() == 0 ? "Журнал пока пуст." : sb.toString();
    }

    private static List<String> tailLines(File file, int maxLines) {
        Deque<String> deque = new ArrayDeque<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                deque.addLast(line);
                if (deque.size() > maxLines) deque.removeFirst();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return new ArrayList<>(deque);
    }
}
