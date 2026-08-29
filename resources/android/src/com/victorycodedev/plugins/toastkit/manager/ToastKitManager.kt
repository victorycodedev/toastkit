package com.victorycodedev.plugins.toastkit.manager

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.victorycodedev.plugins.toastkit.bridge.ToastKitBridgeNormalizer
import com.victorycodedev.plugins.toastkit.bridge.ToastKitEventDispatcher
import com.victorycodedev.plugins.toastkit.model.*

internal data class ToastKitShowResult(val id: String, val accepted: Boolean)

internal object ToastKitManager {
    val visible = mutableStateListOf<ToastKitConfiguration>()
    val rendered = mutableStateListOf<ToastKitConfiguration>()
    val exiting = mutableStateListOf<String>()
    private val states = linkedMapOf<String, ToastKitState>()
    private val waiting = ArrayDeque<String>()
    private val timers = mutableMapOf<String, Runnable>()
    private val main = Handler(Looper.getMainLooper())
    private val identityLock = Any()
    private val activeIds = mutableSetOf<String>()
    private val uniqueToId = mutableMapOf<String, String>()

    fun show(configuration: ToastKitConfiguration): ToastKitShowResult {
        val result = synchronized(identityLock) {
            configuration.uniqueKey?.let { key ->
                uniqueToId[key]?.let { return@synchronized ToastKitShowResult(it, false) }
            }
            if (!activeIds.add(configuration.id)) return@synchronized ToastKitShowResult(configuration.id, false)
            configuration.uniqueKey?.let { uniqueToId[it] = configuration.id }
            ToastKitShowResult(configuration.id, true)
        }
        if (!result.accepted) return result
        onMain {
            states[configuration.id] = ToastKitState(configuration)
            if (canAdmit(configuration)) admit(configuration.id) else waiting.addLast(configuration.id)
        }
        return result
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

    fun updateUnique(key: String, changes: Map<String, Any>): String {
        val id = resolveUnique(key)
        update(id, changes)
        return id
    }

    fun dismiss(id: String, reason: String = "programmatic") = onMain { terminate(id, reason) }

    fun dismissUnique(key: String): String {
        val id = resolveUnique(key)
        dismiss(id)
        return id
    }

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
        val exitDuration = if (!state.visible) 0L else if (state.configuration.nativeAndroid) 160L else when (state.configuration.animation) {
            ToastKitAnimation.SPRING, ToastKitAnimation.BOUNCE -> 450L
            ToastKitAnimation.REVEAL -> 280L
            else -> 260L
        }
        main.postDelayed({
            rendered.removeAll { it.id == id }
            exiting.remove(id)
            releaseIdentity(state.configuration)
            if (promote) promote()
        }, exitDuration)
        states.remove(id)
        ToastKitEventDispatcher.dismissed(id, reason)
    }

    private fun resolveUnique(key: String): String = synchronized(identityLock) {
        uniqueToId[key] ?: throw IllegalArgumentException("Unique toast [$key] is not active.")
    }

    private fun releaseIdentity(configuration: ToastKitConfiguration) = synchronized(identityLock) {
        activeIds.remove(configuration.id)
        configuration.uniqueKey?.let { key ->
            if (uniqueToId[key] == configuration.id) uniqueToId.remove(key)
        }
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
