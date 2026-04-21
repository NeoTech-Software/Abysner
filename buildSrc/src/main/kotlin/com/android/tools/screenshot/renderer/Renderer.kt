package com.android.tools.screenshot.renderer

import com.android.tools.render.common.PreviewScreenshot
import com.android.tools.render.common.PreviewScreenshotResult
import com.android.tools.screenshot.PreviewScreenshotTestEngineInput.RendererInput
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration

/**
 * Shadowed copy of the Renderer from the Android Screenshot Validation JUnit Engine
 * (com.android.tools.screenshot:screenshot-validation-junit-engine:0.0.1-alpha14). Placed in the
 * same package so it takes precedence on the classpath. The only change is replacing the plain
 * URLClassLoader with one that falls back to the SystemClassLoader for resource lookups, allowing
 * the Kover coverage agent's ASM frame computation to resolve class resources without breaking
 * layoutlib's class loading isolation.
 */
class Renderer : Closeable {

    private val isolatedClassLoaderForRendering: ClassLoader
    private val rendererInstance: Closeable

    init {
        val fontsPath = RendererInput.fontsPath.absolutePath.ifBlank { null }
        val resourceApkPath = RendererInput.resourceApkPath.absolutePath.ifBlank { null }
        val namespace = RendererInput.namespace
        val classPath = (RendererInput.mainAllClassPath + RendererInput.screenshotAllClassPath)
        val projectClassPath = (RendererInput.mainProjectClassPath + RendererInput.screenshotProjectClassPath)
        val layoutLibClassPath = RendererInput.layoutlibClassPath
        val layoutlibDataDir = RendererInput.layoutlibDataDir

        val platformClassLoader = ClassLoader::class.java.getMethod("getPlatformClassLoader").invoke(null) as ClassLoader

        val urls = layoutLibClassPath.map { it.toURI().toURL() }.toTypedArray()
        // This is the only change from the original: use a resource-enhanced class loader instead
        // of a plain URLClassLoader(urls, platformClassLoader).
        isolatedClassLoaderForRendering = createResourceEnhancedClassLoader(urls, platformClassLoader)

        val rendererClass = isolatedClassLoaderForRendering.loadClass(
            com.android.tools.render.Renderer::class.java.name
        )

        val constructor = rendererClass.getConstructor(
            String::class.java,
            String::class.java,
            String::class.java,
            List::class.java,
            List::class.java,
            String::class.java,
            List::class.java,
            List::class.java,
        )

        rendererInstance = constructor.newInstance(
            fontsPath,
            resourceApkPath,
            namespace,
            classPath.map { it.absolutePath },
            projectClassPath.map { it.absolutePath },
            layoutlibDataDir.absolutePath,
            RendererInput.testRuntimeResourceDirs.map { it.absolutePath },
            RendererInput.testRuntimeRClassJars.map { it.absolutePath },
        ) as Closeable
    }

    fun render(screenshot: PreviewScreenshot, outputFolderPath: String): List<PreviewScreenshotResult> {
        val copiedScreenshot = copyObject(screenshot, isolatedClassLoaderForRendering)

        val previewScreenshotClass = isolatedClassLoaderForRendering.loadClass(PreviewScreenshot::class.java.name)
        val renderMethod = rendererInstance.javaClass.getMethod("render", previewScreenshotClass, String::class.java)

        val resultFromIsolatedLoader = renderMethod.invoke(rendererInstance, copiedScreenshot, outputFolderPath) as Serializable

        val resultInCurrentLoader = copyObject(resultFromIsolatedLoader, this.javaClass.classLoader!!)

        @Suppress("UNCHECKED_CAST")
        return resultInCurrentLoader as List<PreviewScreenshotResult>
    }

    private fun copyObject(obj: Serializable, targetClassLoader: ClassLoader): Serializable {
        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(obj) }
        val bytes = byteOut.toByteArray()

        val byteIn = ByteArrayInputStream(bytes)
        val objectIn = object : ObjectInputStream(byteIn) {
            override fun resolveClass(desc: ObjectStreamClass): Class<*> =
                Class.forName(desc.name, false, targetClassLoader)
        }

        @Suppress("UNCHECKED_CAST")
        return objectIn.use { it.readObject() as Serializable }
    }

    override fun close() {
        rendererInstance.close()
    }
}

/**
 * Not in the original Renderer. This wraps URLClassLoader with SystemClassLoader fallback for
 * resource lookups.
 */
private fun createResourceEnhancedClassLoader(urls: Array<URL>, parent: ClassLoader): URLClassLoader {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    return object : URLClassLoader(urls, parent) {
        override fun getResourceAsStream(name: String): InputStream? =
            super.getResourceAsStream(name) ?: systemClassLoader.getResourceAsStream(name)

        override fun getResource(name: String): URL? =
            super.getResource(name) ?: systemClassLoader.getResource(name)

        override fun getResources(name: String): Enumeration<URL> {
            val parentResources = super.getResources(name).toList()
            val systemResources = systemClassLoader.getResources(name).toList()
            return Collections.enumeration(parentResources + systemResources)
        }
    }
}
