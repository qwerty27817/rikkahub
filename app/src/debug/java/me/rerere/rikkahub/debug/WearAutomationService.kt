package me.rerere.rikkahub.debug

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLDecoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Debug-only accessibility bridge for automated watch UI tests.
 *
 * The service listens on 127.0.0.1:8096 and accepts simple HTTP commands, so the host can
 * `adb forward tcp:8096 tcp:8096` and drive the UI without fighting IME / synthetic gestures:
 *
 *   GET  /tree                       dump text/desc/bounds/clickable/editable nodes
 *   POST /click?text=OK              click the node with that text or content description
 *   POST /settext?value=...          set text on the (first) editable field, optional &label=/&index=
 *   POST /scroll?dir=forward|backward
 *   POST /back                       global back
 *   POST /tap?x=100&y=200            raw coordinate tap
 *
 * It is declared only in src/debug and never shipped in release builds.
 */
class WearAutomationService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        startServer()
        Log.i(TAG, "accessibility bridge connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun startServer() {
        if (serverThread?.isAlive == true) return
        serverThread = Thread({
            runCatching {
                val socket = ServerSocket(PORT, 20, InetAddress.getByName("127.0.0.1"))
                serverSocket = socket
                Log.i(TAG, "listening on 127.0.0.1:$PORT")
                while (!socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: continue
                    runCatching { handle(client) }
                        .onFailure { Log.w(TAG, "request failed", it) }
                    runCatching { client.close() }
                }
            }.onFailure { Log.e(TAG, "server stopped", it) }
        }, "wear-automation-server").apply {
            isDaemon = true
            start()
        }
    }

    private fun stopServer() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        serverThread = null
    }

    private fun handle(client: java.net.Socket) {
        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
        val requestLine = reader.readLine() ?: return
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
        }
        val parts = requestLine.split(' ')
        if (parts.size < 2) return
        val method = parts[0].uppercase()
        val target = parts[1]
        val path = target.substringBefore('?')
        val query = parseQuery(target.substringAfter('?', ""))

        val body = when {
            method == "GET" && path == "/ping" -> """{"ok":true,"service":"wear-automation"}"""
            method == "GET" && path == "/tree" -> onMain { dumpTree() } ?: errorJson("no active window")
            method == "POST" && path == "/click" -> {
                val text = query["text"] ?: query["desc"] ?: ""
                val ok = onMain { clickByText(text) } ?: false
                resultJson(ok, "click", text)
            }
            method == "POST" && path == "/settext" -> {
                val value = query["value"].orEmpty()
                val label = query["label"]
                val index = query["index"]?.toIntOrNull()
                val ok = onMain { setText(label, value, index) } ?: false
                resultJson(ok, "settext", label ?: "field")
            }
            method == "POST" && path == "/scroll" -> {
                val dir = query["dir"] ?: "forward"
                val ok = onMain { scroll(dir) } ?: false
                resultJson(ok, "scroll", dir)
            }
            method == "POST" && path == "/back" -> {
                val ok = onMain { performGlobalAction(GLOBAL_ACTION_BACK) } ?: false
                resultJson(ok, "back", "")
            }
            method == "POST" && path == "/tap" -> {
                val x = query["x"]?.toFloatOrNull() ?: 0f
                val y = query["y"]?.toFloatOrNull() ?: 0f
                val ok = onMain { tap(x, y) } ?: false
                resultJson(ok, "tap", "$x,$y")
            }
            else -> errorJson("unknown command: $method $path")
        }

        val bytes = body.toByteArray(Charsets.UTF_8)
        val response = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
        val output: OutputStream = client.getOutputStream()
        output.write(response)
        output.write(bytes)
        output.flush()
    }

    private fun parseQuery(raw: String): Map<String, String> =
        raw.split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=').takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val value = URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
            key to value
        }.toMap()

    private fun <T> onMain(block: () -> T?): T? {
        var result: T? = null
        val latch = CountDownLatch(1)
        mainHandler.post {
            result = runCatching(block).getOrNull()
            latch.countDown()
        }
        latch.await(8, TimeUnit.SECONDS)
        return result
    }

    private fun dumpTree(): String {
        val root = rootInActiveWindow ?: return errorJson("no active window")
        return buildJsonObject {
            put("ok", true)
            put("package", root.packageName?.toString().orEmpty())
            putJsonArray("nodes") {
                var index = 0
                fun walk(node: AccessibilityNodeInfo?) {
                    if (node == null) return
                    val text = node.text?.toString().orEmpty()
                    val desc = node.contentDescription?.toString().orEmpty()
                    if (text.isNotEmpty() || desc.isNotEmpty() || node.isEditable || node.isScrollable) {
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        addJsonObject {
                            put("i", index)
                            put("text", text)
                            put("desc", desc)
                            put("class", node.className?.toString()?.substringAfterLast('.') ?: "")
                            put("bounds", listOf(rect.left, rect.top, rect.right, rect.bottom).joinToString(","))
                            put("clickable", node.isClickable)
                            put("editable", node.isEditable)
                            put("scrollable", node.isScrollable)
                            put("focused", node.isFocused)
                        }
                    }
                    index++
                    for (child in 0 until node.childCount) walk(node.getChild(child))
                }
                walk(root)
            }
        }.toString()
    }

    private fun clickByText(target: String): Boolean {
        if (target.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val matches = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root) { node ->
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            if (text == target || desc == target) matches += node
        }
        for (node in matches) {
            var candidate: AccessibilityNodeInfo? = node
            while (candidate != null) {
                if (candidate.isClickable && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                candidate = candidate.parent
            }
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.isEmpty && tap(rect.centerX().toFloat(), rect.centerY().toFloat())) return true
        }
        // fallback: substring match on text
        val partial = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root) { node ->
            val text = node.text?.toString().orEmpty()
            if (text.isNotBlank() && text.contains(target)) partial += node
        }
        for (node in partial) {
            var candidate: AccessibilityNodeInfo? = node
            while (candidate != null) {
                if (candidate.isClickable && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                candidate = candidate.parent
            }
        }
        return false
    }

    private fun setText(label: String?, value: String, index: Int?): Boolean {
        val root = rootInActiveWindow ?: return false
        val editable = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root) { node -> if (node.isEditable) editable += node }
        val target = when {
            index != null -> editable.getOrNull(index)
            label != null -> editable.firstOrNull { node -> isNearLabel(node, label) } ?: editable.firstOrNull()
            else -> editable.firstOrNull()
        } ?: return false
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        if (target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) return true
        // Compose occasionally only exposes focus + text input actions; try requesting focus first.
        target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun isNearLabel(node: AccessibilityNodeInfo, label: String): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            val text = current.text?.toString().orEmpty()
            val desc = current.contentDescription?.toString().orEmpty()
            if (text.contains(label) || desc.contains(label)) return true
            current = current.parent
            depth++
        }
        return false
    }

    private fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollables = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root) { node -> if (node.isScrollable) scrollables += node }
        val action = if (direction == "backward") {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        return scrollables.any { it.performAction(action) }
    }

    private fun collectAll(node: AccessibilityNodeInfo?, block: (AccessibilityNodeInfo) -> Unit) {
        if (node == null) return
        block(node)
        for (child in 0 until node.childCount) collectAll(node.getChild(child), block)
    }

    private fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun resultJson(ok: Boolean, action: String, target: String): String = buildJsonObject {
        put("ok", ok)
        put("action", action)
        put("target", target)
    }.toString()

    private fun errorJson(message: String): String = buildJsonObject {
        put("ok", false)
        put("error", message)
    }.toString()

    private companion object {
        const val TAG = "WearAutomation"
        const val PORT = 8096
    }
}
