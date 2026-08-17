package com.victorycodedev.plugins.toastkit.manager

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.victorycodedev.plugins.toastkit.bridge.BridgeNormalizer
import com.victorycodedev.plugins.toastkit.bridge.NativeEventDispatcher
import com.victorycodedev.plugins.toastkit.model.*

internal object ToastManager {
    val visible = mutableStateListOf<ToastConfiguration>()
    val rendered = mutableStateListOf<ToastConfiguration>()
    val exiting = mutableStateListOf<String>()
    private val states = linkedMapOf<String, ToastState>()
    private val waiting = ArrayDeque<String>()
    private val timers = mutableMapOf<String, Runnable>()
    private val main = Handler(Looper.getMainLooper())

    fun show(configuration: ToastConfiguration) = onMain {
        if (states.containsKey(configuration.id)) return@onMain
        states[configuration.id] = ToastState(configuration)
        if (canAdmit(configuration)) admit(configuration.id) else waiting.addLast(configuration.id)
    }

    fun update(id: String, changes: Map<String, Any>) = onMain {
        val state = states[id] ?: return@onMain
        if (state.terminated) return@onMain
        val updated = try { BridgeNormalizer.applyChanges(state.configuration, changes) } catch (_: IllegalArgumentException) { return@onMain }
        states[id] = state.copy(configuration = updated)
        if (state.visible) {
            replaceVisible(updated)
            if (changes.containsKey("duration") || changes.containsKey("persistent")) schedule(updated)
        }
    }

    fun dismiss(id: String, reason: String = "programmatic") = onMain { terminate(id, reason) }

    fun dismissAll() = onMain {
        states.keys.toList().forEach { terminate(it, "programmatic", promote = false) }
        waiting.clear()
        promote()
    }

    fun action(id: String, actionId: String) = onMain {
        val state = states[id] ?: return@onMain
        if (state.terminated) return@onMain
        NativeEventDispatcher.action(id, actionId)
        terminate(id, "action")
    }

    private fun canAdmit(configuration: ToastConfiguration): Boolean {
        val current = visible.mapNotNull { states[it.id]?.configuration }
        return when (configuration.strategy) {
            ToastStrategy.QUEUE -> current.isEmpty()
            ToastStrategy.STACK -> current.none { it.strategy == ToastStrategy.QUEUE } &&
                current.count { it.strategy == ToastStrategy.STACK } < configuration.maxVisible
        }
    }

    private fun admit(id: String) {
        val state = states[id] ?: return
        if (state.terminated || state.visible) return
        states[id] = state.copy(visible = true)
        visible.add(state.configuration)
        rendered.add(state.configuration)
        schedule(state.configuration)
        NativeEventDispatcher.shown(id)
    }

    private fun schedule(configuration: ToastConfiguration) {
        timers.remove(configuration.id)?.let(main::removeCallbacks)
        val duration = configuration.durationMs ?: return
        val task = Runnable { terminate(configuration.id, "timeout") }
        timers[configuration.id] = task
        main.postDelayed(task, duration)
    }

    private fun terminate(id: String, reason: String, promote: Boolean = true) {
        val state = states[id] ?: return
        if (state.terminated) return
        states[id] = state.copy(terminated = true)
        timers.remove(id)?.let(main::removeCallbacks)
        waiting.remove(id)
        visible.removeAll { it.id == id }
        exiting.add(id)
        main.postDelayed({
            rendered.removeAll { it.id == id }
            exiting.remove(id)
        }, 260L)
        states.remove(id)
        NativeEventDispatcher.dismissed(id, reason)
        if (promote) promote()
    }

    private fun promote() {
        var progressed: Boolean
        do {
            progressed = false
            val id = waiting.firstOrNull() ?: break
            val state = states[id]
            if (state == null || state.terminated) {
                waiting.removeFirst()
                progressed = true
            } else if (canAdmit(state.configuration)) {
                waiting.removeFirst()
                admit(id)
                progressed = true
            }
        } while (progressed)
    }

    private fun replaceVisible(configuration: ToastConfiguration) {
        val index = visible.indexOfFirst { it.id == configuration.id }
        if (index >= 0) visible[index] = configuration
        val renderedIndex = rendered.indexOfFirst { it.id == configuration.id }
        if (renderedIndex >= 0) rendered[renderedIndex] = configuration
    }

    private inline fun onMain(crossinline operation: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) operation() else main.post { operation() }
    }
}
