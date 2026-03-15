package com.claudescreensaver.data.network

import com.claudescreensaver.data.models.AgentStatus
import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.time.Duration

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

class SseClient {

    private val _status = MutableStateFlow(AgentStatus.DISCONNECTED)
    val status: StateFlow<AgentStatus> = _status.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var eventSource: EventSource? = null

    fun connect(url: String) {
        disconnect()
        _connectionState.value = ConnectionState.CONNECTING

        val handler = object : EventHandler {
            override fun onOpen() {
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(event: String, messageEvent: MessageEvent) {
                try {
                    val agentStatus = AgentStatus.fromJson(messageEvent.data)
                    _status.value = agentStatus
                } catch (e: Exception) {
                    // Ignore malformed messages
                }
            }

            override fun onError(t: Throwable) {
                _connectionState.value = ConnectionState.ERROR
            }

            override fun onClosed() {
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onComment(comment: String) {}
        }

        eventSource = EventSource.Builder(handler, URI.create(url))
            .reconnectTime(Duration.ofSeconds(2))
            .build()
        eventSource?.start()
    }

    fun disconnect() {
        eventSource?.close()
        eventSource = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _status.value = AgentStatus.DISCONNECTED
    }
}
