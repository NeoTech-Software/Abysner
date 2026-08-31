/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.plugin.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * JVM agent that extends the compose screenshot tests render sandbox's classloader allowlist so a
 * JVM coverage agent (Kover) can run inside it. Without it, every screenshot render crashes with
 * NoClassDefFoundError on the coverage agent's own runtime helper classes.
 */
public final class RenderClassLoaderPatchAgent implements ClassFileTransformer {

    private static final String RENDERER_PACKAGE = "com/android/tools/render/";

    private static final String CONSTANTS_CLASS = "com.android.tools.render.ClassLoaderConstantsKt";
    private static final String ALLOWLIST_GETTER = "getALLOWED_PACKAGES_FROM_PARENT";
    private static final String COVERAGE_PACKAGE_PREFIX = "com.intellij.rt.";

    // Renderer classes load twice: once in the app classloader, once in the isolated one that
    // actually renders. Only the isolated copy matters, but they're indistinguishable from
    // here, so every classloader that defines renderer classes gets patched.
    private final Set<ClassLoader> patchedLoaders =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static void premain(String arguments, Instrumentation instrumentation) {
        instrumentation.addTransformer(new RenderClassLoaderPatchAgent());
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (loader != null
                && className != null
                && className.startsWith(RENDERER_PACKAGE)
                // addCoveragePackageToAllowlist() force-loads this class below. It's also under
                // RENDERER_PACKAGE, so without this check that load would trigger this same
                // branch again, forever.
                && !className.equals(CONSTANTS_CLASS.replace('.', '/'))
                && !patchedLoaders.contains(loader)) {
            if (addCoveragePackageToAllowlist(loader)) {
                patchedLoaders.add(loader);
            }
        }
        // This transformer never actually changes any bytecode. Only watches for the trigger class
        // to patch the loader.
        return null;
    }

    // FilteringClassLoader rejects any class outside this allowlist, coverage agent classes
    // included.
    private static boolean addCoveragePackageToAllowlist(ClassLoader loader) {
        try {
            // Yeah, this is all a bit hacky, easily breaks, however good enough for our setup.
            Class<?> constants = Class.forName(CONSTANTS_CLASS, true, loader);
            @SuppressWarnings("unchecked")
            List<String> allowed = (List<String>) constants.getMethod(ALLOWLIST_GETTER).invoke(null);
            if (allowed.contains(COVERAGE_PACKAGE_PREFIX)) {
                return true;
            }
            // Kotlin's listOf(vararg) returns Arrays.asList, an array-backed view. A bigger
            // backing array adds to the allowlist for every render after this one.
            Field backingArray = allowed.getClass().getDeclaredField("a");
            backingArray.setAccessible(true);
            String[] current = (String[]) backingArray.get(allowed);
            String[] extended = Arrays.copyOf(current, current.length + 1);
            extended[current.length] = COVERAGE_PACKAGE_PREFIX;
            backingArray.set(allowed, extended);
            return true;
        } catch (Throwable throwable) {
            System.err.println(
                    "w: Unable to extend the renderer classloader allowlist, screenshot test coverage will fail with NoClassDefFoundError: " + throwable
            );
            return false;
        }
    }
}
