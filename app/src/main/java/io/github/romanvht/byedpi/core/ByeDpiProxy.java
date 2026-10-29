package io.github.romanvht.byedpi.core;

/**
 * Thin Java binding for the JNI entry points shipped in libbyedpi.so.
 * The native code is from ByeDPIAndroid (GPL-3.0), built around MIT ByeDPI.
 * This class only exposes the two functions; no upstream Kotlin code is reused.
 */
public final class ByeDpiProxy {
    static {
        System.loadLibrary("hev-socks5-tunnel");
        System.loadLibrary("byedpi");
    }

    private ByeDpiProxy() {
    }

    public static native int jniStartProxy(String[] args);

    public static native int jniStopProxy();
}
