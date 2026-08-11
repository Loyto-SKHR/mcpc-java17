package gg.nationsglory.mcpcj8;

import java.lang.instrument.Instrumentation;

public final class McpcJava8Agent {

    private static final String SPOOFED_JAVA_VERSION = "1.7.0_80";

    public static void premain(String agentArgs, Instrumentation inst) {
        spoofJavaVersion();
        installClassPatches(inst);
    }

    public static void premain(String agentArgs) {
        spoofJavaVersion();
        System.out.println("[mcpc-j8] No Instrumentation available: only the version check is bypassed.");
    }

    private static void spoofJavaVersion() {
        String actual = System.getProperty("java.version");
        if (actual != null && actual.startsWith("1.8")) {
            System.setProperty("java.version", SPOOFED_JAVA_VERSION);
            System.out.println("[mcpc-j8] Java 8 detected (" + actual + "), reporting "
                    + SPOOFED_JAVA_VERSION + " to bypass the MCPC+ version check.");
        } else {
            System.out.println("[mcpc-j8] java.version = " + actual + ", nothing to bypass.");
        }
    }

    private static void installClassPatches(Instrumentation inst) {
        if (inst == null) {
            return;
        }
        try {
            Java8ClassPatcher patcher = new Java8ClassPatcher();
            if (patcher.hasPatches()) {
                inst.addTransformer(patcher);
            } else {
                System.out.println("[mcpc-j8] No class patches bundled: only the version check is bypassed.");
            }
        } catch (Throwable t) {
            System.out.println("[mcpc-j8] Could not install class patches, continuing: " + t);
        }
    }

    private McpcJava8Agent() {
    }
}
