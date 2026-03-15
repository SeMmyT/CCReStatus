package com.claudescreensaver.data.network

import com.claudescreensaver.data.models.AgentState
import org.junit.Assert.*
import org.junit.Test

class SseClientTest {

    @Test
    fun `initial status is DISCONNECTED sentinel`() {
        val client = SseClient()
        assertEquals(AgentState.IDLE, client.status.value.state)
        assertEquals("Not connected", client.status.value.message)
    }

    @Test
    fun `initial connection state is DISCONNECTED`() {
        val client = SseClient()
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
    }
}
