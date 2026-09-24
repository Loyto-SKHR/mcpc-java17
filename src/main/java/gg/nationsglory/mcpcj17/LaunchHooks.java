package gg.nationsglory.mcpcj17;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Called by the patched {@code net.minecraft.launchwrapper.Launch} constructor.
 */
public final class LaunchHooks {

    /**
     * launchwrapper builds its class loader from {@code ((URLClassLoader) getClassLoader()).getURLs()};
     * the application class loader is no longer a URLClassLoader since Java 9.
     */
    public static URL[] classPath() {
        List<URL> urls = new ArrayList<URL>();
        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            try {
                urls.add(new File(entry).getAbsoluteFile().toURI().toURL());
            } catch (Exception e) {
                System.out.println("[mcpc-j17] Ignoring class path entry " + entry + ": " + e);
            }
        }
        return urls.toArray(new URL[0]);
    }

    /**
     * LaunchClassLoader has the bootstrap loader as parent. On Java 8 that covered the whole JDK;
     * since Java 9, modules such as java.sql or java.scripting live in the platform loader, so their
     * packages (javax.sql, javax.script...) must be delegated explicitly.
     */
    public static void configure(ClassLoader launchClassLoader) {
        try {
            Method exclude = launchClassLoader.getClass().getMethod("addClassLoaderExclusion", String.class);
            exclude.invoke(launchClassLoader, "gg.nationsglory.mcpcj17.");
            int count = 0;
            for (Module module : ModuleLayer.boot().modules()) {
                if (module.getClassLoader() == null) {
                    continue;
                }
                for (String pkg : module.getPackages()) {
                    if (!pkg.startsWith("java.")) {
                        exclude.invoke(launchClassLoader, pkg + ".");
                        count++;
                    }
                }
            }
            System.out.println("[mcpc-j17] LaunchClassLoader delegates " + count + " platform packages to the JDK.");
        } catch (Throwable t) {
            System.out.println("[mcpc-j17] Could not configure LaunchClassLoader, continuing: " + t);
        }
    }

    private LaunchHooks() {
    }
}
