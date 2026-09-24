package gg.nationsglory.mcpcj17;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

public final class ClassPatcher implements ClassFileTransformer {

    private static final Map<String, String> TARGETS = new HashMap<String, String>();
    static {
        TARGETS.put("net/minecraft/launchwrapper/Launch", "Launch.bin");
        TARGETS.put("org/objectweb/asm/commons/Remapper", "Remapper.bin");
        TARGETS.put("org/objectweb/asm/ClassReader", "ClassReader.bin");
        TARGETS.put("org/objectweb/asm/tree/MethodInsnNode", "MethodInsnNode.bin");
    }

    /** JDK 8 internals removed in Java 9, and their replacements in this agent. */
    private static final String[][] REDIRECTS = {
        {"sun/reflect/ReflectionFactory", "gg/nationsglory/mcpcj17/shim/ReflectionFactory"},
        {"sun/reflect/ConstructorAccessor", "gg/nationsglory/mcpcj17/shim/ConstructorAccessor"},
        {"sun/reflect/FieldAccessor", "gg/nationsglory/mcpcj17/shim/FieldAccessor"},
    };
    private static final byte[] SUN_REFLECT_SLASH = "sun/reflect/".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SUN_REFLECT_DOT = "sun.reflect.".getBytes(StandardCharsets.US_ASCII);

    private final Map<String, byte[]> patches = new HashMap<String, byte[]>();

    public ClassPatcher() {
        for (Map.Entry<String, String> entry : TARGETS.entrySet()) {
            byte[] bytes = readResource(entry.getValue());
            if (bytes != null) {
                patches.put(entry.getKey(), bytes);
            } else {
                System.out.println("[mcpc-j17] Missing patch resource: " + entry.getValue());
            }
        }
        System.out.println("[mcpc-j17] " + patches.size() + " class patch(es) loaded.");
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefined,
            ProtectionDomain domain, byte[] original) {
        try {
            if (className != null) {
                byte[] patch = patches.get(className);
                if (patch != null) {
                    System.out.println("[mcpc-j17] Patched: " + className);
                    return patch;
                }
                if (loader != null && !className.startsWith("gg/nationsglory/mcpcj17/")
                        && (indexOf(original, SUN_REFLECT_SLASH) >= 0 || indexOf(original, SUN_REFLECT_DOT) >= 0)) {
                    byte[] redirected = redirectSunReflect(original);
                    if (redirected != null) {
                        System.out.println("[mcpc-j17] Redirected sun.reflect in: " + className);
                        return redirected;
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("[mcpc-j17] Could not patch " + className + ", leaving it untouched: " + t);
        }
        return null;
    }

    /**
     * Rewrites the UTF-8 constants of the class that name one of the {@link #REDIRECTS}, in internal
     * form (type references, descriptors) or dotted form (Class.forName strings). Returns null when
     * nothing changed.
     */
    static byte[] redirectSunReflect(byte[] b) {
        int count = u2(b, 8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length + 256);
        out.write(b, 0, 10);
        boolean changed = false;
        int pos = 10;
        for (int i = 1; i < count; i++) {
            int tag = b[pos] & 0xFF;
            int size;
            switch (tag) {
                case 1:
                    int length = u2(b, pos + 1);
                    String value = new String(b, pos + 3, length, StandardCharsets.ISO_8859_1);
                    String replaced = redirect(value);
                    if (replaced != value) {
                        byte[] bytes = replaced.getBytes(StandardCharsets.ISO_8859_1);
                        out.write(1);
                        out.write(bytes.length >>> 8);
                        out.write(bytes.length);
                        out.write(bytes, 0, bytes.length);
                        changed = true;
                        pos += 3 + length;
                        continue;
                    }
                    size = 3 + length;
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    size = 5;
                    break;
                case 5: case 6:
                    size = 9;
                    i++;
                    break;
                case 7: case 8: case 16: case 19: case 20:
                    size = 3;
                    break;
                case 15:
                    size = 4;
                    break;
                default:
                    throw new IllegalStateException("Unknown constant pool tag " + tag);
            }
            out.write(b, pos, size);
            pos += size;
        }
        if (!changed) {
            return null;
        }
        out.write(b, pos, b.length - pos);
        return out.toByteArray();
    }

    /** Returns {@code value} itself when it holds no redirected name. */
    private static String redirect(String value) {
        if (!value.contains("sun/reflect/") && !value.contains("sun.reflect.")) {
            return value;
        }
        String result = value;
        for (String[] redirect : REDIRECTS) {
            result = replaceName(result, redirect[0], redirect[1]);
            result = replaceName(result, redirect[0].replace('/', '.'), redirect[1].replace('/', '.'));
        }
        return result.equals(value) ? value : result;
    }

    /** Replaces whole class names only: sun/reflect/FieldAccessor, not sun/reflect/FieldAccessorImpl. */
    private static String replaceName(String value, String from, String to) {
        StringBuilder sb = null;
        int copied = 0;
        int search = 0;
        int index;
        while ((index = value.indexOf(from, search)) >= 0) {
            int end = index + from.length();
            search = end;
            if (end < value.length() && Character.isJavaIdentifierPart(value.charAt(end))) {
                continue;
            }
            if (sb == null) {
                sb = new StringBuilder(value.length() + 32);
            }
            sb.append(value, copied, index).append(to);
            copied = end;
        }
        if (sb == null) {
            return value;
        }
        return sb.append(value, copied, value.length()).toString();
    }

    private static int u2(byte[] b, int pos) {
        return ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] readResource(String name) {
        try (InputStream in = ClassPatcher.class.getResourceAsStream(name)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (Throwable t) {
            return null;
        }
    }
}
