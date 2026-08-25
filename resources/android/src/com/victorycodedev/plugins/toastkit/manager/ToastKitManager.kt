package com.victorycodedev.plugins.toastkit.manager

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.victorycodedev.plugins.toastkit.bridge.ToastKitBridgeNormalizer
import com.victorycodedev.plugins.toastkit.bridge.ToastKitEventDispatcher
import com.victorycodedev.plugins.toastkit.model.*

internal object ToastKitManager {
    val visible = mutableStateListOf<ToastKitConfiguration>()
    val rendered = mutableStateListOf<ToastKitConfiguration>()
    val exiting = mutableStateListOf<String>()
    private val states = linkedMapOf<String, ToastKitState>()
    private val waiting = ArrayDeque<String>()
    private val timers = mutableMapOf<String, Runnable>()
    private val main = Handler(Looper.getMainLooper())

    fun show(configuration: ToastKitConfiguration) = onMain {
        if (states.containsKey(configuration.id)) return@onMain
        states[configuration.id] = ToastKitState(configuration)
        if (canAdmit(configuration)) admit(configuration.id) else waiting.addLast(configuration.id)
    }

    fun update(id: String, changes: Map<String, Any>) = onMain {
        val state = states[id] ?: return@onMain
        if (state.terminated) return@onMain
        val updated = try { ToastKitBridgeNormalizer.applyChanges(state.configuration, changes) } catch (_: IllegalArgumentException) { return@onMain }
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
        ToastKitEventDispatcher.action(id, actionId)
        terminate(id, "action")
    }

    private fun canAdmit(configuration: ToastKitConfiguration): Boolean {
        if (exiting.isNotEmpty()) return false
        val current = visible.mapNotNull { states[it.id]?.configuration }
        return when (configuration.strategy) {
            ToastKitStrategy.QUEUE -> current.isEmpty()
            ToastKitStrategy.STACK -> current.none { it.strategy == ToastKitStrategy.QUEUE } &&
                current.count { it.strategy == ToastKitStrategy.STACK } < configuration.maxVisible
        }
    }

    private fun admit(id: String) {
        val state = states[id] ?: return
        if (state.terminated || state.visible) return
        states[id] = state.copy(visible = true)
        visible.add(state.configuration)
        rendered.add(state.configuration)
        schedule(state.configuration)
        ToastKitEventDispatcher.shown(id)
    }

    private fun schedule(configuration: ToastKitConfiguration) {
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
        val exitDuration = if (state.configuration.animation == ToastKitAnimation.SPRING) 450L else 260L
        main.postDelayed({
            rendered.removeAll { it.id == id }
            exiting.remove(id)
            if (promote) promote()
        }, exitDuration)
        states.remove(id)
        ToastKitEventDispatcher.dismissed(id, reason)
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

    private fun replaceVisible(configuration: ToastKitConfiguration) {
        val index = visible.indexOfFirst { it.id == configuration.id }
        if (index >= 0) visible[index] = configuration
        val renderedIndex = rendered.indexOfFirst { it.id == configuration.id }
        if (renderedIndex >= 0) rendered[renderedIndex] = configuration
    }

    private inline fun onMain(crossinline operation: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) operation() else main.post { operation() }
    }
}
