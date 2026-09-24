package gg.nationsglory.mcpcj17;

import java.lang.instrument.Instrumentation;

public final class McpcJava17Agent {

    private static final String SPOOFED_JAVA_VERSION = "1.7.0_80";

    public static void premain(String agentArgs, Instrumentation inst) {
        spoofJavaVersion();
        ModuleAccess.openJdkModules(inst);
        ModuleAccess.removeReflectionFilters();
        installClassPatches(inst);
    }

    private static void spoofJavaVersion() {
        String actual = System.getProperty("java.version");
        if (actual != null && !actual.startsWith("1.7")) {
            System.setProperty("java.version", SPOOFED_JAVA_VERSION);
            System.out.println("[mcpc-j17] Java " + actual + " detected, reporting "
                    + SPOOFED_JAVA_VERSION + " to bypass the MCPC+ version check.");
        } else {
            System.out.println("[mcpc-j17] java.version = " + actual + ", nothing to bypass.");
        }
    }

    private static void installClassPatches(Instrumentation inst) {
        try {
            inst.addTransformer(new ClassPatcher());
        } catch (Throwable t) {
            System.out.println("[mcpc-j17] Could not install class patches, continuing: " + t);
        }
    }

    private McpcJava17Agent() {
    }
}
