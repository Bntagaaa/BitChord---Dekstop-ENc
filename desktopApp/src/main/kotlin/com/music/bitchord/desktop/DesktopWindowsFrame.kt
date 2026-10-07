package com.music.bitchord.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.EventQueue
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowStateListener
import java.beans.PropertyChangeListener
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Windows frame bridge for the always-on Windows-style caption; never loaded on Linux. */
internal object DesktopWindowsFrame {
    /**
     * Probe before constructing the AWT peer. A missing/old DLL falls back to a NORMAL decorated
     * window rather than a frameless window without a Close button. The API check also detects
     * an older extracted DLL which has the old traffic-light bridge but not native captions.
     */
    val available: Boolean by lazy {
        DesktopPlatform.isWindows && runCatching {
            DesktopAnalysisRuntime.loadNative(LIBRARY)
            check(nativeCaptionApiVersion() == CAPTION_API_VERSION) { "caption bridge version mismatch" }
        }.onFailure {
            DesktopTrackLog.log("native Windows caption unavailable; using system title bar: ${it.message}")
        }.isSuccess
    }

    @Volatile private var installedWindow: Window? = null
    @Volatile private var installedHandle = 0L
    val isInstalled: Boolean get() = installedHandle != 0L
    private val metrics = MutableStateFlow(DesktopNativeCaptionMetrics())
    val captionMetrics: StateFlow<DesktopNativeCaptionMetrics> = metrics.asStateFlow()

    suspend fun install(window: Window): Boolean {
        if (!available) return false
        repeat(INSTALL_ATTEMPTS) {
            val handle = runCatching {
                if (window.isDisplayable) Pointer.nativeValue(Native.getWindowPointer(window)) else 0L
            }.getOrDefault(0L)
            if (handle != 0L && runCatching { nativeCaptionInstall(handle) }.getOrDefault(false)) {
                installedWindow = window
                installedHandle = handle
                refreshMetrics(window)
                DesktopTrackLog.log("native Windows caption installed (always on)")
                return true
            }
            delay(INSTALL_RETRY_MILLIS)
        }
        DesktopTrackLog.log("native Windows caption install failed; recreating a system-decorated window")
        return false
    }

    /** AWT notifies on resize, maximize/restore and monitor/DPI moves. No busy polling. */
    fun observe(window: Window): AutoCloseable {
        val disposed = AtomicBoolean(false)
        val pending = AtomicBoolean(false)
        fun refresh() {
            if (disposed.get() || !pending.compareAndSet(false, true)) return
            EventQueue.invokeLater {
                pending.set(false)
                if (!disposed.get()) refreshMetrics(window)
            }
        }
        val component = object : ComponentAdapter() {
            override fun componentMoved(event: ComponentEvent) = refresh()
            override fun componentResized(event: ComponentEvent) = refresh()
            override fun componentShown(event: ComponentEvent) = refresh()
        }
        val windowState = WindowStateListener { refresh() }
        val graphics = PropertyChangeListener { refresh() }
        window.addComponentListener(component)
        window.addWindowStateListener(windowState)
        window.addPropertyChangeListener("graphicsConfiguration", graphics)
        refresh()
        return AutoCloseable {
            disposed.set(true)
            window.removeComponentListener(component)
            window.removeWindowStateListener(windowState)
            window.removePropertyChangeListener("graphicsConfiguration", graphics)
            if (installedWindow === window) {
                installedWindow = null
                installedHandle = 0L
                metrics.value = DesktopNativeCaptionMetrics()
            }
        }
    }

    private fun refreshMetrics(window: Window) {
        if (installedWindow !== window || !window.isDisplayable) return
        val transform = window.graphicsConfiguration?.defaultTransform ?: return
        val raw = runCatching { nativeCaptionMetrics(installedHandle) }.getOrNull()
        metrics.value = desktopNativeCaptionMetrics(raw, transform.scaleX, transform.scaleY)
    }

    fun minimize(): Boolean = installedHandle != 0L &&
        runCatching { nativeCaptionCommand(installedHandle, 0) }.getOrDefault(false)

    fun toggleMaximize(): Boolean = installedHandle != 0L &&
        runCatching { nativeCaptionCommand(installedHandle, 1) }.getOrDefault(false)

    fun setBackdrop(kind: Int): Boolean = installedHandle != 0L &&
        runCatching { nativeCaptionSetBackdrop(installedHandle, kind) }.getOrDefault(false)

    @JvmStatic private external fun nativeCaptionApiVersion(): Int
    @JvmStatic private external fun nativeCaptionInstall(handle: Long): Boolean
    @JvmStatic private external fun nativeCaptionMetrics(handle: Long): IntArray
    @JvmStatic private external fun nativeCaptionCommand(handle: Long, action: Int): Boolean
    @JvmStatic private external fun nativeCaptionSetBackdrop(handle: Long, kind: Int): Boolean

    private const val LIBRARY = "bitchord_window"
    private const val CAPTION_API_VERSION = 2
    private const val INSTALL_ATTEMPTS = 20
    private const val INSTALL_RETRY_MILLIS = 100L
}
