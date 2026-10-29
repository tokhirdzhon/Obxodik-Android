package ru.obxodik.android;

public final class Strategies {
    public static final String[] NAMES = {
            "Универсальная",
            "Fake TLS",
            "Split",
            "Fake + split",
            "TLS record"
    };

    private static final String[][] ARGS = {
            {"--split", "1+s", "--disorder", "1+s", "--auto=torst"},
            {"--fake", "-1", "--ttl", "8"},
            {"--split", "1", "--disorder", "1"},
            {"--split", "1+s", "--disorder", "1+s", "--fake", "-1", "--ttl", "8"},
            {"--tlsrec", "1+s"}
    };

    private Strategies() {
    }

    public static String[] args(int index) {
        int i = (index >= 0 && index < ARGS.length) ? index : 0;
        return ARGS[i].clone();
    }
}
