package gg.nationsglory.mcpcj8;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

public final class Java8ClassPatcher implements ClassFileTransformer {

    private static final Map<String, String> TARGETS = new HashMap<String, String>();
    static {
        TARGETS.put("net/minecraft/launchwrapper/Launch", "Launch.bin");
        TARGETS.put("org/objectweb/asm/commons/Remapper", "Remapper.bin");
        TARGETS.put("org/objectweb/asm/ClassReader", "ClassReader.bin");
        TARGETS.put("org/objectweb/asm/tree/MethodInsnNode", "MethodInsnNode.bin");
    }

    private final Map<String, byte[]> patches = new HashMap<String, byte[]>();

    public Java8ClassPatcher() {
        for (Map.Entry<String, String> entry : TARGETS.entrySet()) {
            byte[] bytes = readResource(entry.getValue());
            if (bytes != null) {
                patches.put(entry.getKey(), bytes);
            } else {
                System.out.println("[mcpc-j8] Missing patch resource: " + entry.getValue());
            }
        }
        System.out.println("[mcpc-j8] " + patches.size() + " class patch(es) loaded.");
    }

    public boolean hasPatches() {
        return !patches.isEmpty();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefined,
            ProtectionDomain domain, byte[] original) {
        try {
            if (className != null) {
                byte[] patch = patches.get(className);
                if (patch != null) {
                    System.out.println("[mcpc-j8] Patched: " + className);
                    return patch;
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    private static byte[] readResource(String name) {
        InputStream in = Java8ClassPatcher.class.getResourceAsStream(name);
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
